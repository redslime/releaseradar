package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.playlist.PlaylistDuration
import xyz.redslime.releaseradar.playlist.PlaylistHandler
import xyz.redslime.releaseradar.success

/**
 * @author redslime
 * @version 2023-06-16
 */
class ReminderPlaylistCommand: Command("reminderplaylist", "Setup a playlist to add tracks to", dms = true) {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        var re: EphemeralMessageInteractionResponse? = null
        re = interaction.deferEphemeralResponse().respond {
            embed {
                title = "Reminder Playlist Setup (1/4)"
                description = "Let's walk through a couple options you can set for your reminder playlist:\n\n" +
                        "**How often should a new playlist be created?**"
            }
            actionRow {
                stringSelect("playlist-duration") {
                    PlaylistDuration.values().forEach { duration ->
                        addSelectOption(this, duration.getDescription(), duration.name) {
                            selectAppend(re, interaction.user, duration)
                        }
                    }
                }
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    re?.delete()
                }
            }
        }
    }

    private suspend fun selectAppend(re: EphemeralMessageInteractionResponse?, user: User, duration: PlaylistDuration) {
        re?.delete()

        if(duration != PlaylistDuration.NEVER) {
            selectPublic(re, null, user, duration,true)
            return
        }

        var newRe: EphemeralFollowupMessage? = null
        newRe = re?.createEphemeralFollowup {
            embed {
                title = "Reminder Playlist Setup (2/4)"
                description = "**Should the playlist be cleared every day before new tracks are added?**"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    newRe?.delete()
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Yes - Clear") {
                    selectPublic(re, newRe, user, duration, false)
                }
                addInteractionButton(this, ButtonStyle.Secondary, "No - Append") {
                    selectPublic(re, newRe, user, duration, true)
                }
            }
        }
    }

    private suspend fun selectPublic(re: EphemeralMessageInteractionResponse?, fo: EphemeralFollowupMessage?, user: User, duration: PlaylistDuration, append: Boolean) {
        fo?.delete()
        var newRe: EphemeralFollowupMessage? = null
        newRe = re?.createEphemeralFollowup {
            embed {
                title = "Reminder Playlist Setup (3/4)"
                description = "**Should the playlist be public?**"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    newRe?.delete()
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Yes - Public") {
                    linkSpotify(re, newRe, user, duration, append, true)
                }
                addInteractionButton(this, ButtonStyle.Secondary, "No - Private") {
                    linkSpotify(re, newRe, user, duration, append, false)
                }
            }
        }
    }

    private suspend fun linkSpotify(re: EphemeralMessageInteractionResponse?, fo: EphemeralFollowupMessage?, user: User, duration: PlaylistDuration, append: Boolean, public: Boolean) {
        val visibility = if(public) "public" else "private"
        fo?.delete()
        var newRe: EphemeralFollowupMessage? = null
        newRe = re?.createEphemeralFollowup {
            embed {
                title = "Reminder Playlist Setup (4/4)"
                description = "Finally, please link your Spotify account.\n" +
                        "This authorizes the bot to create $visibility playlists on your behalf."
            }
            actionRow {
                addSpotifyLinkButton(this, user, public) {
                    if(it) {
                        val handler = PlaylistHandler(duration, public, append)
                        db.setUserPlaylistHandler(user.id.asLong(), handler)

                        newRe?.delete()
                        re.createEphemeralFollowup {
                            embed {
                                success()
                                title = ":white_check_mark: Reminder Playlist Setup Complete!"
                                description = "Congratulations! Your reminders will now be sent as a playlist automatically"
                                footer {
                                    text = "To change these settings, just do /reminderplaylist again"
                                }
                            }
                        }
                    } else {
                        re.createEphemeralFollowup {
                            embed {
                                error()
                                title = ":x: Failed to link Spotify"
                                description = "Please try again:"
                            }
                        }
                        linkSpotify(re, null, user, duration, append, public)
                    }
                }
            }
        }
    }
}