package xyz.redslime.releaseradar.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-20
 */
class RadarsCommand : Command("radars", "Lists all radars in the server", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        // nothing here hehe
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        interaction.getChannel().data.guildId.value?.let { guildId ->
            val response = interaction.deferPublicResponse()
            var amt = 0
            var desc = ""

            cache.getRadars(guildId.asLong()).forEach {
                interaction.kord.getChannel(Snowflake(it.channelId!!))?.let { channel ->
                    amt = cache.getArtistNamesInRadarChannel(channel).size

                    if(amt > 0)
                        desc += "${channel.mention}: ${pluralPrefixed("artist", amt)}\n"
                }
            }

            response.respond {
                embed {
                    success()
                    title = "All radars:"
                    description = desc
                    footer {
                        text = if(amt > 0)
                            "Use /list to view artists on a specific radar"
                        else
                            "To create a radar, simply type /add or /import"
                    }
                }
            }
        }
    }
}