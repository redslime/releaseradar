package xyz.redslime.releaseradar.data

import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleArtist
import dev.kord.core.Kord
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.ResolvedChannel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.apache.logging.log4j.LogManager
import org.jooq.DSLContext
import org.jooq.conf.MappedSchema
import org.jooq.conf.RenderMapping
import org.jooq.impl.DSL
import xyz.redslime.releaseradar.EmbedType
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.config
import xyz.redslime.releaseradar.db.releaseradar.Releaseradar
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.references.*
import xyz.redslime.releaseradar.getDbId
import xyz.redslime.releaseradar.playlist.PlaylistDuration
import xyz.redslime.releaseradar.playlist.PlaylistHandler
import xyz.redslime.releaseradar.task.PostLaterTask
import xyz.redslime.releaseradar.util.Timezone
import java.time.Duration
import java.time.LocalDateTime

/**
 * @author redslime
 * @version 2023-05-19
 */
class Database(private val cache: Cache, private val host: String, private val user: String, private val pass: String) {

    init {
        Class.forName(config.dbDriverName)

        try {
            connect().selectFrom(RADAR_CHANNEL).limit(1).fetch()
        } catch (ex: Exception) {
            LogManager.getLogger(javaClass).info("Generating database tables for the first time!")
            connect().ddl(Releaseradar()).executeBatch()
        }

        connect().selectFrom(RADAR_CHANNEL)
            .fetch()
            .forEach(cache::addRadarRecord)
        connect().selectFrom(ARTIST)
            .fetch()
            .forEach(cache::addArtistRecord)
        connect().selectFrom(CONFIG_CHANNEL)
            .fetch()
            .map { rec -> cache.configChannels[rec.serverId!!] = rec.channelId!! }
        connect().selectFrom(ARTIST_RADAR)
            .fetch()
            .forEach(cache::addArtistRadarRecord)
    }

    fun connect(): DSLContext {
        val context = DSL.using(host, user, pass)

        if(config.dbUser == "releaseradar_test") {
            context.settings().withRenderMapping(
                RenderMapping()
                    .withSchemata(MappedSchema().withInput("releaseradar").withOutput("releaseradar_test"))
            )
        }

        return context
    }

    fun getRadarId(channel: Channel): Int {
        return cache.getRadarId(channel.getDbId()) ?: createRadar(channel)
    }

    fun createRadar(channel: Channel): Int {
        val rec = connect().newRecord(RADAR_CHANNEL).apply {
            channelId = channel.getDbId()
            serverId = channel.data.guildId.value!!.asLong()
        }.also {
            it.insert()
            it.refresh()
            cache.addRadarRecord(it)
        }

        return rec.id!!
    }

    fun checkArtist(artist: Artist) {
        cache.findArtistRec(artist)?:run {
            connect().newRecord(ARTIST).apply {
                id = artist.id
                name = artist.name
                lastRelease = LocalDateTime.now()
            }.also {
                it.insert()
                it.refresh()
                cache.addArtistRecord(it)
            }
        }
    }

    fun checkArtists(artists: Collection<SimpleArtist>) {
        val unknown = artists.filter { cache.findArtistRec(it) == null }.toList()

        if(unknown.isEmpty())
            return

        val con = connect()
        val recs: List<ArtistRecord> = unknown.map { artist ->
            con.newRecord(ARTIST).apply {
                id = artist.id
                name = artist.name
                lastRelease = LocalDateTime.now()
            }
        }
        recs.map {
            con.insertInto(ARTIST)
                .set(it)
                .onDuplicateKeyIgnore()
        }.let { queries -> con.batch(queries).execute() }
        recs.forEach { cache.addArtistRecord(it) }
    }

    fun addArtistToRadar(artist: Artist, channel: ResolvedChannel): Boolean {
        return addArtistToRadar(artist, getRadarId(channel))
    }

    fun addArtistToRadar(artist: Artist, rid: Int): Boolean {
        checkArtist(artist)
        var success: Int

        val rec = connect().newRecord(ARTIST_RADAR).apply {
            artistId = artist.id
            radarId = rid
        }

        try {
            success = rec.insert()
            rec.refresh()
        } catch (ex: Exception) {
            success = 0
        }

        cache.addArtistRadarRecord(rec)

        return success == 1
    }

