package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.ChunkedString
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2024-03-05
 */
class ExcludeArtistCommand: ArtistCommand("exclude", "Exclude an artist from a radar", PermissionLevel.CONFIG_CHANNEL) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar the artist should be excluded from")
    }

    override suspend fun handleArtist(
        artist: Artist,
        response: DeferredMessageInteractionResponseBehavior,
        interaction: ChatInputCommandInteraction,
    ) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val radarId = db.getRadarId(channel)
        val success = db.excludeArtistFromRadar(artist, radarId)
        val removed = db.removeArtistFromRadar(artist, radarId)

        response.respond {
            if(success) {
                embed {
                    success()
                    description = "Excluded from release radar in ${channel.mention}\n" +
                            "Note: Any releases with ${artist.name} involved will be excluded, even if another artist on the same release is on the radar."

                    if(removed)
                        description += "\n\n:warning: This artist was previously on the radar, is removed now."

                    author {
                        name = artist.name
                        icon = artist.images?.firstOrNull()?.url
                        url = artist.externalUrls.spotify
                    }
                }
                actionRow {
                    addInteractionButton(this, ButtonStyle.Secondary, "Undo") {
                        it.message.delete()
                        db.includeArtistInRadar(artist, radarId)
                    }
                }
            } else {
                embed {
                    error()
                    description = "Failed to add to exclude list, perhaps already on the list?"
                    author {
                        name = artist.name
                        icon = artist.images?.firstOrNull()?.url
                        url = artist.externalUrls.spotify
                    }
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
        val radarId = db.getRadarId(channel)
        val description = ChunkedString()
        val simpleArtists = artists.map { it.toSimpleArtist() }
        val skipped = db.excludeArtistsFromRadar(simpleArtists, radarId)
        val removedSkipped = db.removeArtistsFromRadar(simpleArtists, channel)
        val actualList = ArrayList(simpleArtists)
        actualList.removeAll(skipped.toSet())
        val added = artists.size - skipped.size

        simpleArtists.forEach { artist ->
            if(!skipped.contains(artist)) {
                var line = ":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))"

                if(!removedSkipped.contains(artist))
                    line += " :warning: _Removed from radar_"

                description.add(line)
            } else {
                description.add(":x: **${artist.name}** not excluded, already on list?")
            }
        }

        unresolved.forEach {
            description.add(":x: **$it** not added, failed to find artist")
        }

        val finalDesc = ChunkedString()
        finalDesc.add("Excluded ${pluralPrefixed("artist", added)} from ${channel.mention}:\n")
        finalDesc.addAll(description)
        finalDesc.chunked({ first ->
            response.respond {
                embed {
                    colorize(added, artists.size)
                    this.description = first
                }
                actionRow {
                    addInteractionButton(this, ButtonStyle.Secondary, "Undo") {
                        it.message.delete()
                        db.includeArtistsInRadar(actualList, radarId)
                    }
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