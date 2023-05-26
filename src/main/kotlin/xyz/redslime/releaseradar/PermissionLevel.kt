package xyz.redslime.releaseradar

import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

/**
 * @author redslime
 * @version 2023-05-19
 */
enum class PermissionLevel {

    SERVER_ADMIN,
    CONFIG_CHANNEL,
    EVERYONE;

    suspend fun hasPermission(client: Kord, interaction: ChatInputCommandInteraction): Boolean {
        return interaction.data.member.value?.let { memberData ->
            client.getGuildOrNull(memberData.guildId)?.let { guild ->
                when(this@PermissionLevel) {
                    SERVER_ADMIN -> {
                        val roles = memberData.roles.mapNotNull { guild.getRoleOrNull(it) }
                        roles.any { it.permissions.contains(Permission.Administrator) } || guild.ownerId.asLong() == memberData.userId.asLong()
                    }
                    CONFIG_CHANNEL -> interaction.channelId.asLong() == cache.getConfigChannelId(guild.id.asLong())
                    EVERYONE -> true
                }
            }
        } ?: false

    }
}