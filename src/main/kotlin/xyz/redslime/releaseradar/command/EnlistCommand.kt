package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import xyz.redslime.releaseradar.db

/**
 * @author redslime
 * @version 2023-09-22
 */
class EnlistCommand: AdminCommand("enlist", "Allow a user to use /linkspotify") {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        builder.string("userid", "The users snowflake id") {
            required = true
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val userId = interaction.command.strings["userid"]!!.toLong()
        val response = interaction.deferEphemeralResponse()

        if(db.setUserEnlisted(userId, true)) {
            respondSuccessEmbed(response, ":white_check_mark: Allowed user to access /linkspotify")
        } else {
            respondErrorEmbed(response, ":x: An error occurred, please see error log")
        }
    }
}