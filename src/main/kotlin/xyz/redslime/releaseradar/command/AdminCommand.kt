package xyz.redslime.releaseradar.command

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.commands

/**
 * @author redslime
 * @version 2023-05-22
 */
private const val ADMIN_SERVER = 546718105595019294L

abstract class AdminCommand(name: String, desc: String): Command(name, desc, perm = PermissionLevel.SERVER_ADMIN) {

    override suspend fun register(client: Kord) {
        LogManager.getLogger(javaClass).info("Registering command ${javaClass.simpleName}")
        commands.add(this)
        client.createGuildChatInputCommand(Snowflake(ADMIN_SERVER), name, description) {
            addParameters(this)
        }
    }
}