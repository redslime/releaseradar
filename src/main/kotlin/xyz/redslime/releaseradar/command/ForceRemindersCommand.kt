package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.DiscordClient

/**
 * @author redslime
 * @version 2023-09-29
 */
class ForceRemindersCommand: AdminCommand("forcereminders", "Executes the post later task") {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addTimezoneInput(builder, "Timezone to post reminders of")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        respondSuccessEmbed(interaction.deferEphemeralResponse(), "Launching PostLater task")
        DiscordClient.postLaterTask.runActual(interaction.kord, getTimezoneInput(interaction)!!)
    }
}