package xyz.redslime.releaseradar.task

import com.adamratzman.spotify.models.Album
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER_CACHE
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
    private val subscribers = mutableListOf<RadarSubscriber>()

    fun subscribe(subscriber: RadarSubscriber) = subscribers.add(subscriber)

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

        var subscribersFailed = 0
        val cacheRemoved = cleanAlbumCache()
        val duration = measureTime {
            printToDiscord(client, LOGGER, "---- Checking new releases (removed $cacheRemoved from cache) ----")
            val artists = cache.getAllArtistsOnRadars()
            val count = AtomicInteger(0)
            val channel = Channel<Album>(Channel.UNLIMITED)

            // use a channel to preserve first-come-first-serve order (most popular artist releases are posted first)
            CoroutineScope(SupervisorJob()).launch {
                val postedAlbums = mutableSetOf<String>()

                try {
                    spotify.getAlbumsAfterFlow(artists).collect { sa ->
                        sa.toAlbum()?.let { album ->
                            if (postedAlbums.add(album.id)) {
                                count.getAndIncrement()
                                channel.send(album)
                            }
                        }
                    }
                } finally {
                    channel.close()
                }
            }

            for(album in channel) {
                // find radars this needs to be posted in
                val artistIds = album.artists.map { it.id }.distinct()
                val radars = cache.getAllRadarsWithArtists(artistIds)
                val excludedRadar = cache.getAllRadarsWithExcludedArtists(artistIds)

                // prepare album information
                val embedPair = try {
                    val color = getArtworkColor(album.images?.get(0)?.url ?: "")
                    val radarEmbed = buildAlbumEmbed(album, radarPost = true, color = color)
                    val dmEmbed = buildAlbumEmbed(album, radarPost = false, color = color)

                    // cache album information
                    val time = measureTime {
                        db.connect().use {
                            PostLaterCacheContainer.from(album, radarEmbed, dmEmbed).push(it)
                        }
                    }
                    LOGGER.info("Cached album ${album.name} in ${time.inWholeMilliseconds}ms")

                    Pair(radarEmbed, album.getSmartLink())
                } catch (ex: Exception) {
                    LOGGER.error("Failed to build or cache embed for ${album.id}, skipping!", ex)
                    continue
                }

                radars.forEach { radarId ->
                    cache.getChannelId(radarId)?.also { channelId ->
                        if(excludedRadar.contains(radarId)) {
                            LOGGER.info("Not posting ${album.name} since one of the artists is excluded on this radar")
                            return@also
                        }

                        try {
                            val ch = client.getChannel(Snowflake(channelId)) as MessageChannelBehavior
                            val timezone = cache.getRadarTimezone(radarId)

                            if(timezone == Timezone.ASAP) {
                                LOGGER.info("Posting ${album.name} now in $channelId")
                                postRadarAlbum(embedPair.second, embedPair.first, ch, radarId)
                            } else {
                                LOGGER.info("Scheduled to post ${album.name} later in $channelId (${timezone.name})")
                                DiscordClient.postLaterTask.add(album, channelId, timezone)
                            }
                        } catch(ex: RestRequestException) {
                            if(ex.status.code == 403)
                                LOGGER.info("Tried to post album ${album.name} to channel $channelId, missing access", ex)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            LOGGER.error(ex)
                        }
                    }
                }

                if(subscribers.isNotEmpty()) {
                    subscribers.forEach {
                        try {
                            it.onNewAlbum(album, radars)
                        } catch (ex: Exception) {
                            subscribersFailed++
                            LOGGER.error("Radar subscriber failed to process new album:", ex)
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

        if(subscribersFailed > 0)
            printToDiscord(
                client,
                LOGGER,
                "Failed to notify $subscribersFailed subscribers of new releases"
            )
    }

    private fun cleanAlbumCache(): Int {
        return db.connect().use { con ->
            con.deleteFrom(POST_LATER_CACHE)
                .where(POST_LATER_CACHE.ALBUM_ID.notIn(
                con.selectDistinct(POST_LATER.CONTENT_ID)
                        .from(POST_LATER)
                        .where(POST_LATER.CONTENT_ID.isNotNull)
                ))
            .execute()
        }
    }
}