    fun addArtistsToRadar(artists: Collection<SimpleArtist>, rid: Int): List<SimpleArtist> {
        checkArtists(artists)

        val skipped: MutableList<SimpleArtist> = ArrayList()
        val newArtists = artists.filter { !cache.isOnRadar(it, rid) }
        skipped.addAll(artists.filter { cache.isOnRadar(it, rid) })

        if(newArtists.isEmpty())
            return skipped

        val con = connect()
        val recs: List<ArtistRadarRecord> = newArtists.map { artist ->
            con.newRecord(ARTIST_RADAR).apply {
                artistId = artist.id
                radarId = rid
            }
        }
        val array = recs.map {
            con.insertInto(ARTIST_RADAR)
                .set(it)
                .onDuplicateKeyIgnore()
        }.let { queries -> con.batch(queries).execute() }

        skipped.addAll(array.mapIndexed { index, insertCount ->
            if(insertCount == 0)
                newArtists[index]
            else
                null
        }.filterNotNull())

        recs.forEach(cache::addArtistRadarRecord)
        return skipped
    }

    fun removeArtistFromRadar(artist: Artist, channel: ResolvedChannel): Boolean {
        val rid = getRadarId(channel)
        val removed = cache.removeArtistFromRadar(artist, rid)

        if(removed.first) {
            return connect().deleteFrom(ARTIST_RADAR)
                .where(ARTIST_RADAR.RADAR_ID.eq(rid))
                .and(ARTIST_RADAR.ARTIST_ID.eq(artist.id))
                .execute() == 1 || removed.second
        }

        return false
    }

    fun removeArtistsFromRadar(artists: List<SimpleArtist>, channel: ResolvedChannel): List<SimpleArtist> {
        val rid = getRadarId(channel)
        val con = connect()
        val array = artists.map {
            con.deleteFrom(ARTIST_RADAR)
                .where(ARTIST_RADAR.RADAR_ID.eq(rid))
                .and(ARTIST_RADAR.ARTIST_ID.eq(it.id))
        }.let { queries -> con.batch(queries).execute() }

        val skipped: List<SimpleArtist> = array.mapIndexed { index, i ->
            if(i == 0)
                artists[index]
            else
                null
        }.filterNotNull()

        artists.forEach { cache.removeArtistFromRadar(it, rid) }
        return skipped
    }

    fun removeArtistIdsFromRadar(artistIds: List<String>, channel: ResolvedChannel): List<String> {
        return removeArtistIdsFromRadar(artistIds, getRadarId(channel))
    }

    fun removeArtistIdsFromRadar(artistIds: List<String>, rid: Int): List<String> {
        val con = connect()
        val array = artistIds.map {
            con.deleteFrom(ARTIST_RADAR)
                .where(ARTIST_RADAR.RADAR_ID.eq(rid))
                .and(ARTIST_RADAR.ARTIST_ID.eq(it))
        }.let { queries -> con.batch(queries).execute() }

        val skipped: List<String> = array.mapIndexed { index, i ->
            if(i == 0)
                artistIds[index]
            else
                null
        }.filterNotNull()

        cache.removeArtistsFromRadar(artistIds, rid)
        return skipped
    }

    fun updateLastCheck(artists: List<ArtistRecord>) {
        val ids = artists.map { it.id!! }
        connect().update(ARTIST)
            .set(ARTIST.LAST_RELEASE, LocalDateTime.now())
            .where(ARTIST.ID.`in`(ids))
            .execute()
    }

    fun updateLastRelease(artists: List<String>, releaseDate: LocalDateTime) {
        cache.updateLastRelease(artists, releaseDate)
        connect().update(ARTIST)
            .set(ARTIST.LAST_RELEASE, releaseDate)
            .where(ARTIST.ID.`in`(artists))
            .execute()
    }

    fun setConfigChannel(serverId: Long?, channelId: Long): Boolean {
        return serverId?.let { gid ->
            cache.configChannels[gid] = channelId
            connect().insertInto(CONFIG_CHANNEL)
                .set(CONFIG_CHANNEL.SERVER_ID, gid)
                .set(CONFIG_CHANNEL.CHANNEL_ID, channelId)
                .onDuplicateKeyUpdate()
                .set(CONFIG_CHANNEL.CHANNEL_ID, channelId)
                .execute() == 1
        } ?: false
    }

    fun purgeServerData(serverId: Long) {
        connect().deleteFrom(CONFIG_CHANNEL)
            .where(CONFIG_CHANNEL.SERVER_ID.eq(serverId))
            .execute()
        connect().deleteFrom(RADAR_CHANNEL)
            .where(RADAR_CHANNEL.SERVER_ID.eq(serverId))
            .execute()
        cache.purgeServerData(serverId)
    }

