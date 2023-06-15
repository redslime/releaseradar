package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.util.addPostLater
import xyz.redslime.releaseradar.util.extractSpotifyLink

/**
 * @author redslime
 * @version 2023-05-26
 */
class ReactListener {

    fun register(client: Kord) {
        client.on<ReactionAddEvent> {
            val message = this.message.fetchMessage()
            val user = this.user.asUser()

            if(message.author?.id != client.getSelf().id)
                return@on
            if(this.emoji != ReactionEmoji.Unicode("\u23F0"))
                return@on

            extractSpotifyLink(message)?.let { addPostLater(it, user) }
        }
    }
}