package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.request.RestRequestException
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.albumRegex

/**
 * @author redslime
 * @version 2023-05-21
 */
class PrintCommand: ArtistCommand("print", "Prints the lastest release of the specified artist to the specified channel to test permissions", singleOnly = true, perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel to post the latest release of the specified artist to")
    }

    override suspend fun handleArtist(
        artist: Artist,
        response: DeferredMessageInteractionResponseBehavior,
        interaction: ChatInputCommandInteraction
    ) {
        val channel = getChannelInput(interaction)!!
        val radarId = db.getRadarId(channel)

        spotify.getLatestRelease(artist.id)?.toAlbum()?.also { album ->
            try {
                postRadarAlbum(album, channel.fetchChannel() as MessageChannelBehavior, radarId)
                respondSuccessEmbed(response, desc =  "Posted latest release of ${artist.name} to ${channel.mention}")
            } catch (ex: RestRequestException) {
                if (ex.status.code == 403)
                    respondErrorEmbed(response, "No permission to post messages in ${channel.mention} :(")
            } catch (ex: Exception) {
                ex.printStackTrace()
                respondErrorEmbed(response, "Something went wrong, please double check permissions etc")
            }
        }
    }

    override suspend fun handleArtists(
        artists: List<Artist>,
        response: DeferredMessageInteractionResponseBehavior,
        unresolved: List<String>,
        interaction: ChatInputCommandInteraction
    ) {

    }

    override fun isCustomHandle(): Boolean {
        return true
    }

    override suspend fun handleInput(str: String, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {
        if(str.matches(albumRegex)) {
            val channel = getChannelInput(interaction)!!
            val radarId = db.getRadarId(channel)
            val id = str.replace(albumRegex, "$1")
            val album = spotify.getAlbumInstance(id)

            if(album != null) {
                postRadarAlbum(album, channel.fetchChannel() as MessageChannelBehavior, radarId)
                respondSuccessEmbed(response, desc = "Posted ${album.name} to ${channel.mention}")
            } else
                respondErrorEmbed(response, "Failed to find album with id $id")
        } else
            respondErrorEmbed(response, "No artist or album id found")
    }
}