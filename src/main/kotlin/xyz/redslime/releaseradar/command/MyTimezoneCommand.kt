package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db

/**
 * @author redslime
 * @version 2023-05-26
 */
class MyTimezoneCommand: Command("mytimezone", "Set your own timezone", dms = true) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addTimezoneInput(builder, "The timezone you live in to receive track reminders at the correct time")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val timezone = getTimezoneInput(interaction)!!

        interaction.deferEphemeralResponse().respond {
            content = "Saved your timezone: ${timezone.friendly}"
            db.setUserTimezone(interaction.user.id.asLong(), timezone)
        }
    }
}