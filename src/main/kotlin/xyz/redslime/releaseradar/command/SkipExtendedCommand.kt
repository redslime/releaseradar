package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.success

/**
 * @author redslime
 * @version 2024-03-28
 */
class SkipExtendedCommand: Command("skipextended", "Toggle whether extended tracks should be skipped in reminders", dms = true) {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val userId = interaction.user.id.asLong()
        val state = !db.isUserSkippingExtended(userId)
        val re = interaction.deferEphemeralResponse()

        db.setUserSkippingExtended(userId, state)
        re.respond {
            embed {
                success()
                title = if(state) ":white_check_mark: Now skipping extended tracks" else ":negative_squared_cross_mark: No longer skipping extended tracks"
                description = if(state)
                    "Extended tracks will now be skipped in your reminders."
                else
                    "Extended tracks will no longer be skipped in your reminders."
                footer {
                    text = "To undo, run the command again"
                }
            }
        }
    }
}