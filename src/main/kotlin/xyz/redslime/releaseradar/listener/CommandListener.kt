package xyz.redslime.releaseradar.listener

import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.errorEmbed
import xyz.redslime.releaseradar.util.coroutine

/**
 * @author redslime
 * @version 2023-05-19
 */
class CommandListener {

    fun register(client: Kord) {
        client.on<ChatInputCommandInteractionCreateEvent> {
            val dms = interaction.channel.fetchChannel().type == ChannelType.DM
            val key = interaction.data.data.name.value

            if(key != null) {
                commands.stream()
                    .filter { it.name == key }
                    .forEach {
                        coroutine {
                            if ((dms && it.dms) || it.perm.hasPermission(client, interaction)) {
                                it.handleInteraction(interaction)
                            } else {
                                interaction.deferEphemeralResponse().respond {
                                    errorEmbed {
                                        description = "This command is either not available here or you don't have enough permissions."
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}