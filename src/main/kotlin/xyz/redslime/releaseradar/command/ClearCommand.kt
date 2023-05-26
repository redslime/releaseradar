package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.warning

/**
 * @author redslime
 * @version 2023-05-20
 */
class ClearCommand : Command("clear", "Clears the entire radar list of the specified channel", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel to clear the radar list in")
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = getChannelInput(interaction)!!

        interaction.respondPublic {
            embed {
                warning()
                title = ":warning: Are you sure?"
                description = "Clearing the radar list of ${channel.mention} will remove all artists from the radar!\n" +
                        "This action can not be undone!"
            }
            actionRow {
                addInteractionButton(this, ButtonStyle.Danger, "Confirm") {
                    db.clearRadar(db.getRadarId(channel))
                    it.message.delete()
                    it.respondPublic {
                        embed {
                            success()
                            title = "Cleared release radar in ${channel.mention}"
                        }
                    }
                }
                addInteractionButton(this, ButtonStyle.Secondary, "Cancel") {
                    it.message.delete()
                }
            }
        }
    }
}