package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.DiscordClient.Companion.scanTask

/**
 * @author redslime
 * @version 2023-05-21
 */
class ScanCommand: AdminCommand("scan", "Looks for new releases globally") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        respondSuccessEmbed(interaction.deferEphemeralResponse(), "Starting scanning for new releases...")
        scanTask.runActual(interaction.kord, true)
    }
}