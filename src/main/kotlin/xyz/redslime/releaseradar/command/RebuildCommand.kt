package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.SimpleArtist
import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.util.albumRegex
import xyz.redslime.releaseradar.util.extractSpotifyLink

/**
 * @author redslime
 * @version 2023-12-16
 */
class RebuildCommand: Command("rebuild", "Rebuild a radar from its old releases", perm = PermissionLevel.SERVER_ADMIN) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "Channel to read from", req = true)
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = getChannelInput(interaction)!!

        if(interaction.user.id.asLong() != 115834525329653760L) {
            respondErrorEmbed(interaction.deferEphemeralResponse(), "You can't use this!")
            return
        }

        val response = interaction.deferEphemeralResponse()
        val urls = mutableListOf<String>()
        val ch = channel.fetchChannel() as MessageChannelBehavior

        val re = response.respond {
            content = "Starting scan of ${channel.mention}"
        }

        // extract all spotify album urls from channel
        channel.data.lastMessageId.value?.let { lastMessage ->
            ch.getMessagesBefore(lastMessage)
                .takeWhile { true }
                .filter { it.author == interaction.kord.getSelf() }
                .toList()
                .forEach { message ->
                    extractSpotifyLink(message)?.let { url ->
                        urls.add(url)
                    }
                }
        }

        re.createEphemeralFollowup {
            content = "Found ${urls.size} urls, fetching artists"
        }

        // extract ids from urls
        val ids = urls.map { it.replace(albumRegex, "$1") }.toMutableSet()
        ids.removeIf { it.trim().isEmpty() }

        re.createEphemeralFollowup {
            content = "Fetched ${ids.size} artist ids, fetching album objects & extracting artists"
        }

        // fetch artists from spotify
        val albums = mutableSetOf<Album>()
        ids.chunked(50).forEach {
            albums.addAll(spotify.api.albums.getAlbums(*it.toTypedArray(), market = Market.WS).filterNotNull())
        }

        // get artists from albums
        val artists = mutableSetOf<SimpleArtist>()
        albums.forEach { album -> album.artists.forEach { artist -> artists.add(artist) } }

        re.createEphemeralFollowup {
            content = "Fetched ${artists.size} artist objects, inserting into radar"
        }

        // insert into radar
        val radarId = db.getRadarId(channel)
        val added = db.addArtistsToRadar(artists, radarId)

        re.createEphemeralFollowup {
            content = ":white_check_mark: Added ${artists.size - added.size} to radar ${channel.mention}"
        }
    }
}