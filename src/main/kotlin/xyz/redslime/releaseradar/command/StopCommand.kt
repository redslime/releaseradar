package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlin.system.exitProcess

/**
 * @author redslime
 * @version 2023-05-25
 */
class StopCommand: AdminCommand("stop", "Restart the bot") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        interaction.kord.logout()
        exitProcess(0)
    }
}