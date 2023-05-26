package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.SimpleArtist
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.exception.InvalidUrlException
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-20
 */
class ImportCommand: Command("import", "Import all artists from a playlist into a release radar", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        builder.string("playlist", "The spotify playlist url to import artists from") {
            required = true
        }
        addChannelInput(builder, "The channel the artists should be added to")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferPublicResponse()
        val cmd = interaction.command
        val playlist = cmd.strings["playlist"]!!
        val channel = cmd.channels["channel"]!!
        val radarId = db.getRadarId(channel)
        val artists: List<SimpleArtist>

        try {
            artists = spotify.getArtistsFromPlaylist(playlist)
        } catch (ex: InvalidUrlException) {
            respondErrorEmbed(response, "Invalid playlist url given!")
            return
        } catch (ex: SpotifyException.BadRequestException) {
            respondErrorEmbed(response, "Failed to retrieve playlist! Is it public?")
            return
        }

        val skipped = db.addArtistsToRadar(artists, radarId)
        val actualList = ArrayList(artists)
        actualList.removeAll(skipped.toSet())
        val actualAdded = artists.size - skipped.size

        response.respond {
            embed {
                success()
                title = "Added ${pluralPrefixed("artist", actualAdded)} from playlist to ${channel.mention}"

                if(skipped.isNotEmpty())
                    description = "Skipped ${pluralPrefixed("artist", skipped.size)}, already on radar?"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Secondary, "Undo") {
                    it.message.delete()
                    db.removeArtistsFromRadar(actualList, channel)
                }
            }
        }
    }
}