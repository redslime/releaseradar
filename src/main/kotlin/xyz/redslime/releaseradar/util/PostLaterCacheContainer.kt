package xyz.redslime.releaseradar.util

import com.adamratzman.spotify.models.Album
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.EmbedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jooq.DSLContext
import xyz.redslime.releaseradar.buildAlbumEmbed
import xyz.redslime.releaseradar.db.releaseradar.tables.references.POST_LATER_CACHE
import xyz.redslime.releaseradar.getSmartLink
import xyz.redslime.releaseradar.toEmbedBuilder

data class PostLaterCacheContainer(
    val albumId: String,
    val data: Data
) {
    @Serializable
    data class Data(
        val smartLink: String?,
        val trackIds: List<String>,
        val trackExtendedIds: List<String>,
        val radarEmbedJson: String,
        val dmEmbedJson: String,
    )

    companion object {
        suspend fun from(album: Album): PostLaterCacheContainer {
            val color = getArtworkColor(album.images?.get(0)?.url ?: "")
            val radarEmbed = buildAlbumEmbed(album, radarPost = true, color = color)
            val dmEmbed = buildAlbumEmbed(album, radarPost = false, color = color)

            return from(album, radarEmbed, dmEmbed)
        }

        suspend fun from(album: Album, radarEmbed: EmbedBuilder, dmEmbed: EmbedBuilder): PostLaterCacheContainer {
            val smartLink = album.getSmartLink()
            val tracks = album.tracks.getAllItems().filterNotNull()
            val trackIds = tracks.filter { !it.name.lowercase().contains("extended") }.map { it.id }
            val extendedIds = tracks.filter { it.name.lowercase().contains("extended") }.map { it.id }
            val radarEmbedJson = Json.encodeToString(EmbedRequest.serializer(), radarEmbed.toRequest())
            val dmEmbedJson = Json.encodeToString(EmbedRequest.serializer(), dmEmbed.toRequest())

            return PostLaterCacheContainer(album.id, Data(smartLink, trackIds, extendedIds, radarEmbedJson, dmEmbedJson))
        }

        suspend fun fetchAll(con: DSLContext, albumIds: List<String>): List<PostLaterCacheContainer> {
            val recs = withContext(Dispatchers.IO) {
                con.selectFrom(POST_LATER_CACHE)
                    .where(POST_LATER_CACHE.ALBUM_ID.`in`(albumIds))
                    .fetch()
            }

            return recs.map { PostLaterCacheContainer(it.albumId!!, Json.decodeFromString<Data>(it.container!!)) }
        }
    }

    fun getTrackIds(skipExtended: Boolean = false): List<String> {
        return if(skipExtended) data.trackIds else data.trackIds + data.trackExtendedIds
    }

    fun getRadarEmbed(): EmbedBuilder {
        return Json.decodeFromString<EmbedRequest>(data.radarEmbedJson).toEmbedBuilder()
    }

    fun getDmEmbed(): EmbedBuilder {
        return Json.decodeFromString<EmbedRequest>(data.dmEmbedJson).toEmbedBuilder()
    }

    suspend fun push(con: DSLContext) {
        withContext(Dispatchers.IO) {
            con.insertInto(POST_LATER_CACHE)
                .set(POST_LATER_CACHE.ALBUM_ID, albumId)
                .set(POST_LATER_CACHE.CONTAINER, Json.encodeToString(data))
                .onDuplicateKeyIgnore()
                .execute()
        }
    }
}