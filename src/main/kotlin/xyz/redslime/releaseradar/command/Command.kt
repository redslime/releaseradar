package xyz.redslime.releaseradar.command

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.allowedChannels
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.util.Interactable
import xyz.redslime.releaseradar.util.Timezone

/**
 * @author redslime
 * @version 2023-05-19
 */
abstract class Command(val name: String, val description: String, val perm: PermissionLevel = PermissionLevel.EVERYONE, val dms: Boolean = false): Interactable {

    abstract fun addParameters(builder: ChatInputCreateBuilder)

    abstract suspend fun handleInteraction(interaction: ChatInputCommandInteraction)

    open suspend fun register(client: Kord) {
        LogManager.getLogger(javaClass).info("Registering command ${javaClass.simpleName}")
        commands.add(this)
        client.createGlobalChatInputCommand(name, description) {
            addParameters(this)
        }
    }

    protected fun addChannelInput(builder: ChatInputCreateBuilder, desc: String, req: Boolean = true) {
        builder.channel("channel", desc) {
            required = req
            channelTypes = allowedChannels
        }
    }

    protected fun getChannelInput(interaction: ChatInputCommandInteraction): ResolvedChannel? {
        return interaction.command.channels["channel"]
    }

    protected fun addTimezoneInput(builder: ChatInputCreateBuilder, desc: String, req: Boolean = true) {
        builder.string("timezone", desc) {
            required = req
            Timezone.values().forEach {
                choice(it.friendly, it.name)
            }
        }
    }

    protected fun getTimezoneInput(interaction: ChatInputCommandInteraction): Timezone? {
        return interaction.command.strings["timezone"]?.let { Timezone.valueOf(it) }
    }

    protected suspend fun respondErrorEmbed(response: DeferredMessageInteractionResponseBehavior, title: String) {
        response.respond {
            embed {
                error()
                this.title = title
            }
        }
    }

    protected suspend fun checkRadarChannel(radarId: Int?, response: DeferredMessageInteractionResponseBehavior, channel: ResolvedChannel): Boolean {
        if(radarId == null) {
            respondErrorEmbed(response, "${channel.mention} is not a radar channel!")
            return false
        }

        return true
    }
}