package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.DiscordClient
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.startedAt

/**
 * @author redslime
 * @version 2023-05-19
 */
class QuitListener {

    fun register(client: Kord) {
        client.on<GuildDeleteEvent> {
            if(this.unavailable) {
                DiscordClient.disconnectedGuilds.add(this.guildId)
            } else {
                db.purgeServerData(guildId.asLong())
            }
        }

        client.on<DisconnectEvent> {
            println("Discord gateway disconnected (${this.javaClass.simpleName})")
            startedAt = System.currentTimeMillis()
        }
    }
}