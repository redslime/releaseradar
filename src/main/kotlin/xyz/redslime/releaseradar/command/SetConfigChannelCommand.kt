package xyz.redslime.releaseradar.command

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.*

/**
 * @author redslime
 * @version 2023-05-19
 */
class SetConfigChannelCommand : Command("setconfigchannel", "Sets the current channel to be the config channel. (Choose a protected one!)", perm = PermissionLevel.SERVER_ADMIN) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        // nothing here hehe
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = interaction.channel.asChannel()
        val guildId = interaction.data.guildId.value?.asLong()
        val configId = cache.getConfigChannelId(guildId)

        if(configId != null && configId == channel.id.asLong()) {
            respondErrorEmbed(interaction.deferEphemeralResponse(), "${channel.mention} is already the configuration channel!")
        } else {
            if (db.setConfigChannel(guildId, channel.id.asLong())) {
                respondSuccessEmbed(interaction.deferPublicResponse()) {
                    title = "${channel.mention} is now the configuration channel"
                    description =
                        "Everyone with access to ${channel.mention} is now able to edit release radar lists:\n" +
                                "Type ``/add``, ``/remove``, ``/list`` to see command options."
                    footer("You can set a new config channel with this command anytime")
                }
            } else {
                respondErrorEmbed(interaction.deferEphemeralResponse(), "Failed to set configuration channel") {
                    description = "Please try again later."
                }
            }
        }
    }
}