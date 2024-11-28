package xyz.redslime.releaseradar.listener

import com.adamratzman.spotify.utils.Market
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
import xyz.redslime.releaseradar.getDurationFriendly
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.toAlbum
import xyz.redslime.releaseradar.util.silentCancellableCoroutine
import xyz.redslime.releaseradar.util.trackRegex

/**
 * Listens for messages containing open.spotify.com url(s) and replies with a custom embed containing information
 * about the linked item(s) if the default spotify embed is not working (as it often the case)
 *
 * @author redslime
 * @version 2024-11-28
 */
class EmbedListener {

    private val regex = Regex(".*open.spotify.com(/intl-[a-zA-Z]{2})*/(track/([A-z0-9]{22})|album/([A-z0-9]{22})).*")
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
                        delay(1200)

                        msg.reply {
                            this.allowedMentions {
                                this.repliedUser = false
                            }

                            // get the corresponding items in the message
                            regex.findAll(msg.content).toList().forEach { result ->
                                val item = result.groupValues[2] // in the form of (track|album)/[A-z0-9]{22}

                                embed {
                                    if(trackRegex.matches(item)) {
                                        val trackId = result.groupValues[3]
                                        spotify.api { it.tracks.getTrack(trackId, Market.WS) }?.let { track ->
                                            val artists = track.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
                                            val year = track.album.releaseDate?.year

                                            this.author {
                                                this.name = artists
                                                this.icon = track.artists.first().toFullArtist()?.images?.get(0)?.url ?: ""
                                            }
                                            this.title = track.name
                                            this.url = track.externalUrls.spotify
                                            this.thumbnail {
                                                this.url = track.album.images?.get(0)?.url ?: ""
                                            }
                                            this.description = "Single • ${track.album.toAlbum().label} • ${track.getDurationFriendly()} • $year"
                                        }
                                    } else {
                                        val albumId = result.groupValues[4]
                                        spotify.api { it.albums.getAlbum(albumId, Market.WS) }?.let { album ->
                                            val artists = album.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
                                            val year = album.releaseDate.year

                                            this.author {
                                                this.name = artists
                                                this.icon = album.artists.first().toFullArtist()?.images?.get(0)?.url ?: ""
                                            }
                                            this.title = album.name
                                            this.url = album.externalUrls.spotify
                                            this.thumbnail {
                                                url = album.images?.get(0)?.url ?: ""
                                            }
                                            this.description = "Album • ${album.label} • ${album.totalTracks} tracks • $year"
                                        }
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
                        if(msg.embeds.any { it.data.url.value?.matches(regex) == true }) {
                            // Message containing spotify link(s) that previously had no embeds now has some, cancel our custom embed
                            pool.remove(this.messageId)?.cancel()
                        }
                    }
                }
            }
        }
    }
}