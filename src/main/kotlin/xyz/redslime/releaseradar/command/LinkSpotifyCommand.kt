package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.success

/**
 * @author redslime
 * @version 2023-09-22
 */
class LinkSpotifyCommand: Command("linkspotify", "Link your Spotify account", dms = true) {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val userId = interaction.user.id.asLong()

        if(!db.isUserEnlisted(userId)) {
            interaction.deferEphemeralResponse().respond {
                embed {
                    error()
                    title = ":x: You are not allowed to do this!"
                    description = "Spotify linking is currently only available to selected people.\n" +
                            "Contact <@115834525329653760> for help."
                }
            }
            return
        }

        var re: EphemeralMessageInteractionResponse? = null
        re = interaction.deferEphemeralResponse().respond {
            embed {
                success()
                description = "Click the button below to link your Spotify account.\n" +
                        "This authorizes the bot to access and create public playlists on your behalf."
            }
            actionRow {
                addSpotifyLinkButton(this, interaction.user, true) {
                    re?.delete()

                    if(it) {
                        db.setUserPlaylistData(userId, null)
                        re?.createEphemeralFollowup {
                            embed {
                                success()
                                title = ":white_check_mark: Spotify linked successfully"
                            }
                        }
                    } else {
                        re?.createEphemeralFollowup {
                            embed {
                                error()
                                title = ":x: Failed to link Spotify"
                                description = "This is likely due to an internal error. Please retry later."
                            }
                        }
                    }
                }
            }
        }
    }
}