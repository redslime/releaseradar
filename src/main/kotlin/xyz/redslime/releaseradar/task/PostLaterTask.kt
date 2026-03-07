package xyz.redslime.releaseradar.task

import com.adamratzman.spotify.models.Album
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.records.PostLaterRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER
import xyz.redslime.releaseradar.util.PostLaterCacheContainer
import xyz.redslime.releaseradar.util.Timezone
import xyz.redslime.releaseradar.util.coroutine
import xyz.redslime.releaseradar.util.getMillisUntilTopOfTheHour
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.measureTime

/**
 * @author redslime
 * @version 2023-05-26
 */
class PostLaterTask: Task(Duration.ofMillis(getMillisUntilTopOfTheHour()), Duration.ofMinutes(5)) {

    data class Entry(val albumId: String, val channelId: Long, val timezone: Timezone, val dm: Boolean)

    private val entries = mutableSetOf<Entry>()
    private val logger = LogManager.getLogger(javaClass)

    init {
        db.connect().use { con ->
            con.selectFrom(POST_LATER)
                .fetch()
                .forEach(this::add)
        }
    }

    fun getUniqueAlbumReminders(): Int {
        return entries.map { e -> e.albumId }.distinct().count()
    }

    override fun run(client: Kord): TimerTask.() -> Unit {
        return {
            coroutine {
                runActual(client)
            }
        }
    }

    suspend fun runActual(client: Kord, tz: Timezone? = null) {
        // find the timezone where it's midnight
        val tzz = tz ?: Timezone.entries.firstOrNull { ZonedDateTime.now(it.zone).hour == 0 }

        tzz?.let { timezone ->
            val userDms = mutableMapOf<Long, MutableList<PostLaterCacheContainer>>()
            val albumIds = entries.filter { it.timezone == timezone }.map { it.albumId }.distinct()

            if(albumIds.isNotEmpty())
                logger.info("Found ${albumIds.size} albums to post now")

            val duration = measureTime {
                try {
                    val cachedAlbums = db.connect().use { con ->
                        PostLaterCacheContainer.fetchAll(con, albumIds)
                    }

                    cachedAlbums.forEach { cachedAlbum ->
                        entries.filter { (it.timezone == timezone) && it.albumId == cachedAlbum.albumId }
                            .forEach { entry ->
                                if (entry.dm) {
                                    userDms.getOrPut(entry.channelId) { mutableListOf() }.add(cachedAlbum)
                                } else {
                                    client.getChannel(Snowflake(entry.channelId))?.let { channel ->
                                        postRadarAlbum(cachedAlbum.data.smartLink, cachedAlbum.getRadarEmbed(), channel as MessageChannelBehavior, db.getRadarId(channel))
                                    }
                                }
                            }
                    }
                } catch (ex: Exception) {
                    logger.error("Error trying to get albums to post reminders for:", ex)
                    return
                }

                userDms.forEach { (channelId, list) ->
                    try {
                        val playlistHandler = db.getUserPlaylistHandler(channelId)
                        val user = client.getUser(Snowflake(channelId)) ?: return
                        val userRec = db.getUserRecord(user.id.asLong())

                        if (!playlistHandler.disabled && (list.size >= 5 || playlistHandler.always)) {
                            try {
                                playlistHandler.postAlbums(user, userRec, list)
                            } catch (ex: Exception) {
                                logger.error("Tried to create/edit playlist for ${user.username}, failed:", ex)
                                sendIndividualLinks(user, list)
                            } finally {
                                entries.removeIf { it.channelId == channelId }
                                db.clearPostLater(channelId)
                            }
                        } else {
                            sendIndividualLinks(user, list)
                            entries.removeIf { it.channelId == channelId }
                            db.clearPostLater(channelId)
                        }
                    } catch (ex: Exception) {
                        logger.error("Error trying to determine reminder playlist handler:", ex)
                    }
                }
            }

            if(albumIds.isNotEmpty())
                logger.info("Posting ${albumIds.size} albums took $duration (${duration.toInt(DurationUnit.SECONDS) / albumIds.size}s/album)")

            // clear up channel reminders
            if(albumIds.isNotEmpty()) {
                entries.removeIf { !it.dm && (it.timezone == timezone) }
                db.clearPostLater(timezone)
            }
        }
    }

    fun add(album: Album, channelId: Long, timezone: Timezone) {
        add(Entry(album.id, channelId, timezone, false))
    }

    suspend fun add(albumId: String, user: User): Boolean {
        val userId = user.id.asLong()
        val timezone = db.getUserTimezone(userId)

        if(timezone == null) {
            postTimezonePrompt(user) {
                add(Entry(albumId, userId, this, true))
            }
            return true
        } else {
            return add(Entry(albumId, userId, timezone, true))
        }
    }

    private fun add(rec: PostLaterRecord): Boolean {
        return entries.add(Entry(rec.contentId!!, rec.channelId!!, Timezone.valueOf(rec.timezone!!), rec.userChannel!!))
    }

    private fun add(entry: Entry): Boolean {
        if(!entries.contains(entry)) {
            db.addPostLater(entry)
            entries.add(entry)
            return true
        }

        return false
    }

    private suspend fun sendIndividualLinks(user: User, albums: MutableList<PostLaterCacheContainer>) {
        val embeds = albums.map { it.getDmEmbed() }

        user.getDmChannelOrNull()?.let {
            embeds.chunked(10).forEach { chunk ->
                it.createMessage {
                    chunk.forEach { embed ->
                        addEmbed(embed)
                    }
                }
            }
        }
    }
}