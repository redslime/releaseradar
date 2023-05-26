package xyz.redslime.releaseradar.task

import com.adamratzman.spotify.models.SimpleAlbum
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.records.PostLaterRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER
import xyz.redslime.releaseradar.util.Timezone
import xyz.redslime.releaseradar.util.getMillisUntilTopOfTheHour
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

/**
 * @author redslime
 * @version 2023-05-26
 */
class PostLaterTask: Task(Duration.ofMillis(getMillisUntilTopOfTheHour()), Duration.ofMinutes(5)) {

    data class Entry(val albumId: String, val channelId: Long, val timezone: Timezone, val dm: Boolean)

    private val entries = mutableListOf<Entry>()
    private val logger = LogManager.getLogger(javaClass)

    init {
        db.connect().selectFrom(POST_LATER)
            .fetch()
            .forEach(this::add)
    }

    override fun run(client: Kord): TimerTask.() -> Unit {
        return {
            runBlocking {
                launch {
                    runActual(client)
                }
            }
        }
    }

    private suspend fun runActual(client: Kord) {
        // find the timezone where its midnight
        Timezone.values().firstOrNull { ZonedDateTime.now(it.zone).hour == 0 }?.let { timezone ->
            val albumIds = entries.filter { it.timezone == timezone }.map { it.albumId }

            if(albumIds.isNotEmpty())
                logger.info("Found ${albumIds.size} albums to post now")

            spotify.getAlbumsBatch(albumIds).forEach { album ->
                entries.filter { it.timezone == timezone && it.albumId == album.id }.forEach { entry ->
                    if(entry.dm) {
                        client.getUser(Snowflake(entry.channelId))?.getDmChannelOrNull()?.let {
                            it.createMessage {
                                content = album.getSmartLink()
                            }
                        }
                    } else {
                        client.getChannel(Snowflake(entry.channelId))?.let { channel ->
                            postAlbum(album, channel as MessageChannelBehavior, db.getRadarId(channel))
                        }
                    }
                }
            }

            entries.removeIf { it.timezone == timezone }
            db.clearPostLater(timezone)
        }
    }

    fun add(album: SimpleAlbum, channelId: Long, timezone: Timezone) {
        add(Entry(album.id, channelId, timezone, false))
    }

    suspend fun add(albumId: String, user: User) {
        val userId = user.id.asLong()
        val timezone = db.getUserTimezone(userId)

        if(timezone == null) {
            postTimezonePrompt(user) {
                add(Entry(albumId, userId, this, true))
            }
        } else {
            add(Entry(albumId, userId, timezone, true))
        }
    }

    fun add(rec: PostLaterRecord) {
        add(Entry(rec.contentId!!, rec.channelId!!, Timezone.valueOf(rec.timezone!!), rec.userChannel!!))
    }

    private fun add(entry: Entry) {
        db.addPostLater(entry)
        entries.add(entry)
    }

    fun reset() {
        db.clearPostLater()
        entries.clear()
    }
}