    fun clearRadar(radarId: Int) {
        connect().deleteFrom(ARTIST_RADAR)
            .where(ARTIST_RADAR.RADAR_ID.eq(radarId))
            .execute()
        cache.clearRadar(radarId)
    }

    fun setLastUpdatedTracks() {
        connect().insertInto(INFO)
            .set(INFO.KEY, "last_scan")
            .set(INFO.VALUE, "${System.currentTimeMillis()}")
            .onDuplicateKeyUpdate()
            .set(INFO.VALUE, "${System.currentTimeMillis()}")
            .execute()
    }

    fun getDurationSinceLastUpdatedTracks(): Duration {
        val rec = connect().selectFrom(INFO)
            .where(INFO.KEY.eq("last_scan"))
            .fetchOne()

        return rec?.let {
            it.value?.let { time ->
                val lastScan = time.toLong()
                Duration.ofMillis(System.currentTimeMillis() - lastScan)
            }
        } ?: Duration.ofDays(1)
    }

    fun getUserTimezone(userId: Long): Timezone? {
        return connect().selectFrom(USER)
            .where(USER.ID.eq(userId))
            .fetchOne()?.timezone?.let {
                Timezone.valueOf(it)
            }
    }

    fun setUserTimezone(userId: Long, timezone: Timezone) {
        connect().insertInto(USER)
            .set(USER.ID, userId)
            .set(USER.TIMEZONE, timezone.name)
            .onDuplicateKeyUpdate()
            .set(USER.TIMEZONE, timezone.name)
            .execute()
    }

