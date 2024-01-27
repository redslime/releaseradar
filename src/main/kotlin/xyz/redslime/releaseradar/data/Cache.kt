package xyz.redslime.releaseradar.data

import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleArtist
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.Channel
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.RadarChannelRecord
import xyz.redslime.releaseradar.util.NameCacheProvider
import xyz.redslime.releaseradar.util.Timezone
import java.time.LocalDateTime

/**
 * @author redslime
 * @version 2023-05-19
 */
class Cache : NameCacheProvider {

    private val radarChannels: MutableList<RadarChannelRecord> = ArrayList()
    private val artists: MutableList<ArtistRecord> = ArrayList()
    val configChannels: MutableMap<Long, Long> = HashMap()
    private val artistRadars: MutableList<ArtistRadarRecord> = ArrayList()

    fun findArtistRec(artist: SimpleArtist): ArtistRecord? {
        return artists.firstOrNull { it.id!! == artist.id }
    }

    fun findArtistRec(artist: Artist): ArtistRecord? {
        return artists.firstOrNull { it.id!! == artist.id }
    }

    fun findArtistRec(id: String): ArtistRecord? {
        return artists.firstOrNull { it.id == id }
    }

    override suspend fun findArtistRecByName(name: String, ignoreCase: Boolean): List<ArtistRecord> {
        return artists.filter { it.name.equals(name, ignoreCase = ignoreCase) }
    }

    fun getArtistName(id: String?): String? {
        return artists.firstOrNull { rec -> rec.id == id }?.name
    }

    fun getArtistRecordsInRadarChannel(channel: Channel): List<ArtistRecord> {
        val radarId = db.getRadarId(channel)
        return artistRadars
            .filter { rec -> rec.radarId == radarId }
            .mapNotNull { id -> artists.firstOrNull { it.id == id.artistId } }
    }

    fun getArtistNamesInRadarChannel(channel: Channel): Map<String, String> {
        val radarId = db.getRadarId(channel)
        return artistRadars
            .filter { rec -> rec.radarId == radarId }
            .map { rec -> getArtistName(rec.artistId) to rec.artistId!! }
            .filter { it.first != null }
            .sortedBy { it.first!!.lowercase() }
            .associate { it.first!! to it.second }
    }

    fun isOnRadar(artist: Artist, channel: Channel): Boolean {
        return isOnRadar(artist.toSimpleArtist(), channel)
    }

    fun isOnRadar(artist: SimpleArtist, channel: Channel): Boolean {
        val radarId = db.getRadarId(channel)
        return artistRadars.any { it.artistId == artist.id && it.radarId == radarId}
    }

    fun isOnRadar(artist: SimpleArtist, radarId: Int): Boolean {
        return artistRadars.any { it.artistId == artist.id && it.radarId == radarId}
    }

    fun getChannelId(radarId: Int): Long? {
        return radarChannels.filter { it.id == radarId }.firstNotNullOfOrNull { it.channelId }
    }

    fun getRadarId(channelId: Long): Int? {
        return radarChannels.filter { it.channelId == channelId }.firstNotNullOfOrNull { it.id }
    }

    fun getRadars(serverId: Long): List<RadarChannelRecord> {
        return radarChannels.filter { it.serverId == serverId }
    }

    fun getConfigChannelId(guildId: Long?): Long? {
        return configChannels[guildId]
    }

    fun addArtistRecord(rec: ArtistRecord) {
        synchronized(artists) {
            if (artists.none { it.id == rec.id })
                artists.add(rec)
        }
    }

    fun addRadarRecord(rec: RadarChannelRecord) {
        synchronized(radarChannels) {
            if(radarChannels.none { it.id == rec.id })
                radarChannels.add(rec)
        }
    }

    fun addArtistRadarRecord(rec: ArtistRadarRecord) {
        synchronized(artistRadars) {
            if(artistRadars.none { it.artistId == rec.artistId && it.radarId == rec.radarId })
                artistRadars.add(rec)
        }
    }

    fun removeArtistFromRadar(artist: Artist, rid: Int): Pair<Boolean, Boolean> {
        return removeArtistFromRadar(artist.toSimpleArtist(), rid)
    }

    fun removeArtistFromRadar(artist: SimpleArtist, rid: Int): Pair<Boolean, Boolean> {
        return removeArtistFromRadar(artist.id, rid)
    }

    fun removeArtistFromRadar(artistId: String, rid: Int): Pair<Boolean, Boolean> {
        val removed = artistRadars.removeIf { it.radarId == rid && it.artistId == artistId }

        if(artistRadars.none { it.artistId == artistId }) {
            // artist is on no radar anymore, remove it
            artists.removeIf { it.id == artistId }
            db.removeArtist(artistId)
            return Pair(removed, true)
        }

        return Pair(removed, false)
    }

