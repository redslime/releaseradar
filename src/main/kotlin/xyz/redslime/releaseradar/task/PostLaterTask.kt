package xyz.redslime.releaseradar.task

import com.adamratzman.spotify.models.Album
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.records.PostLaterRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER
import xyz.redslime.releaseradar.util.Timezone
import xyz.redslime.releaseradar.util.coroutine
import xyz.redslime.releaseradar.util.getArtworkColor
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

    private val entries = mutableSetOf<Entry>()
    private val logger = LogManager.getLogger(javaClass)

    init {
        db.connect().selectFrom(POST_LATER)
            .fetch()
            .forEach(this::add)
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
        // find the timezone where its midnight
        val tzz = tz ?: Timezone.entries.firstOrNull { ZonedDateTime.now(it.zone).hour == 0 }

        tzz?.let { timezone ->
            val userDms = mutableMapOf<Long, MutableList<Pair<Album, EmbedBuilder>>>()
            val albumIds = entries.filter { it.timezone == timezone }.map { it.albumId }.distinct()

            if(albumIds.isNotEmpty())
                logger.info("Found ${albumIds.size} albums to post now")

            try {
                spotify.getAlbumsBatch(albumIds).forEach { album ->
                    // analyse album art just once here
                    val embedColor = getArtworkColor(album.images?.get(0)?.url ?: "")

                    entries.filter { (it.timezone == timezone) && it.albumId == album.id }
                        .forEach { entry ->
                            if (entry.dm) {
                                val embed = buildAlbumEmbed(album, radarPost = false, color = embedColor)
                                userDms.getOrPut(entry.channelId) { mutableListOf() }.add(Pair(album, embed))
                            } else {
                                client.getChannel(Snowflake(entry.channelId))?.let { channel ->
                                    val embed = buildAlbumEmbed(album, radarPost = true, color = embedColor)
                                    postRadarAlbum(album, embed, channel as MessageChannelBehavior, db.getRadarId(channel))
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
                    val skipExtended = userRec?.skipExtended ?: false

                    if (!playlistHandler.disabled && (list.size >= 5 || playlistHandler.always)) {
                        try {
                            playlistHandler.postAlbums(user, userRec, list.map { it.first })
                        } catch (ex: Exception) {
                            logger.error("Tried to create/edit playlist for ${user.username}, failed:", ex)
                            sendIndividualLinks(user, skipExtended, list)
                        } finally {
                            entries.removeIf { it.channelId == channelId }
                            db.clearPostLater(channelId)
                        }
                    } else {
                        sendIndividualLinks(user, skipExtended, list)
                        entries.removeIf { it.channelId == channelId }
                        db.clearPostLater(channelId)
                    }
                } catch (ex: Exception) {
                    logger.error("Error trying to determine reminder playlist handler:", ex)
                }
            }

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
        if(entries.add(entry)) {
            db.addPostLater(entry)
            return true
        }

        return false
    }

    private suspend fun sendIndividualLinks(user: User, skipExtended: Boolean, albums: MutableList<Pair<Album, EmbedBuilder>>) {
        val embeds = mutableListOf<EmbedBuilder>()

        if(skipExtended) {
            // this makes only sense here if there are 2 tracks on the album, one of them being the extended one
            albums.forEach { (album, embed) ->
                if(album.totalTracks == 2 && album.tracks.count { it?.name?.lowercase()?.contains("extended") == true } == 1) {
                    album.tracks.filterNotNull().find { !it.name.lowercase().contains("extended") }?.let {
                        embeds.add(buildTrackEmbed(it))
                    }
                } else {
                    album.getSmartLink()?.let { embeds.add(embed) }
                }
            }
        } else {
            albums.forEach { (_, embed) ->
                embeds.add(embed)
            }
        }

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