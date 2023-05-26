package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.Timezone
import kotlin.coroutines.coroutineContext

/**
 * @author redslime
 * @version 2023-05-20
 */
class InteractionListener {

    companion object {
        val timezoneCallbacks = mutableMapOf<Long, Timezone.() -> Unit>()
    }

    fun register(client: Kord) {
        client.on<ButtonInteractionCreateEvent> {
            this.interaction.data.data.customId.value.let { key ->
                commands.filter { cmd -> cmd.buttons.containsKey(key) }.forEach { cmd ->
                    cmd.buttons[key]?.invoke(coroutineContext, this@on.interaction)
                    cmd.buttons.remove(key)
                }
            }
        }

        client.on<SelectMenuInteractionCreateEvent> {
            val interaction = this.interaction
            val timezone = Timezone.valueOf(this.interaction.values.first())
            val userId = interaction.user.id.asLong()

            interaction.data.data.customId.value.let { key ->
                commands.filter { cmd -> cmd.timezoneSelectors.containsKey(key) }.forEach { cmd ->
                    cmd.timezoneSelectors[key]?.invoke(coroutineContext, timezone)
                    cmd.timezoneSelectors.remove(key)
                }

                if(key == "timezone-prompt") {
                    interaction.deferEphemeralResponse().respond {
                        timezoneCallbacks.remove(userId)?.invoke(timezone)
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
                }
            }
        }
    }
}