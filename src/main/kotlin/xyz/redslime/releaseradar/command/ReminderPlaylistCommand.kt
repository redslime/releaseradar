package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.playlist.PlaylistDuration
import xyz.redslime.releaseradar.playlist.PlaylistHandler
import xyz.redslime.releaseradar.util.albumRegex
import xyz.redslime.releaseradar.util.getStartOfToday
import xyz.redslime.releaseradar.util.trackRegex

/**
 * @author redslime
 * @version 2023-06-16
 */
class ReminderPlaylistCommand: Command("reminderplaylist", "Setup a playlist to add tracks to", dms = true) {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val userId = interaction.user.id.asLong()
        val handler = db.getUserPlaylistHandler(userId)
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

                if(handler != null)
                    addInteractionButton(this, ButtonStyle.Secondary, "Revert to individual track links") {
                        val response = it.deferEphemeralResponse()
                        db.setUserPlaylistHandler(userId, null)

                        response.respond {
                            embed {
                                success()
                                title = "Reminders will be posted individually from now on"
                            }
                        }
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
                        newRe = re.createEphemeralFollowup {
                            embed {
                                success()
                                title = ":white_check_mark: Reminder Playlist Setup Complete!"
                                description = "Congratulations! Your reminders will now be sent as a playlist automatically"
                                footer {
                                    text = "To change these settings, just do /reminderplaylist again"
                                }
                            }
                            actionRow {
                                addInteractionButton(this, ButtonStyle.Success, "Send today's tracks as playlist", ReactionEmoji.Unicode("\uD83D\uDD03")) { interaction ->
                                    val response = interaction.deferEphemeralResponse()
                                    val today = getStartOfToday()
                                    val channel = interaction.user.getDmChannelOrNull()

                                    if(channel?.getLastMessage() != null) {
                                        val messages = channel.getMessagesBefore(channel.lastMessageId!!)
                                            .takeWhile { it.timestamp > today }
                                            .filter { it.author == interaction.kord.getSelf() }
                                            .toList()

                                        if(messages.isNotEmpty()) {
                                            val albums = messages.flatMap { it.content.split("\n") }
                                                .filter { it.matches(trackRegex) || it.matches(albumRegex) }
                                                .mapNotNull {
                                                    if(it.matches(trackRegex)) {
                                                        val trackId = it.replace(trackRegex, "$1")
                                                        spotify.getAlbumFromTrack(trackId)?.toAlbum()
                                                    } else {
                                                        val albumId = it.replace(albumRegex, "$1")
                                                        spotify.getAlbumInstance(albumId)
                                                    }
                                                }

                                            newRe?.delete()
                                            handler.postAlbums(interaction.user, albums)
                                            return@addInteractionButton
                                        }
                                    }

                                    response.respond {
                                        content = "Looks like there are no tracks for today! You will automatically receive tracks as a playlist in the future :)"
                                    }
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