    fun removeArtistsFromRadar(artistIds: List<String>, rid: Int) {
        val abandoned = mutableListOf<String>()

        artistIds.forEach { artistId ->
            artistRadars.removeIf { it.radarId == rid && it.artistId == artistId }

            if(artistRadars.none { it.artistId == artistId }) {
                artists.removeIf { it.id == artistId }
                abandoned.add(artistId)
            }
        }

        db.removeArtists(abandoned)
    }

    fun clearRadar(rid: Int) {
        val artists = artistRadars.filter { it.radarId == rid }.toList()
        artistRadars.removeIf { it.radarId == rid }

        // clean up abandoned artists
        val gone = artists.filter { a -> artistRadars.none { it.artistId == a.artistId } }
        db.removeArtists(gone.mapNotNull { it.artistId })
    }

    fun getAllArtistsOnRadars(): List<ArtistRecord> {
        return artistRadars.map { cache.findArtistRec(it.artistId!!) }.toList().filterNotNull().distinctBy { rec -> rec.id }
    }

    fun getAllRadarsWithArtists(artistIds: List<String>): List<Int> {
        return artistRadars.filter { artistIds.contains(it.artistId) }.mapNotNull { it.radarId }.distinct()
    }

    fun getServerRadarsWithArtist(artist: Artist, serverId: Long): List<Int> {
        val radars = getRadars(serverId).map { it.id }
        return artistRadars.filter { it.artistId == artist.id && radars.contains(it.radarId) }.toList().map { it.radarId!! }
    }

    fun getTotalArtistsOnRadar(): Int {
        return artistRadars.mapNotNull { it.artistId }.distinct().count()
    }

    fun getTotalRadarCount(): Int {
        return artistRadars.map { it.radarId }.distinct().count()
    }

    fun setRadarTimezone(radarId: Int, tz: Timezone) {
        radarChannels.filter { it.id == radarId }.forEach { it.timezone = tz.name }
    }

    fun getRadarTimezone(radarId: Int): Timezone {
        return radarChannels.filter { it.id == radarId }.map { Timezone.valueOf(it.timezone!!) }.firstOrNull() ?: Timezone.ASAP
    }

    fun setServerTimezone(serverId: Long, tz: Timezone) {
        radarChannels.filter { it.serverId == serverId }.forEach { it.timezone = tz.name }
    }

    fun getEmbedType(radarId: Int): EmbedType {
        return radarChannels.filter { it.id == radarId }.map { EmbedType.valueOf(it.embedType!!) }.firstOrNull() ?: EmbedType.CUSTOM
    }

    fun setEmbedType(radarId: Int, type: EmbedType) {
        radarChannels.filter { it.id == radarId }.forEach { it.embedType = type.name }
    }

    fun setServerEmbedType(serverId: Long, type: EmbedType) {
        radarChannels.filter { it.serverId == serverId }.forEach { it.embedType = type.name }
    }

    fun setRadarEmotes(radarId: Int, mentions: String) {
        radarChannels.filter { it.id == radarId }.forEach { it.emotes = mentions }
    }

    fun setServerEmotes(serverId: Long, mentions: String) {
        radarChannels.filter { it.serverId == serverId }.forEach { it.emotes = mentions }
    }

    fun getRadarEmotes(radarId: Int): List<ReactionEmoji> {
        val str = radarChannels.filter { it.id == radarId }.map { it.emotes }.firstOrNull()

        if(str != null)
            return str.split(",").map { ReactionEmoji.from(it) }.toList()
        return listOf(ReactionEmoji.Unicode("\uD83D\uDC4D"), ReactionEmoji.Unicode("\uD83D\uDC4E"), ReactionEmoji.Unicode("\u2764\uFE0F"))
    }

    fun updateLastRelease(artistIds: List<String>, releaseDate: LocalDateTime) {
        artistIds.forEach { aid ->
            artists.filter { it.id == aid }.forEach { it.lastRelease = releaseDate }
        }
    }

    fun getAllActiveRadars(): List<Long> {
        return artistRadars.mapNotNull { it.radarId }.distinct().mapNotNull { getChannelId(it) }.toList()
    }

    fun purgeServerData(serverId: Long) {
        val radars = radarChannels.filter { it.serverId == serverId }.map { it.id }.toList()
        val artists = radars.flatMap { rid -> artistRadars.filter { it.radarId == rid }.toList() }

        configChannels.remove(serverId)
        radars.forEach { rid -> artistRadars.removeIf { it.radarId == rid } }
        radarChannels.removeIf { it.serverId == serverId }

        // clean up abandoned artists
        val gone = artists.filter { a -> artistRadars.none { it.artistId == a.artistId } }
        db.removeArtists(gone.mapNotNull { it.artistId })
    }
}