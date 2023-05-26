package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        val album = latest?.toAlbum()

        response.respond {
            embed { buildAlbum(album, this) }
        }
    }

    override suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction) {
        val first = spotify.getLatestRelease(artists.first().id)
        val firstAlbum = first?.toAlbum()

        val re = response.respond {
            embed { buildAlbum(firstAlbum, this) }
        }

        artists.stream().skip(1).forEach {
            runBlocking {
                launch {
                    re.createPublicFollowup {
                        embed { buildAlbum(spotify.getLatestRelease(it.id)?.toAlbum(), this) }
                    }
                }
            }
        }

        if(unresolved.isNotEmpty()) {
            re.createPublicFollowup {
                embed {
                    error()
                    title = ":x: Failed to resolve artists:"
                    description += unresolved.joinToString("\n")
                }
            }
        }
    }
}