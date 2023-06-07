package xyz.redslime.releaseradar.task

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.Timezone
import xyz.redslime.releaseradar.util.getMillisUntilMidnightNZ
import xyz.redslime.releaseradar.util.printToDiscord
import java.time.Duration
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * @author redslime
 * @version 2023-05-19
 */
class ScanNewTracksTask : Task(Duration.ofMillis(getMillisUntilMidnightNZ()), Duration.ofDays(1)) {

    private val LOGGER = LogManager.getLogger(javaClass)

    override fun run(client: Kord): TimerTask.() -> Unit {
        return {
            runBlocking {
                launch {
                    runActual(client)
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun runActual(client: Kord) {
        val lastScan = db.getDurationSinceLastUpdatedTracks()

        if(lastScan < Duration.ofHours(2)) {
            LOGGER.info("Last scan of new releases was too recent ($lastScan), skipping")
            return
        }

        db.setLastUpdatedTracks()
        DiscordClient.postLaterTask.reset()

        val duration = measureTime {
            printToDiscord(client, LOGGER, "---- Checking new releases ----")
            val artists = cache.getAllArtistsOnRadars()
            val albums = artists.flatMap { spotify.getAlbumsAfter(it.id!!, it.lastRelease) }.toList().distinctBy { it.id }

            printToDiscord(client, LOGGER, "New albums found: ${albums.size}")

            albums.forEach { album ->
                // find radars this needs to be posted in
                val artistIds = album.artists.map { it.id }.distinct()
                val radars = cache.getAllRadarsWithArtists(artistIds)

                radars.forEach { radarId ->
                    cache.getChannelId(radarId)?.also { channelId ->
                        try {
                            val channel = client.getChannel(Snowflake(channelId)) as MessageChannelBehavior
                            val timezone = cache.getRadarTimezone(radarId)

                            if(timezone == Timezone.ASAP) {
                                LOGGER.info("Posting ${album.name} now in $channelId")
                                postAlbum(album.toAlbum(), channel, radarId)
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
        }
        printToDiscord(client, LOGGER, "Checking new releases took $duration")
    }
}