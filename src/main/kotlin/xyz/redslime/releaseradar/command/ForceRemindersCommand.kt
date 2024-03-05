package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.DiscordClient
import xyz.redslime.releaseradar.success

/**
 * @author redslime
 * @version 2023-09-29
 */
class ForceRemindersCommand: AdminCommand("forcereminders", "Executes the post later task") {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addTimezoneInput(builder, "Timezone to post reminders of")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val timezone = getTimezoneInput(interaction)!!

        interaction.deferEphemeralResponse().respond {
            embed {
                success()
                title = "Launching PostLater task"
            }
        }

        DiscordClient.postLaterTask.runActual(interaction.kord, timezone)
    }
}