package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.*

/**
 * @author redslime
 * @version 2023-05-26
 */
class EmbedTypeCommand: Command("embedtype", "Whether new releases should be posted as a custom embed or the standard spotify embed", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel to set the embed type of")
        builder.string("type", "The embed type that should be used for new releases") {
            required = true
            EmbedType.values().forEach {
                choice(it.getFriendly(), it.name)
            }
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = getChannelInput(interaction)!!
        val radarId = db.getRadarId(channel)
        val type = EmbedType.valueOf(interaction.command.strings["type"]!!)

        interaction.deferPublicResponse().respond {
            db.setRadarEmbedType(radarId, type)

            embed {
                success()
                title = "Updated embed type of ${channel.mention}"
                description = "The ${type.getFriendly()} embed will now be used for new tracks"
            }

            actionRow {
                addInteractionButton(this, ButtonStyle.Success, "Apply to all radars") {
                    it.deferPublicResponse().respond {
                        db.setServerEmbedType(it.message.getGuild().id.asLong(), type)

                        embed {
                            success()
                            title = "Updated embed type for all radars"
                            description = "The ${type.getFriendly()} embed will now be used for new tracks everywhere"
                        }
                    }
                    it.message.delete()
                }
            }
        }
    }
}