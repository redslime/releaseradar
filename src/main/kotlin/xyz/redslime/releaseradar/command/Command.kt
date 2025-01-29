package xyz.redslime.releaseradar.command

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.response.MessageInteractionResponse
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
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

    protected fun addChannelInput(builder: ChatInputCreateBuilder, desc: String, req: Boolean = true, name: String? = null) {
        builder.channel(name ?: "channel", desc) {
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
            Timezone.entries.forEach {
                choice(it.friendly, it.name)
            }
        }
    }

    protected fun getTimezoneInput(interaction: ChatInputCommandInteraction): Timezone? {
        return interaction.command.strings["timezone"]?.let { Timezone.valueOf(it) }
    }

    protected suspend fun respondEmbed(response: DeferredMessageInteractionResponseBehavior,
                                       title: String? = null, desc: String? = null,
                                       embed: EmbedBuilder = EmbedBuilder(),
                                       block: EmbedBuilder.() -> Unit = {}): MessageInteractionResponse {
        embed.title = title
        embed.description = desc
        block.invoke(embed)
        return response.respond {
            addEmbed(embed)
        }
    }

    protected suspend fun respondErrorEmbed(response: DeferredMessageInteractionResponseBehavior,
                                            title: String? = null, desc: String? = null,
                                            embed: EmbedBuilder = EmbedBuilder(),
                                            block: EmbedBuilder.() -> Unit = {}): MessageInteractionResponse {
        embed.title = title
        embed.description = desc
        embed.error()
        block.invoke(embed)
        return response.respond {
            addEmbed(embed)
        }
    }

    protected suspend fun respondSuccessEmbed(response: DeferredMessageInteractionResponseBehavior,
                                              title: String? = null, desc: String? = null,
                                              embed: EmbedBuilder = EmbedBuilder(),
                                              block: EmbedBuilder.() -> Unit = {}): MessageInteractionResponse {
        embed.title = title
        embed.description = desc
        embed.success()
        block.invoke(embed)
        return response.respond {
            addEmbed(embed)
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