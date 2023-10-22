package xyz.redslime.releaseradar.util

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.StringSelectBuilder
import dev.kord.rest.builder.component.option
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.interactionManager
import xyz.redslime.releaseradar.webServer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * @author redslime
 * @version 2023-09-22
 */
interface Interactable {

    fun addInteractionButton(builder: ActionRowBuilder, style: ButtonStyle, label: String, emoji: ReactionEmoji.Unicode? = null, groupId: String? = "${builder.hashCode()}", block: suspend CoroutineContext.(ButtonInteraction) -> Unit) {
        val key = "$groupId$${label.lowercase()}-${System.currentTimeMillis().hashCode()}"
        builder.interactionButton(style, key) {
            this.label = label
            emoji?.let { emoji(it) }
        }

        interactionManager.buttons[key] = block
    }

    fun addStaticInteractionButton(key: String, builder: ActionRowBuilder, style: ButtonStyle, label: String, emoji: ReactionEmoji.Unicode? = null) {
        builder.interactionButton(style, key) {
            this.label = label
            emoji?.let { emoji(it) }
        }
    }

    fun addSelectOption(builder: StringSelectBuilder, label: String, key: String, groupId: String? = "${builder.hashCode()}", block: suspend CoroutineContext.(SelectMenuInteraction) -> Unit) {
        val keyk = "$groupId$$key-${System.currentTimeMillis().hashCode()}"
        builder.option(label, keyk)
        interactionManager.selectors[keyk] = block
    }

    fun addMasterSpotifyLinkButton(builder: ActionRowBuilder, block: suspend CoroutineContext.(Boolean) -> Unit) {
        builder.linkButton(webServer.getAuthUrl(true) {
            if (this != null) {
                db.updateSpotifyMasterRefreshToken(this)
                block.invoke(coroutineContext, true)
            } else {
                block.invoke(coroutineContext, false)
            }
        }) {
            label = "Link Spotify"
            emoji(ReactionEmoji.Unicode("\uD83D\uDD17"))
        }
    }

    fun addSpotifyLinkButton(builder: ActionRowBuilder, user: User, public: Boolean, block: suspend CoroutineContext.(Boolean) -> Unit) {
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
}