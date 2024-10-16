package xyz.redslime.releaseradar.task

import com.adamratzman.spotify.models.SimpleAlbum
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.*
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.measureTime

/**
 * @author redslime
 * @version 2023-05-19
 */
class ScanNewTracksTask : Task(Duration.ofMillis(getMillisUntilMidnightNZ()), Duration.ofDays(1)) {

    private val LOGGER = LogManager.getLogger(javaClass)

    override fun run(client: Kord): TimerTask.() -> Unit {
        return {
            coroutine {
                runActual(client)
            }
        }
    }

    suspend fun runActual(client: Kord, force: Boolean = false) {
        val lastScan = db.getDurationSinceLastUpdatedTracks()

        if(lastScan < Duration.ofHours(2) && !force) {
            LOGGER.info("Last scan of new releases was too recent ($lastScan), skipping")
            return
        }

        db.setLastUpdatedTracks()

        val duration = measureTime {
            printToDiscord(client, LOGGER, "---- Checking new releases ----")
            val artists = cache.getAllArtistsOnRadars()
            val count = AtomicInteger(0)
//            val albums = artists.flatMap { spotify.getAlbumsAfter(it.id!!, it.lastRelease) }.toList().distinctBy { it.id }
            val channel = Channel<SimpleAlbum>(Channel.UNLIMITED)

            // use a channel to preserve first-come-first-serve order (most popular artist releases are posted first)
            coroutine {
                val postedAlbums = mutableSetOf<String>()

                spotify.getAlbumsAfterFlow(artists).collect { album ->
                    if(postedAlbums.add(album.id)) {
                        count.getAndIncrement()
                        channel.send(album)
                    }
                }
                channel.close()
            }

            for(album in channel) {
                // find radars this needs to be posted in
                val artistIds = album.artists.map { it.id }.distinct()
                val radars = cache.getAllRadarsWithArtists(artistIds)
                val excludedRadar = cache.getAllRadarsWithExcludedArtists(artistIds)

                radars.forEach { radarId ->
                    cache.getChannelId(radarId)?.also { channelId ->
                        if(excludedRadar.contains(radarId)) {
                            LOGGER.info("Not posting ${album.name} since on of the artists is excluded on this radar")
                            return@also
                        }

                        try {
                            val ch = client.getChannel(Snowflake(channelId)) as MessageChannelBehavior
                            val timezone = cache.getRadarTimezone(radarId)

                            if(timezone == Timezone.ASAP) {
                                LOGGER.info("Posting ${album.name} now in $channelId")
                                postAlbum(album.toAlbum(), ch, radarId)
                            } else {
                                LOGGER.info("Scheduled to post ${album.name} later in $channelId (${timezone.name})")
                                DiscordClient.postLaterTask.add(album, channelId, timezone)
                            }
                        } catch(ex: RestRequestException) {
                            if(ex.status.code == 403)
                                LOGGER.info("Tried to post album ${album.name} to channel $channelId, missing access")
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }

                db.updateLastRelease(artistIds, album.getReleaseDateTime()) // this won't be reached 99% of the time so batching is not important here
            }

            printToDiscord(client, LOGGER, "New albums found: ${count.get()}")
            client.editPresence {
                listening(pluralPrefixed("new release", count.get()))
            }
        }
        printToDiscord(client, LOGGER, "Checking new releases took $duration")
    }
}