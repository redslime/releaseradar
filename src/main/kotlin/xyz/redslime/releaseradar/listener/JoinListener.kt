package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.DiscordClient
import xyz.redslime.releaseradar.startedAt
import xyz.redslime.releaseradar.success
import java.time.Duration

/**
 * @author redslime
 * @version 2023-05-19
 */
class JoinListener {

    fun register(client: Kord) {
        client.on<GuildCreateEvent> {
            if(System.currentTimeMillis() - startedAt < Duration.ofSeconds(30).toMillis())
                return@on
            if(DiscordClient.disconnectedGuilds.remove(this.guild.id))
                return@on

            guild.systemChannel?.asChannel()?.createEmbed {
                success()
                title = "Hi there!"
                description = "To get started, choose a protected channel and run ``/setconfigchannel`` there.\n" +
                        "Everyone with access to specified channel will be able to edit release radar lists."
                thumbnail {
                    url = "https://redslime.xyz/happyhehe.png"
                }
            }
        }
    }
}