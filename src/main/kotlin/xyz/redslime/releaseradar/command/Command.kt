package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.StringSelectBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.Timezone
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * @author redslime
 * @version 2023-05-19
 */
abstract class Command(val name: String, val description: String, val perm: PermissionLevel = PermissionLevel.EVERYONE, val dms: Boolean = false) {

    val buttons: HashMap<String, suspend CoroutineContext.(ButtonInteraction) -> Unit> = HashMap()
    val selectors: HashMap<String, suspend CoroutineContext.(SelectMenuInteraction) -> Unit> = HashMap()

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

    protected fun addInteractionButton(builder: ActionRowBuilder, style: ButtonStyle, label: String, emoji: ReactionEmoji.Unicode? = null, block: suspend CoroutineContext.(ButtonInteraction) -> Unit) {
        val key = "$name-${label.lowercase()}-${System.currentTimeMillis().hashCode()}"
        builder.interactionButton(style, key) {
            this.label = label
            emoji?.let { emoji(it) }
        }
        buttons[key] = block
    }

    protected fun addSpotifyLinkButton(builder: ActionRowBuilder, user: User, public: Boolean, block: suspend CoroutineContext.(Boolean) -> Unit) {
        builder.linkButton(webServer.getAuthUrl(public) {
            if (this != null) {
                db.updateUserRefreshToken(user.id.asLong(), this)
                block.invoke(coroutineContext, true)
            } else {
                block.invoke(coroutineContext, false)
            }
        }) {
            label = "Link Spotify"
            emoji(ReactionEmoji.Unicode("\uD83D\uDD17"))
        }
    }

    protected fun addSelectOption(builder: StringSelectBuilder, label: String, key: String, block: suspend CoroutineContext.(SelectMenuInteraction) -> Unit) {
        val keyk = "$key-${System.currentTimeMillis().hashCode()}"
        builder.option(label, keyk)
        selectors[keyk] = block
    }
}