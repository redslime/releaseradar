package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.util.*
import java.time.Duration
import kotlin.time.toKotlinDuration

/**
 * @author redslime
 * @version 2023-10-08
 */
class TopCommand: Command("top", "Lists the top tracks in the specified channel from the last 30 days") {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel to list the top for")
        builder.boolean("invert", "Invert the list to show the lowest tracks") {
            required = false
        }
        builder.boolean("scores", "Show the exact score of each track") {
            required = false
        }
        builder.boolean("silent", "Send the results only visible to you") {
            required = false
        }
        builder.integer("days", "The range of past days to search") {
            required = false
            maxValue = 180
            minValue = 1
        }
        builder.string("artist", "Filter by artist") {
            required = false
        }
        builder.string("label", "Filter by label") {
            required = false
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = interaction.command.channels["channel"]!!
        val invert = interaction.command.booleans["invert"] ?: false
        val showScore = interaction.command.booleans["scores"] ?: false
        val silent = interaction.command.booleans["silent"] ?: false
        val days = interaction.command.integers["days"] ?: 30
        val artist = interaction.command.strings["artist"]
        val label = interaction.command.strings["label"]
        val radarId = cache.getRadarId(channel.id.asLong())
        val response = if(silent) {
            interaction.deferEphemeralResponse()
        } else {
            interaction.deferPublicResponse()
        }

        if(!checkRadarChannel(radarId, response, channel))
            return

        val reacts = cache.getRadarEmotes(radarId!!)
        val tracks = mutableMapOf<String, Score>()
        val ch = channel.fetchChannel() as MessageChannelBehavior

        channel.data.lastMessageId.value?.let { lastMessage ->
            val instant = lastMessage.timestamp.minus(Duration.ofDays(days).toKotlinDuration())

            ch.getMessagesBefore(lastMessage)
                .takeWhile { it.timestamp > instant }
                .filter { it.author == interaction.kord.getSelf() }
                .filter { artist == null || extractEmbedArtistTitle(it)?.lowercase()?.contains(artist.lowercase()) == true }
                .filter { label == null || extractEmbedLabel(it)?.trim().equals(label.trim(), ignoreCase = true) }
                .toList()
                .forEach { message ->
                    extractSpotifyLink(message)?.let { url ->
                        val score = Score(reacts, 0, 0, 0, 0)
                        message.reactions.forEach {
                            if(it.emoji == reacts[0])
                                score.likes += it.count - 1
                            if(it.emoji == reacts[1])
                                score.dislikes += it.count - 1
                            if(it.emoji == reacts[2])
                                score.hearts += it.count - 1
                            if(it.emoji == reminderEmoji)
                                score.reminders += it.count - 1
                        }

                        tracks[url] = score
                    }
                }
        }

        val sorted = if(!invert) {
            tracks.toList()
                // don't filter positive scores for artists and labels
                .filter { (_, score) -> artist != null || label != null || score.getScore() >= 1 }
                .sortedByDescending { (_, score) -> score.getScore() }
                .take(20)
                .toMap()
                .mapKeys { it.key.replace(albumRegex, "$1") }
        } else {
            tracks.toList()
                .sortedBy { (_, score) -> score.getScore() }
                .take(20)
                .toMap()
                .mapKeys { it.key.replace(albumRegex, "$1") }
        }

        if(sorted.isEmpty()) {
            response.respond {
                embed {
                    error()
                    title = "Found no recent tracks with ratings in ${channel.mention}"
                }
            }
            return
        }

        val op = if(invert) "Lowest" else "Top"
        val artistf = if(artist != null) " $artist" else ""
        val labl = if(label != null) " $label" else ""
        val data = spotify.api { api ->
            api.albums.getAlbums(*sorted.keys.toTypedArray(), market = Market.WS)
        }
        val final = sorted.mapKeys { entry ->
            data.find { it?.id == entry.key }
        }

        if(final.isEmpty()) {
            response.respond {
                embed {
                    title = "$op$artistf tracks$labl of ${channel.mention} (last $days days)"
                    description = "None... Start reacting so tracks show up here!"
                }
            }
            return
        }

        response.respond {
            embed {
                title = "$op ${final.size}$artistf$labl tracks of ${channel.mention} (last $days days)"
                description = ""
                thumbnail {
                    url = final.keys.firstOrNull()?.images?.firstOrNull()?.url.toString()
                }

                final.filter { it.key != null }.onEachIndexed { i, (album, score) ->
                    val artists = album?.artists?.filter { it.name != null }?.joinToString(" & ") { it.name!! }
                    val name = album?.name
                    description += "\n$i. [$artists - $name](${album?.externalUrls?.spotify}) ${score.getFriendly()}"

                    if(showScore) {
                        description += " (**${String.format("%.1f", score.getScore())}**, \u23F0 ${score.reminders})"
                    }
                }
            }
        }
    }

    data class Score(val reacts: List<ReactionEmoji>, var likes: Int, var dislikes: Int, var hearts: Int, var reminders: Int) {
        fun getScore(): Double {
            return likes + (-1 * dislikes) + (2 * hearts) + (0.1 * reminders)
        }

        fun getFriendly(): String {
            val map = mutableMapOf<String, Int>()

            if(likes > 0)
                map["${reacts[0].mention} $likes"] = likes
            if(dislikes > 0)
                map["${reacts[1].mention} $dislikes"] = dislikes
            if(hearts > 0)
                map["${reacts[2].mention} $hearts"] = hearts

            val sorted = map.toList().sortedByDescending { (_, score) -> score }.toMap()
            return sorted.keys.joinToString(", ")
        }
    }
}