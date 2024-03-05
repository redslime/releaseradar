package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.count
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.prettyPrint
import xyz.redslime.releaseradar.startedAt
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

        interaction.respondEphemeral {
            embed {
                title = "Global Bot Statistics"
                thumbnail {
                    url = "https://redslime.xyz/releaseradar.png"
                }
                description = "Tracking **${cache.getTotalArtistsOnRadar()} unique artists**\n" +
                        "on **${cache.getTotalRadarCount()} total radars**\n" +
                        "for **${interaction.kord.guilds.count()} servers.**\n" +
                        "Uptime: ${uptime.prettyPrint()}"
            }
        }
    }
}