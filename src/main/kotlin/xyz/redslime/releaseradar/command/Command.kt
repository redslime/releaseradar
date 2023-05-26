package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.allowedChannels
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.util.Timezone
import kotlin.coroutines.CoroutineContext

/**
 * @author redslime
 * @version 2023-05-19
 */
abstract class Command(val name: String, val description: String, val perm: PermissionLevel = PermissionLevel.EVERYONE, val dms: Boolean = false) {

    val buttons: HashMap<String, suspend CoroutineContext.(ButtonInteraction) -> Unit> = HashMap()
    val timezoneSelectors: HashMap<String, suspend CoroutineContext.(Timezone) -> Unit> = HashMap()

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

    protected fun addInteractionButton(builder: ActionRowBuilder, style: ButtonStyle, label: String, block: suspend CoroutineContext.(ButtonInteraction) -> Unit) {
        val key = "$name-${label.lowercase()}-${System.currentTimeMillis().hashCode()}"
        builder.interactionButton(style, key) {
            this.label = label
        }
        buttons[key] = block
    }

    protected fun addTimezoneSelector(builder: ActionRowBuilder, block: suspend CoroutineContext.(Timezone) -> Unit) {
        val key = "$name-timezone-${System.currentTimeMillis().hashCode()}"
        builder.stringSelect(key) {
            Timezone.values().forEach {
                option(it.friendly, it.name)
            }
        }
        timezoneSelectors[key] = block
    }
}