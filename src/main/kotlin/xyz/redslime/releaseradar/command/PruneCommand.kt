package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.*

/**
 * @author redslime
 * @version 2024-01-15
 */
class PruneCommand: Command("prune", "Remove inactive or unpopular artists from a radar", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "Radar channel to prune")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferPublicResponse()
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val radarId = cache.getRadarId(channel.getDbId())

        if(!checkRadarChannel(radarId, response, channel) || radarId == null)
            return

        // fetch all messages with reacts in radar channel
        val reacts = cache.getRadarEmotes(radarId)
        val tracks = mutableListOf<Triple<String, String, Int>>() // embed title (artists) -> url -> score
        val ch = channel.fetchChannel() as MessageChannelBehavior

        channel.data.lastMessageId.value?.let { lastMessage ->
            ch.getMessagesBefore(lastMessage)
                .takeWhile { true }
                .filter { it.author == interaction.kord.getSelf() }
                .toList()
                .forEach { message ->
                    extractSpotifyLink(message)?.let { url ->
                        extractEmbedArtistTitle(message)?.let { title ->
                            var score = 0

                            message.reactions.forEach {
                                if(it.emoji == reacts[0])
                                    score += it.count - 1
                                if(it.emoji == reacts[1])
                                    score += it.count - 1
                                if(it.emoji == reacts[2])
                                    score += it.count - 1
                                if(it.emoji == reminderEmoji)
                                    score += it.count - 1
                            }

                            tracks.add(Triple(title, url, score))
                        }
                    }
                }
        }

        // artists that have '&' in their names which need to be handled as an edge case
        val artistDelimiterNames = cache.getArtistNamesInRadarChannel(channel).keys.filter { it.contains(" & ") }.toList()
        // artist names that are duplicated and will be mapped to their id instead to avoid dumb shit
        val artistDuplicates = cache.getArtistNamesInRadarChannel(channel).keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        val artistEntries = mutableListOf<ArtistEntry>()

        // fill entries with all artists in radar so ones with no releases don't get skipped
        cache.getArtistNamesInRadarChannel(channel).forEach { (name, id) -> artistEntries.add(ArtistEntry(name, id, 0, 0)) }

        tracks.forEach { (artists, url, score) ->
            val artistList = artists.split(" & ").toMutableList()

            // handle edge case of artist names having '&' in them
            val delimiterMatches = artistDelimiterNames.filter { artists.contains(it) }

            if(delimiterMatches.isNotEmpty()) {
                delimiterMatches.forEach { name ->
                    name.split(" & ").forEach { s -> artistList.remove(s) }
                    artistList.add(name)
                }
            }

            // handle edge case of artist names existing twice
            if(artistList.any { str -> artistDuplicates.contains(str) }) {
                spotify.getArtistsFromUrl(url)?.forEach { artist ->
                    artistEntries.firstOrNull { it.id == artist.id }?.addScore(score)
                }
            }

            // handle normal artist names
            artistList.forEach { artistName ->
                artistEntries.firstOrNull { it.name == artistName }?.addScore(score)
            }
        }

        promptConfirm(artistEntries, 0.5, false, channel, response)
    }

    private suspend fun promptConfirm(entries: List<ArtistEntry>, cutoff: Double, details: Boolean, channel: ResolvedChannel, response: DeferredMessageInteractionResponseBehavior) {
        val list = entries.filter { it.getAvgScore() <= cutoff }.sortedBy { it.getAvgScore() }

        response.respond {
            embed {
                warning()
                title = "This would remove **${pluralPrefixed("artist", list.size)}** from ${channel.mention} (${formatPercent(list.size, entries.size)})"
                description = "Based on an average reaction score of **<=${String.format("%.1f", cutoff)}**\n" +
                        "calculated as follows: ``score = (all reactions/releases)``"

                if(details && list.isNotEmpty()) {
                    var add = "\n\n**Artists subject to be removed**:\n"
                    add += list.map { e -> e.name + " (${e.getAvgScoreFriendly()})" }.joinToString("\n")

                    if(add.length < 4000)
                        description += add
                    else
                        this@respond.addFile("list.txt", ChannelProvider(null) { ByteReadChannel(add) })
                }
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Success, "Confirm") {
                    it.message.delete()
                    val re = it.deferPublicResponse()
                    var description = ""
                    val ids = list.mapNotNull { e -> e.id }
                    val skipped = db.removeArtistIdsFromRadar(ids, channel)

                    list.forEach { ae ->
                        if(!skipped.contains(ae.name))
                            description += ae.name + "\n"
                    }

                    re.respond {
                        embed {
                            success()
                            title = "Removed ${pluralPrefixed("artist",  list.size - skipped.size)} from ${channel.mention}"

                            if(description.length < 4000)
                                this.description = description
                        }

                        if(description.length >= 4000)
                            this.addFile("list.txt", ChannelProvider(null) { ByteReadChannel(description) })
                    }
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Toggle detailed list", disabled = list.isEmpty()) {
                    it.message.delete()
                    promptConfirm(entries, cutoff, !details, channel, it.deferPublicResponse())
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Change cutoff score") {
                    editCutoffMessage(entries, cutoff, details, channel, it.deferPublicMessageUpdate())
                }
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    it.message.delete()
                }
            }
        }
    }

    private suspend fun editCutoffMessage(entries: List<ArtistEntry>, cutoff: Double, details: Boolean, channel: ResolvedChannel, re: PublicMessageInteractionResponseBehavior) {
        val list = entries.filter { it.getAvgScore() <= cutoff }.sortedBy { it.getAvgScore() }

        re.edit {
            embed {
                warning()
                title = "Changing cutoff score"
                description = "Current score: **${format1(cutoff)}**\n" +
                        "This would remove **${pluralPrefixed("artist", list.size)}** from ${channel.mention} (${formatPercent(list.size, entries.size)})\n" +
                        "For each artist a score is calculated as follows: ``score = (all reactions/releases)``\n" +
                        "Those with a score <=${format1(cutoff)} are subject to be removed."

                if(details && list.isNotEmpty()) {
                    var add = "\n\n**Artists subject to be removed**:\n"
                    add += list.joinToString("\n") { e -> e.name + " (${e.getAvgScoreFriendly()})" }

                    if(add.length < 4000)
                        description += add
                    else
                        this@edit.addFile("list.txt", ChannelProvider(null) { ByteReadChannel(add) })
                }
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Primary, "-0.5", disabled = cutoff - 0.5 < 0) {
                    editCutoffMessage(entries, cutoff - 0.5, details, channel, it.deferPublicMessageUpdate())
                }
                addInteractionButton(this, ButtonStyle.Primary, "-0.1", disabled = cutoff - 0.1 < 0) {
                    editCutoffMessage(entries, cutoff - 0.1, details, channel, it.deferPublicMessageUpdate())
                }
                addInteractionButton(this, ButtonStyle.Success, "Set score") {
                    it.message.delete()
                    promptConfirm(entries, cutoff, details, channel, it.deferPublicResponse())
                }
                addInteractionButton(this, ButtonStyle.Primary, "+0.1") {
                    editCutoffMessage(entries, cutoff + 0.1, details, channel, it.deferPublicMessageUpdate())
                }
                addInteractionButton(this, ButtonStyle.Primary, "+0.5") {
                    editCutoffMessage(entries, cutoff + 0.5, details, channel, it.deferPublicMessageUpdate())
                }
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Secondary, "Toggle detailed list") {
                    editCutoffMessage(entries, cutoff, !details, channel, it.deferPublicMessageUpdate())
                }
            }
        }
    }

    data class ArtistEntry(val name: String, var id: String? = null, var score: Int = 0, var entries: Int = 0) {
        fun addScore(score: Int) {
            this.score += score
            entries++
        }

        fun getAvgScore(): Double {
            if(entries == 0)
                return 0.0
            return (score * 1.0) / (entries * 1.0)
        }

        fun getAvgScoreFriendly(): String {
            return format1(getAvgScore())
        }
    }
}
