package xyz.redslime.releaseradar.listener

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
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

    fun register(client: Kord) {
        client.on<MessageCreateEvent> {
            if(this.message.author == client.getSelf())
                return@on

            if(regex.containsMatchIn(this.message.content)) {
                if(this.message.embeds.isEmpty()) {
                    val msg = this.message
                    val job = silentCancellableCoroutine {
                        // wait a second, perhaps the embed is loading, then this will be cancelled by the UpdateEvent below
                        delay(1300)

                        msg.reply {
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
                    }
                    pool[this.message.id] = job
                }
            }
        }

        client.on<MessageUpdateEvent> {
            if(pool.containsKey(this.messageId)) {
                val msg = this.getMessage()

                if(regex.containsMatchIn(msg.content)) {
                    if(msg.embeds.isNotEmpty()) {
                        if(msg.embeds.any { it.data.url.value?.contains(regex) == true }) {
                            // Message containing spotify link(s) that previously had no embeds now has some, cancel our custom embed
                            pool.remove(this.messageId)?.cancel()
                        }
                    }
                }
            }
        }
    }
}