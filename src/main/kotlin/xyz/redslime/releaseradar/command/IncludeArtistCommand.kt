package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.ChunkedString
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2024-03-05
 */
class IncludeArtistCommand: ArtistCommand("include", "Include an artist in a radar which was previously excluded") {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel the artist should be included in")
    }

    override suspend fun handleArtist(
        artist: Artist,
        response: DeferredMessageInteractionResponseBehavior,
        interaction: ChatInputCommandInteraction,
    ) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val rid = db.getRadarId(channel)

        response.respond {
            if(db.includeArtistInRadar(artist, rid)) {
                embed {
                    success()
                    title = "${artist.name} is no longer excluded from radar in ${channel.mention}"
                }
            } else {
                embed {
                    error()
                    title = "There is no artist named ${artist.name} excluded from the radar in ${channel.mention}"
                    description = "Check who is excluded from the radar using the ``/list excluded`` command!"
                }
            }
        }
    }

    override suspend fun handleArtists(
        artists: List<Artist>,
        response: DeferredMessageInteractionResponseBehavior,
        unresolved: List<String>,
        interaction: ChatInputCommandInteraction,
    ) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val rid = db.getRadarId(channel)
        val description = ChunkedString()
        val simpleArtists = artists.map { it.toSimpleArtist() }
        val skipped = db.includeArtistsInRadar(simpleArtists, rid)
        val removed = artists.size - skipped.size

        simpleArtists.forEach {  artist ->
            if(!skipped.contains(artist)) {
                description.add(":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))")
            } else {
                description.add(":x: **${artist.name}** not removed, not excluded?")
            }
        }

        unresolved.forEach {
            description.add(":x: **$it** not removed, failed to find artist")
        }

        description.chunked({ first ->
            response.respond {
                embed {
                    colorize(removed, artists.size)
                    this.description = "${pluralPrefixed("artist", removed)} no longer excluded from radar in ${channel.mention}:\n\n" + first
                }
            }
        }, { _, first, chunk ->
            first.createPublicFollowup {
                embed {
                    this.description = chunk
                }
            }
        })
    }
}