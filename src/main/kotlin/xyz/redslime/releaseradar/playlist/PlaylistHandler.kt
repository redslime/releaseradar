package xyz.redslime.releaseradar.playlist

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.toPlayableUri
import com.adamratzman.spotify.refreshSpotifyClientToken
import com.adamratzman.spotify.spotifyClientApi
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.actionRow
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.config
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.db.releaseradar.tables.records.UserRecord
import xyz.redslime.releaseradar.util.DEFAULT_MARKET
import xyz.redslime.releaseradar.util.Interactable
import xyz.redslime.releaseradar.util.PostLaterCacheContainer
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-06-16
 */
class PlaylistHandler(val duration: PlaylistDuration, val public: Boolean, val append: Boolean, var disabled: Boolean, val always: Boolean): Interactable {

    private val logger = LogManager.getLogger(javaClass)

    suspend fun postAlbums(user: User, userRec: UserRecord?, albums: List<PostLaterCacheContainer>) {
        val api = getClient(user)
        val userId = user.id.asLong()
        val playlistData = userRec?.playlistData
        val skipExtended = userRec?.skipExtended ?: false
        val playlistPair = getPlaylist(api, playlistData, userId)
        val playlist = playlistPair.first
        val newPlaylist = playlistPair.second
        val playables = albums.flatMap { it.getTrackIds(skipExtended) }.map { it.toPlayableUri() }.toTypedArray()

        api.playlists.addPlayablesToClientPlaylist(playlist.id, *playables)
        playlist.externalUrls.spotify?.let { user.getDmChannelOrNull()?.createMessage {
            if(newPlaylist) {
                if(duration == PlaylistDuration.DAY || duration == PlaylistDuration.NEVER)
                    content = "Wow, that's a lot for today! Here's a playlist with all ${albums.size} releases: $it"
                else
                    content = "New ${duration.name.lowercase()}, new playlist! $it"
            } else {
                content = "Added ${pluralPrefixed("release", albums.size)} to the playlist: $it"
            }

            actionRow {
                addStaticInteractionButton("reminderplaylist", this, ButtonStyle.Secondary, "Change playlist settings")
            }
        } }
        api.shutdown()
    }

    private suspend fun getClient(user: User): SpotifyClientApi {
        val userId = user.id.asLong()
        var isUserToken = true
        var refreshToken = db.getUserRefreshToken(userId)

        if(refreshToken == null) {
            refreshToken = db.getSpotifyMasterRefreshToken()
            isUserToken = false
        }

        val token = refreshSpotifyClientToken(config.spotifyClientId, config.spotifySecret, refreshToken, false)
        val api = spotifyClientApi(
            clientId = config.spotifyClientId,
            clientSecret = config.spotifySecret,
            redirectUri = config.redirectUrl,
            authorization = SpotifyUserAuthorization(token = token)
        ){
            afterTokenRefresh = {
                it.token.refreshToken?.let { newToken ->
                    if(isUserToken)
                        db.updateUserRefreshToken(userId, newToken)
                    else
                        db.updateSpotifyMasterRefreshToken(newToken)
                }
            }
        }.build()
        return api
    }

    private suspend fun getPlaylist(api: SpotifyClientApi, data: String?, userId: Long): Pair<Playlist, Boolean> {
        logger.info("Playlist data for user $userId: $data")

        if(data == null) {
            logger.info("No playlist data found for user $userId, creating new one")
            return Pair(createNewPlaylist(api, userId), true)
        }

        val arr = data.split(";")

        if(arr.size != 2) {
            logger.info("Invalid playlist data found for user $userId, creating new one")
            return Pair(createNewPlaylist(api, userId), true)
        }

        val date = arr[0]
        val id = arr[1]

        when(duration) {
            PlaylistDuration.NEVER -> {
                val playlist = api.playlists.getClientPlaylist(id)

                if(playlist == null) { // deleted or something, recreate
                    logger.info("Failed to find playlist with id $id for $userId, creating new one")
                    return Pair(createNewPlaylist(api, userId), true)
                } else {
                    if(!append) { // if we dont append, clear first
                        api.playlists.removeAllClientPlaylistPlayables(id)
                    }

                    logger.info("Found existing playlist with id $id for $userId")
                    return Pair(playlist.toFullPlaylist(DEFAULT_MARKET)!!, false)
                }
            }

            else -> {
                if(date == duration.getTitleToday()) {
                    val playlist = api.playlists.getClientPlaylist(id)

                    if (playlist != null) {
                        logger.info("Found existing playlist with id $id for $userId")
                        return Pair(playlist.toFullPlaylist(DEFAULT_MARKET)!!, false)
                    }
                }

                logger.info("No existing playlist found for $userId, creating new one ($date|${duration.getTitleToday()})")
                return Pair(createNewPlaylist(api, userId), true)
            }
        }
    }

    private suspend fun createNewPlaylist(api: SpotifyClientApi, userId: Long): Playlist {
        api.getUserId()
        val playlist = api.playlists.createClientPlaylist(
            duration.getDefaultPlaylistName(),
            description = "Automatically generated by Release Radar discord bot",
            public = public
        )
        logger.info("Created new playlist ${playlist.name}(${playlist.id}) for user $userId")
        db.setUserPlaylistData(userId, "${duration.getTitleToday()};${playlist.id}")
        return playlist
    }
}