    fun setRadarTimezone(radarId: Int, timezone: Timezone) {
        cache.setRadarTimezone(radarId, timezone)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.TIMEZONE, timezone.name)
            .where(RADAR_CHANNEL.ID.eq(radarId))
            .execute()
    }

    fun setServerTimezone(serverId: Long, timezone: Timezone) {
        cache.setServerTimezone(serverId, timezone)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.TIMEZONE, timezone.name)
            .where(RADAR_CHANNEL.SERVER_ID.eq(serverId))
            .execute()
    }

    fun addPostLater(entry: PostLaterTask.Entry) {
        connect().insertInto(POST_LATER)
            .set(POST_LATER.CONTENT_ID, entry.albumId)
            .set(POST_LATER.TIMEZONE, entry.timezone.name)
            .set(POST_LATER.CHANNEL_ID, entry.channelId)
            .set(POST_LATER.USER_CHANNEL, entry.dm)
            .execute()
    }

    fun clearPostLater(timezone: Timezone) {
        connect().deleteFrom(POST_LATER)
            .where(POST_LATER.TIMEZONE.eq(timezone.name))
            .and(POST_LATER.USER_CHANNEL.isFalse)
            .execute()
    }

    fun clearPostLater(userId: Long) {
        connect().deleteFrom(POST_LATER)
            .where(POST_LATER.CHANNEL_ID.eq(userId))
            .execute()
    }

    fun clearPostLater() {
        connect().deleteFrom(POST_LATER)
            .where(DSL.trueCondition())
            .execute()
    }

    fun setRadarEmbedType(radarId: Int, type: EmbedType) {
        cache.setEmbedType(radarId, type)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.EMBED_TYPE, type.name)
            .where(RADAR_CHANNEL.ID.eq(radarId))
            .execute()
    }

    fun setServerEmbedType(serverId: Long, type: EmbedType) {
        cache.setServerEmbedType(serverId, type)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.EMBED_TYPE, type.name)
            .where(RADAR_CHANNEL.SERVER_ID.eq(serverId))
            .execute()
    }

    fun setRadarEmotes(radarId: Int, mentions: String) {
        cache.setRadarEmotes(radarId, mentions)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.EMOTES, mentions)
            .where(RADAR_CHANNEL.ID.eq(radarId))
            .execute()
    }

    fun setServerEmotes(serverId: Long, mentions: String) {
        cache.setServerEmotes(serverId, mentions)
        connect().update(RADAR_CHANNEL)
            .set(RADAR_CHANNEL.EMOTES, mentions)
            .where(RADAR_CHANNEL.SERVER_ID.eq(serverId))
            .execute()
    }

    fun updateUserRefreshToken(userId: Long, token: String) {
        connect().insertInto(USER)
            .set(USER.ID, userId)
            .set(USER.REFRESH_TOKEN, token)
            .onDuplicateKeyUpdate()
            .set(USER.REFRESH_TOKEN, token)
            .execute()
    }

    fun updateSpotifyMasterRefreshToken(token: String) {
        connect().insertInto(TOKEN)
            .set(TOKEN.ID, "spotify_master")
            .set(TOKEN.VALUE, token)
            .onDuplicateKeyUpdate()
            .set(TOKEN.VALUE, token)
            .execute()
    }

    fun getSpotifyMasterRefreshToken(): String? {
        return connect().selectFrom(TOKEN)
            .where(TOKEN.ID.eq("spotify_master"))
            .fetchOne()?.value
    }

    fun getUserRefreshToken(userId: Long): String? {
        return connect().selectFrom(USER)
            .where(USER.ID.eq(userId))
            .fetchOne()?.refreshToken
    }

    fun setUserPlaylistHandler(userId: Long, handler: PlaylistHandler?) {
        if(handler != null) {
            connect().update(USER)
                .set(USER.PLAYLIST_TYPE, "${handler.duration.name};${handler.public};${handler.append};${handler.disabled};${handler.always}")
                .where(USER.ID.eq(userId))
                .execute()
        } else {
            connect().update(USER)
                .setNull(USER.PLAYLIST_DATA)
                .setNull(USER.PLAYLIST_TYPE)
                .where(USER.ID.eq(userId))
                .execute()
        }
    }

    fun getUserPlaylistHandler(userId: Long): PlaylistHandler {
        val rec = connect().selectFrom(USER)
            .where(USER.ID.eq(userId))
            .fetchOne()

        if(rec != null && rec.playlistType?.isNotBlank() == true) {
            val arr = rec.playlistType!!.split(";")
            val duration = PlaylistDuration.valueOf(arr[0])
            return PlaylistHandler(duration, arr[1].toBoolean(), arr[2].toBoolean(), arr[3].toBoolean(), arr[4].toBoolean())
        }

        val handler = PlaylistHandler(PlaylistDuration.DAY, true, true, false, false)
        setUserPlaylistHandler(userId, handler)
        return handler
    }

    fun getUserPlaylistData(userId: Long): String? {
        return connect().selectFrom(USER)
            .where(USER.ID.eq(userId))
            .fetchOne()?.playlistData
    }

    fun setUserPlaylistData(userId: Long, data: String?) {
        if(data != null) {
            connect().update(USER)
                .set(USER.PLAYLIST_DATA, data)
                .where(USER.ID.eq(userId))
                .execute()
        } else {
            connect().update(USER)
                .setNull(USER.PLAYLIST_DATA)
                .where(USER.ID.eq(userId))
                .execute()
        }
    }

    fun removeArtist(artistId: String) {
        connect().deleteFrom(ARTIST)
            .where(ARTIST.ID.eq(artistId))
            .execute()
    }

    fun removeArtists(artistIds: List<String>) {
        connect().deleteFrom(ARTIST)
            .where(ARTIST.ID.`in`(artistIds))
            .execute()
    }

    fun isUserEnlisted(userId: Long): Boolean {
        return connect().selectFrom(USER)
            .where(USER.ID.eq(userId))
            .fetchOne()?.enlisted ?: false
    }

    fun setUserEnlisted(userId: Long, enlisted: Boolean): Boolean {
        return connect().update(USER)
            .set(USER.ENLISTED, enlisted)
            .where(USER.ID.eq(userId))
            .execute() == 1
    }

    fun logUserReact(client: Kord, userId: Long, serverId: Long, albumId: String, date: Instant, like: Boolean? = null, dislike: Boolean? = null, heart: Boolean? = null, clock: Boolean? = null) {
        if(client.selfId.asLong() == userId)
            return

        var step = connect().insertInto(USER_STAT)
            .set(USER_STAT.USER_ID, userId)
            .set(USER_STAT.SERVER_ID, serverId)
            .set(USER_STAT.ALBUM_ID, albumId)

        if(like != null)
            step = step.set(USER_STAT.LIKE, like)
        if(dislike != null)
            step = step.set(USER_STAT.DISLIKE, dislike)
        if(heart != null)
            step = step.set(USER_STAT.HEART, heart)
        if(clock != null)
            step = step.set(USER_STAT.CLOCK, clock)

        var on = step.set(USER_STAT.TIMESTAMP, date.toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime())
            .onDuplicateKeyUpdate()
            .set(USER_STAT.USER_ID, userId)

        if(like != null)
            on = on.set(USER_STAT.LIKE, like)
        if(dislike != null)
            on = on.set(USER_STAT.DISLIKE, dislike)
        if(heart != null)
            on = on.set(USER_STAT.HEART, heart)
        if(clock != null)
            on = on.set(USER_STAT.CLOCK, clock)

        on.execute()
    }
}