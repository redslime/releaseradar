package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.DiscordClient.Companion.scanTask
import xyz.redslime.releaseradar.success

/**
 * @author redslime
 * @version 2023-05-21
 */
class ScanCommand: AdminCommand("scan", "Looks for new releases globally") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        interaction.deferEphemeralResponse().respond {
            embed {
                success()
                title = "Starting scanning for new releases..."
            }
        }
        scanTask.runActual(interaction.kord, true)
    }
}