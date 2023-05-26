package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.db

/**
 * @author redslime
 * @version 2023-05-19
 */
class QuitListener {

    fun register(client: Kord) {
        client.on<GuildDeleteEvent> {
            db.purgeServerData(guildId.asLong())
        }
    }
}