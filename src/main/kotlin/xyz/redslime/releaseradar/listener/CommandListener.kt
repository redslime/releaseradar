package xyz.redslime.releaseradar.listener

import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.error

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
                    .forEach { runBlocking { launch {
                        if((dms && it.dms) || it.perm.hasPermission(client, interaction)) {
                            it.handleInteraction(interaction)
                        } else {
                            interaction.deferEphemeralResponse().respond {
                                embed {
                                    error()
                                    description = "This command is either not available here or you don't have enough permissions."
                                }
                            }
                        }
                    }}}
            }
        }
    }
}