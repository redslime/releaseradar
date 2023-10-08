package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.util.albumRegex
import xyz.redslime.releaseradar.util.extractSpotifyLink
import xyz.redslime.releaseradar.util.reminderEmoji
import java.time.Duration
import kotlin.time.toKotlinDuration

/**
 * @author redslime
 * @version 2023-10-08
 */
class TopCommand: Command("top", "Lists the top tracks in the specified channel from the last 30 days") {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel to list the top for")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferPublicResponse()
        val channel = interaction.command.channels["channel"]!!
        val radarId = cache.getRadarId(channel.id.asLong())

        if(radarId == null) {
            response.respond {
                embed {
                    error()
                    title = "${channel.mention} is not a radar channel!"
                }
            }
            return
        }

        val reacts = cache.getRadarEmotes(radarId)
        val tracks = mutableMapOf<String, Score>()
        val ch = channel.fetchChannel() as MessageChannelBehavior

        channel.data.lastMessageId.value?.let { lastMessage ->
            val instant = lastMessage.timestamp.minus(Duration.ofDays(30).toKotlinDuration())

            ch.getMessagesBefore(lastMessage)
                .takeWhile { it.timestamp > instant }
                .filter { it.author == interaction.kord.getSelf() }
                .toList()
                .forEach { message ->
                    extractSpotifyLink(message)?.let { url ->
                        val score = Score(reacts, 0, 0, 0, 0.0)
                        message.reactions.forEach {
                            if(it.emoji == reacts[0])
                                score.likes += it.count - 1
                            if(it.emoji == reacts[1])
                                score.dislikes += it.count - 1
                            if(it.emoji == reacts[2])
                                score.hearts += it.count - 1
                            if(it.emoji == reminderEmoji)
                                score.reminders += 0.1 * (it.count - 1)
                        }

                        tracks[url] = score
                    }
                }
        }

        val sorted = tracks.toList()
            .filter { (_, score) -> score.getScore() >= 1}
            .sortedByDescending { (_, score) -> score.getScore() }
            .take(20)
            .toMap()
            .mapKeys { it.key.replace(albumRegex, "$1") }

        if(sorted.isEmpty()) {
            response.respond {
                embed {
                    error()
                    title = "Found no recent tracks with ratings in ${channel.mention}"
                }
            }
            return
        }

        val data = spotify.api.albums.getAlbums(*sorted.keys.toTypedArray(), market = Market.WS)
        val final = sorted.mapKeys { entry ->
            data.find { it?.id == entry.key }
        }

        if(final.isEmpty()) {
            response.respond {
                embed {
                    title = "Top tracks of ${channel.mention} (last 30 days)"
                    description = "None... Start reacting so tracks show up here!"
                }
            }
            return
        }

        response.respond {
            embed {
                title = "Top ${final.size} tracks of ${channel.mention} (last 30 days)"
                description = ""
                thumbnail {
                    url = final.keys.firstOrNull()?.images?.firstOrNull()?.url.toString()
                }

                final.filter { it.key != null }.onEachIndexed { i, (album, score) ->
                    val artists = album?.artists?.joinToString(" & ") { it.name }
                    val name = album?.name
                    description += "\n$i. [$artists - $name](${album?.externalUrls?.spotify}) ${score.getFriendly()}"
                }
            }
        }
    }

    data class Score(val reacts: List<ReactionEmoji>, var likes: Int, var dislikes: Int, var hearts: Int, var reminders: Double) {
        fun getScore(): Double {
            return likes + (-1 * dislikes) + (2 * hearts) + reminders
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