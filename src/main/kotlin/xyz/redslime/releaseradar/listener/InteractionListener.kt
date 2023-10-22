package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.interactionManager
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.Timezone
import kotlin.coroutines.coroutineContext

/**
 * @author redslime
 * @version 2023-05-20
 */
class InteractionListener {

    companion object {
        val timezoneCallbacks = mutableListOf<Pair<Long, Timezone.() -> Unit>>()
    }

    fun register(client: Kord) {
        client.on<ButtonInteractionCreateEvent> {
            this.interaction.data.data.customId.value.let { key ->
                interactionManager.buttons.filter { it.key == key }.forEach { _ ->
                    interactionManager.buttons[key]?.invoke(coroutineContext, this@on.interaction)
                    interactionManager.buttons.remove(key)
                    key?.split("$")?.getOrNull(0)?.let { removePrefixedListeners(it) }
                }

                interactionManager.staticButtons.filter { it.key == key }.forEach { _ ->
                    interactionManager.staticButtons[key]?.invoke(coroutineContext, this@on.interaction)
                }
            }
        }

        client.on<SelectMenuInteractionCreateEvent> {
            val interaction = this.interaction
            val userId = interaction.user.id.asLong()
            val selected = this.interaction.values.first()

            interaction.data.data.customId.value.let { key ->
                if(key == "timezone-prompt") {
                    val timezone = Timezone.valueOf(selected)

                    interaction.deferEphemeralResponse().respond {
                        timezoneCallbacks.filter { it.first == userId }.forEach { it.second.invoke(timezone) }
                        timezoneCallbacks.removeIf { it.first == userId }
                        db.setUserTimezone(userId, timezone)

                        embed {
                            success()
                            title = "Saved your timezone"
                            description = "Set to: ${timezone.friendly}"
                            footer {
                                text = "You can change this anytime by typing /mytimezone"
                            }
                        }
                    }
                    interaction.message.delete()
                } else {
                    interactionManager.selectors.filter { it.key == selected }.let {
                        interactionManager.selectors.remove(selected)?.invoke(coroutineContext, interaction)
                    }

                    selected.split("$").getOrNull(0)?.let { removePrefixedListeners(it) }
                }
            }
        }
    }

    private fun removePrefixedListeners(prefix: String) {
        val buttons = interactionManager.buttons.filterKeys { it.startsWith(prefix) }.keys
        buttons.forEach { interactionManager.buttons.remove(it) }

        val selectors = interactionManager.selectors.filterKeys { it.startsWith(prefix) }.keys
        selectors.forEach { interactionManager.selectors.remove(it) }
    }
}