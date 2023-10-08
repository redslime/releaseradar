package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ActionInteraction
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
        sendPrompt(interaction)
    }

    suspend fun sendPrompt(interaction: ActionInteraction) {
        val userId = interaction.user.id.asLong()
        val handler = db.getUserPlaylistHandler(userId)
        var re: EphemeralMessageInteractionResponse? = null

        re = interaction.deferEphemeralResponse().respond {
            embed {
                title = "Reminder Playlist Settings (1/3)"
                description = "**How often should the playlist be reset?**"
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

                addInteractionButton(this, ButtonStyle.Secondary, "Disable playlist") {
                    re?.delete()
                    val response = it.deferEphemeralResponse()
                    handler.disabled = true
                    db.setUserPlaylistHandler(userId, handler)

                    response.respond {
                        embed {
                            success()
                            title = "Disabled reminder playlist"
                            description = "Reminders will be posted individually from now on"
                            footer {
                                text = "To change these settings, just do /reminderplaylist again"
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun selectAppend(re: EphemeralMessageInteractionResponse?, user: User, duration: PlaylistDuration) {
        if(duration != PlaylistDuration.NEVER) {
            selectAlways(re, null, user, duration,true, false)
            return
        }

        var newRe: EphemeralFollowupMessage? = null
        newRe = re?.createEphemeralFollowup {
            embed {
                title = "Reminder Playlist Settings (2/3)"
                description = "**Should the playlist be cleared every day before new tracks are added?**"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    newRe?.delete()
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Yes - Clear") {
                    selectAlways(re, newRe, user, duration, false, false)
                }
                addInteractionButton(this, ButtonStyle.Secondary, "No - Append") {
                    selectAlways(re, newRe, user, duration, true, false)
                }
            }
        }
        re?.delete()
    }

    private suspend fun selectAlways(re: EphemeralMessageInteractionResponse?, fo: EphemeralFollowupMessage?, user: User, duration: PlaylistDuration, append: Boolean, disabled: Boolean) {
        fo?.delete()

        var newRe: EphemeralFollowupMessage? = null
        newRe = re?.createEphemeralFollowup {
            embed {
                title = "Reminder Playlist Settings (3/3)"
                description = "**Should new tracks always be added to the playlist, regardless of the amount of new releases?**"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                    newRe?.delete()
                }
                addInteractionButton(this, ButtonStyle.Success, "Yes") {
                    finish(re, newRe, user, duration, append, true, disabled, true)
                }
                addInteractionButton(this, ButtonStyle.Secondary, "No - Only for 5 or more releases") {
                    finish(re, newRe, user, duration, append, false, disabled, false)
                }
            }
        }

        re?.delete()
    }

//    private suspend fun selectPublic() {
        // There's currently a bug in the Spotify API that essentially makes private playlists impossible to work with:
        // https://community.spotify.com/t5/Spotify-for-Developers/Api-to-create-a-private-playlist-doesn-t-work/td-p/5407807

//        fo?.delete()
//        var newRe: EphemeralFollowupMessage? = null
//        newRe = re?.createEphemeralFollowup {
//            embed {
//                title = "Reminder Playlist Setup (3/4)"
//                description = "**Should the playlist be public?**"
//            }
//            actionRow {
//                addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
//                    newRe?.delete()
//                }
//                addInteractionButton(this, ButtonStyle.Secondary, "Yes - Public") {
//                    linkSpotify(re, newRe, user, duration, append, true)
//                }
//                addInteractionButton(this, ButtonStyle.Secondary, "No - Private") {
//                    linkSpotify(re, newRe, user, duration, append, false)
//                }
//            }
//        }
//    }

    private suspend fun finish(re: EphemeralMessageInteractionResponse?, fo: EphemeralFollowupMessage?, user: User, duration: PlaylistDuration, append: Boolean, public: Boolean, disabled: Boolean, always: Boolean) {
        val handler = PlaylistHandler(duration, public, append, disabled, always)
        db.setUserPlaylistHandler(user.id.asLong(), handler)
        fo?.delete()
        re?.createEphemeralFollowup {
            embed {
                success()
                title = ":white_check_mark: Reminder Playlist Setup Complete!"
                description = "Congratulations! Your playlist settings were saved successfully."
                footer {
                    text = "To change these settings, just do /reminderplaylist again"
                }
            }
        }
    }
}