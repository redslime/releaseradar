package xyz.redslime.releaseradar.playlist

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.toPlayableUri
import com.adamratzman.spotify.refreshSpotifyClientToken
import com.adamratzman.spotify.spotifyClientApi
import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.actionRow
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.command.ReminderPlaylistCommand
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.config
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.util.Interactable
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-06-16
 */
class PlaylistHandler(val duration: PlaylistDuration, val public: Boolean, val append: Boolean, var disabled: Boolean, val always: Boolean): Interactable {

    suspend fun postAlbums(user: User, albums: List<Album>) {
        val api = getClient(user)
        val userId = user.id.asLong()
        val playlistData = db.getUserPlaylistData(userId)
        val playlistPair = getPlaylist(api, playlistData, userId)
        val playlist = playlistPair.first
        val newPlaylist = playlistPair.second
        val playables = albums.flatMap { it.tracks.toList() }.mapNotNull { it?.uri?.uri?.toPlayableUri() }.toTypedArray()

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
                addInteractionButton(this, ButtonStyle.Secondary, "Change playlist settings") { i ->
                    commands.firstOrNull { it.name == "reminderplaylist" }?.let { (it as ReminderPlaylistCommand).sendPrompt(i) }
                }
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
        if(data == null) {
            return Pair(createNewPlaylist(api, userId), true)
        }

        val arr = data.split(";")

        if(arr.size != 2) {
            return Pair(createNewPlaylist(api, userId), true)
        }

        val date = arr[0]
        val id = arr[1]

        when(duration) {
            PlaylistDuration.NEVER -> {
                val playlist = api.playlists.getClientPlaylist(id)

                if(playlist == null) { // deleted or something, recreate
                    return Pair(createNewPlaylist(api, userId), true)
                } else {
                    if(!append) { // if we dont append, clear first
                        api.playlists.removeAllClientPlaylistPlayables(id)
                    }

                    return Pair(playlist.toFullPlaylist(Market.WS)!!, false)
                }
            }

            else -> {
                if(date == duration.getTitleToday()) {
                    val playlist = api.playlists.getClientPlaylist(id)

                    if (playlist != null) {
                        return Pair(playlist.toFullPlaylist(Market.WS)!!, false)
                    }
                }

                return Pair(createNewPlaylist(api, userId), true)
            }
        }
    }

    private suspend fun createNewPlaylist(api: SpotifyClientApi, userId: Long): Playlist {
        val playlist = api.playlists.createClientPlaylist(duration.getDefaultPlaylistName(), description = "Automatically generated by Release Radar discord bot", public = public, user = api.getUserId())
        db.setUserPlaylistData(userId, "${duration.getTitleToday()};${playlist.id}")
        return playlist
    }
}