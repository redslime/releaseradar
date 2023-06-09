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
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-19
 */
class RemoveArtistCommand : ArtistCommand("remove", "Remove an artist from a release radar for the specified channel", PermissionLevel.CONFIG_CHANNEL, artistLimit = 50) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel the artist should be removed from")
    }

    override suspend fun handleArtist(artist: Artist, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!

        response.respond {
            if(db.removeArtistFromRadar(artist, channel)) {
                embed {
                    success()
                    title = "Removed ${artist.name} from radar in ${channel.mention}"
                }
            } else {
                embed {
                    error()
                    title = "There is no artist named ${artist.name} on the radar in ${channel.mention}"
                    description = "Check who is on the radar using the /list command!"
                }
            }
        }
    }

    override suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        var description = ""
        val simpleArtists = artists.map { it.toSimpleArtist() }
        val skipped = db.removeArtistsFromRadar(simpleArtists, channel)
        val removed = artists.size - skipped.size

        simpleArtists.forEach {  artist ->
            if(!skipped.contains(artist)) {
                description += ":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))\n"
            } else {
                description += ":x: **${artist.name}** not removed, not on radar?\n"
            }
        }

        unresolved.forEach {
            description += ":x: **$it** not removed, failed to find artist\n"
        }

        // limit of 4096 chars in a single embed
        val chunks = description.chunked(4000)

        val re = response.respond {
            embed {
                colorize(removed, artists.size)
                this.description = "Removed ${pluralPrefixed("artist", removed)} from ${channel.mention}:\n\n" + chunks[0]
            }
        }

        chunks.stream().skip(1).forEach { desc ->
            runBlocking {  // todo this ugly
                launch {
                    re.createPublicFollowup {
                        embed {
                            this.description = desc
                        }
                    }
                }
            }
        }
    }
}