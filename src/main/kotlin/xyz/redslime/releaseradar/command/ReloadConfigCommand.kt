package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.reloadConfig
import xyz.redslime.releaseradar.spotify

/**
 * @author redslime
 * @version 2024-03-07
 */
class ReloadConfigCommand: AdminCommand("reloadconfig", "Reload config from disk") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        try {
            val config = reloadConfig()
            spotify.login(config.spotifyFallbacks)
            respondSuccessEmbed(interaction.deferPublicResponse(), "Reloaded the config file")
        } catch (ex: Exception) {
            respondErrorEmbed(interaction.deferPublicResponse(), ex.javaClass.name, ex.message)
        }
    }
}