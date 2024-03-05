package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.error
import xyz.redslime.releaseradar.success

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

        if(db.setUserEnlisted(userId, true)) {
            interaction.deferEphemeralResponse().respond {
                embed {
                    success()
                    title = ":white_check_mark: Allowed user to access /linkspotify"
                }
            }
        } else {
            interaction.deferEphemeralResponse().respond {
                embed {
                    error()
                    title = ":x: An error occurred, please see error log"
                }
            }
        }
    }
}