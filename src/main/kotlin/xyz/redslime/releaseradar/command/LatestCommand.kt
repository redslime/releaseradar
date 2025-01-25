package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*

/**
 * @author redslime
 * @version 2023-05-19
 */
class LatestCommand : ArtistCommand("latest", "Gets the latest release of the specified artist", ephemeral = true) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        // nothing here hehe
    }

    override suspend fun handleArtist(artist: Artist, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {
        val latest = spotify.getLatestRelease(artist.id)

        latest?.toAlbum()?.let { album ->
            val eb = EmbedBuilder()
            buildAlbumEmbed(album, eb)

            response.respond {
                addEmbed(eb)
            }
        }
    }

    override suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction) {
        val re = response.respond {
            artists.take(10).forEach { artist ->
                spotify.getLatestRelease(artist.id)?.toAlbum()?.let { album ->
                    addEmbed(buildAlbumEmbed(album))
                }
            }
        }

        artists.stream().skip(10).toList().chunked(10).forEach { artistList ->
            re.createEphemeralFollowup {
                artistList.forEach { artist ->
                    spotify.getLatestRelease(artist.id)?.toAlbum()?.let { album ->
                        addEmbed(buildAlbumEmbed(album))
                    }
                }
            }
        }

        if(unresolved.isNotEmpty()) {
            re.createEphemeralFollowup {
                embed {
                    error()
                    title = ":x: Failed to resolve artists:"
                    description += unresolved.joinToString("\n")
                }
            }
        }
    }
}