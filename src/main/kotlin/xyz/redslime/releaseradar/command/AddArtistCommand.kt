package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.ChunkedString
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-19
 */
class AddArtistCommand : ArtistCommand("add", "Add an artist to the release radar for the specified channel", PermissionLevel.CONFIG_CHANNEL) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel new releases of the artist should be posted to")
    }

    override suspend fun handleArtist(artist: Artist, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val radarId = db.getRadarId(channel)
        val success = db.addArtistToRadar(artist, channel)

        response.respond {
            if(success) {
                embed {
                    success()
                    description = "Added to release radar in ${channel.mention}"
                    author {
                        name = artist.name
                        icon = artist.images.firstOrNull()?.url
                        url = artist.externalUrls.spotify
                    }
                }
                actionRow {
                    addInteractionButton(this, ButtonStyle.Secondary, "Undo") {
                        it.message.delete()
                        db.removeArtistFromRadar(artist, channel)
                    }
                    addInteractionButton(this, ButtonStyle.Secondary, "Print latest release") {
                        val re = it.deferEphemeralResponse()
                        spotify.getLatestRelease(artist.id)?.let {
                            it.toFullAlbum()?.let {
                                postAlbum(it, channel.fetchChannel() as MessageChannelBehavior, radarId)
                            }
                        }
                        re.delete()
                    }
                }
            } else {
                embed {
                    error()
                    description = "Failed to add to release radar, perhaps already on the list?"
                    author {
                        name = artist.name
                        icon = artist.images.firstOrNull()?.url
                        url = artist.externalUrls.spotify
                    }
                }
            }
        }
    }

    override suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val radarId = db.getRadarId(channel)
        val description = ChunkedString()
        val simpleArtists = artists.map { it.toSimpleArtist() }
        val skipped = db.addArtistsToRadar(simpleArtists, radarId)
        val actualList = ArrayList(simpleArtists)
        actualList.removeAll(skipped.toSet())
        val added = artists.size - skipped.size

        simpleArtists.forEach { artist ->
            if(!skipped.contains(artist)) {
                description.add(":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))")
            } else {
                description.add(":x: **${artist.name}** not added, already on list?")
            }
        }

        unresolved.forEach {
            description.add(":x: **$it** not added, failed to find artist")
        }

        val finalDesc = ChunkedString()
        finalDesc.add("Added ${pluralPrefixed("artist", added)} to ${channel.mention}:\n")
        finalDesc.addAll(description)
        val chunks = finalDesc.getChunks(4000, "\n") // limit of 4096 chars in a single embed

        val re = response.respond {
            embed {
                colorize(added, artists.size)
                this.description = chunks[0]
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Secondary, "Undo") {
                    it.message.delete()
                    db.removeArtistsFromRadar(actualList, channel)
                }
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