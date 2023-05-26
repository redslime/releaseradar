package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.Timezone

/**
 * @author redslime
 * @version 2023-05-26
 */
class SetTimezoneCommand: Command("settimezone", "Sets the timezone of the specified radar to post new releases at midnight in the specified timezone", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel to set the timezone for")
        addTimezoneInput(builder, "The timezone to post new releases at midnight in the specified timezone")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = getChannelInput(interaction)!!
        val radarId = db.getRadarId(channel)
        val timezone = getTimezoneInput(interaction)!!

        interaction.deferPublicResponse().respond {
            db.setRadarTimezone(radarId, timezone)

            embed {
                success()
                title = "Updated timezone of ${channel.mention}"
                description = if(timezone == Timezone.ASAP)
                    "New tracks will now be posted as soon as possible"
                else
                    "New tracks will now be posted when it's midnight in ${timezone.friendly}"
            }

            actionRow {
                addInteractionButton(this, ButtonStyle.Success, "Apply to all radars") {
                    it.deferPublicResponse().respond {
                        db.setServerTimezone(it.message.getGuild().id.asLong(), timezone)

                        embed {
                            success()
                            title = "Updated timezone for all radars"
                            description = if(timezone == Timezone.ASAP)
                                "New tracks will now be posted as soon as possible"
                            else
                                "New tracks will now be posted when it's midnight in ${timezone.friendly}"
                        }
                    }
                    it.message.delete()
                }
            }
        }
    }
}