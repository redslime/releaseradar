package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.reloadConfig
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.success

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
            spotify.login(config.spotifyClientId, config.spotifySecret)
            interaction.deferPublicResponse().respond {
                embed {
                    success()
                    title = "Reloaded the config file"
                }
            }
        } catch (ex: Exception) {
            interaction.deferPublicResponse().respond {
                embed {
                    error()
                    title = ex.javaClass.name
                    description = ex.message
                }
            }
        }
    }
}