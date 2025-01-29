package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlinx.coroutines.flow.count
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.prettyPrint
import xyz.redslime.releaseradar.startedAt
import xyz.redslime.releaseradar.thumbnail
import java.time.Duration

/**
 * @author redslime
 * @version 2023-05-22
 */
class GlobalStatsCommand: Command("globalstats", "Displays global bot statistics") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val uptime = Duration.ofMillis(System.currentTimeMillis() - startedAt)
        val guildCount = interaction.kord.guilds.count()

        respondEmbed(interaction.deferEphemeralResponse(), "Global Bot Statistics") {
            thumbnail("https://redslime.xyz/releaseradar.png")
            description = "Tracking **${cache.getTotalArtistsOnRadar()} unique artists**\n" +
                    "on **${cache.getTotalRadarCount()} total radars**\n" +
                    "for **$guildCount servers.**\n" +
                    "Uptime: ${uptime.prettyPrint()}"
        }
    }
}