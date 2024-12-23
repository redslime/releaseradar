package xyz.redslime.releaseradar.listener

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import xyz.redslime.releaseradar.buildAlbumEmbed
import xyz.redslime.releaseradar.buildArtistEmbed
import xyz.redslime.releaseradar.buildSingleEmbed
import xyz.redslime.releaseradar.util.silentCancellableCoroutine

/**
 * Listens for messages containing open.spotify.com url(s) and replies with a custom embed containing information
 * about the linked item(s) if the default spotify embed is not working (as it often the case)
 *
 * @author redslime
 * @version 2024-11-28
 */
class EmbedListener {

    private val regex = Regex("open.spotify.com(?:/intl-[A-z]{2})?/(track|album|artist)/([A-z0-9]{22})")
    private val pool = mutableMapOf<Snowflake, Job>()
    private val posted = mutableMapOf<Snowflake, Pair<Message, Job>>()

    fun register(client: Kord) {
        client.on<MessageCreateEvent> {
            if(this.message.author == client.getSelf())
                return@on

            if(regex.containsMatchIn(this.message.content)) {
                if(this.message.embeds.isEmpty()) {
                    val msg = this.message
                    val postJob = silentCancellableCoroutine {
                        // wait a second, perhaps the embed is loading, then this will be cancelled by the UpdateEvent below
                        delay(1300)

                        val reply = msg.reply {
                            this.allowedMentions {
                                this.repliedUser = false
                            }

                            // get the corresponding items in the message
                            regex.findAll(msg.content).toList().forEach { result ->
                                val type = result.groupValues[1]
                                val id = result.groupValues[2]

                                embed {
                                    when(type) {
                                        "track" -> buildSingleEmbed(id, this)
                                        "album" -> buildAlbumEmbed(id, this)
                                        "artist" -> buildArtistEmbed(id, this)
                                    }
                                }
                            }
                        }

                        // sometimes the embed *will* load after the delay above, in that case delete our reply again
                        // if it loads within 10 seconds (also see below), after that clean up any references
                        val deleteJob = silentCancellableCoroutine {
                            delay(10 * 1000)
                            posted.remove(msg.id)
                        }

                        posted[msg.id] = Pair(reply, deleteJob)
                    }
                    pool[msg.id] = postJob
                }
            }
        }

        client.on<MessageUpdateEvent> {
            if(hasSpotifyEmbed(this.getMessage())) {
                // message containing spotify link(s) that previously had no embeds now has some, cancel our custom embed
                pool.remove(this.messageId)?.cancel()

                // our embed already exists but the embed loaded later, delete our reply
                posted.remove(this.messageId)?.let { (msg, job) ->
                    msg.delete()
                    job.cancel()
                }
            }
        }
    }

    private fun hasSpotifyEmbed(msg: Message): Boolean {
        return regex.containsMatchIn(msg.content)
                && msg.embeds.isNotEmpty()
                && msg.embeds.any { it.data.url.value?.contains(regex) == true }
    }
}