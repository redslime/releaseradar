package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.SimpleArtist
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.followup.PublicFollowupMessage
import dev.kord.core.entity.interaction.response.MessageInteractionResponse
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.exception.InvalidUrlException
import xyz.redslime.releaseradar.util.ChunkedString
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

        val previewSkipped = artists.filter { cache.isOnRadar(it, radarId) }.toList()
        val previewAddCount = artists.size - previewSkipped.size
        var re: MessageInteractionResponse? = null
        re = response.respond {
            embed {
                warning()
                title = "Importing would add ${pluralPrefixed("artist", previewAddCount)} to ${channel.mention}"

                if(previewSkipped.isNotEmpty())
                    description = "${pluralPrefixed("artist", previewSkipped.size)} are already on radar"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Success, "Confirm import") {
                    import(artists, channel, radarId, it)
                }
                addInteractionButton(this, ButtonStyle.Danger, "Cancel import") {
                    it.message.delete()
                }
                addInteractionButton(this, ButtonStyle.Secondary, "See detailed list") {
                    val description = ChunkedString()

                    artists.forEach { artist ->
                        if(!previewSkipped.contains(artist)) {
                            description.add(":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))")
                        } else {
                            description.add(":x: **${artist.name}** not added, already on list?")
                        }
                    }

                    val messageParts = mutableListOf<PublicFollowupMessage>()
                    val rep = description.chunked({ first ->
                        respondEmbed(it.deferPublicResponse(), desc = first)
                    }, { _, first, chunk ->
                        messageParts.add(first.createPublicFollowup {
                            embed {
                                this.description = chunk
                            }
                        })
                    })

                    rep.createPublicFollowup {
                        embed {
                            warning()
                            title = "Importing would add ${pluralPrefixed("artist", previewAddCount)} to ${channel.mention}"

                            if(previewSkipped.isNotEmpty())
                                this.description = "${pluralPrefixed("artist", previewSkipped.size)} are already on radar"
                        }
                        actionRow {
                            addInteractionButton(this, ButtonStyle.Success, "Confirm import") {
                                import(artists, channel, radarId, it)
                            }
                            addInteractionButton(this, ButtonStyle.Danger, "Cancel import") {
                                it.message.delete()
                                messageParts.forEach { m -> m.delete() }
                                re!!.delete()
                                rep.delete()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun import(artists: List<SimpleArtist>, channel: ResolvedChannel, radarId: Int, re: ActionInteractionBehavior) {
        val skipped = db.addArtistsToRadar(artists, radarId)
        val actualList = ArrayList(artists)
        actualList.removeAll(skipped.toSet())
        val actualAdded = artists.size - skipped.size

        re.respondPublic {
            successEmbed( "Added ${pluralPrefixed("artist", actualAdded)} from playlist to ${channel.mention}") {
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