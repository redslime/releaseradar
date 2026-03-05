package xyz.redslime.releaseradar.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.commands
import xyz.redslime.releaseradar.config

/**
 * @author redslime
 * @version 2023-05-22
 */
abstract class AdminCommand(name: String, desc: String): Command(name, desc, perm = PermissionLevel.SERVER_ADMIN) {

    override suspend fun register(client: Kord) {
        config.adminGuild?.let { gid ->
            LogManager.getLogger(javaClass).info("Registering command ${javaClass.simpleName}")
            commands.add(this)
            client.createGuildChatInputCommand(Snowflake(gid), name, description) {
                addParameters(this)
            }
        } ?: LogManager.getLogger(javaClass).warn("Not registering command ${javaClass.simpleName}, no admin guild set in config!")
    }
}