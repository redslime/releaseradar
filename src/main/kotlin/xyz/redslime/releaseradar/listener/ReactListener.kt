package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.db
import xyz.redslime.releaseradar.getDbId
import xyz.redslime.releaseradar.util.*

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

            if(this.emoji == ReactionEmoji.Unicode("\u23F0")) {
                extractSpotifyLink(message)?.let {
                    val albumId = it.replace(albumRegex, "$1")
                    addPostLater(it, user)
                    db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, clock = true)
                }
            } else {
                this.getChannelOrNull()?.let { ch -> cache.getRadarId(ch.getDbId())?.let { rid ->
                    extractAlbumId(message)?.let { albumId ->
                        val emotes = cache.getRadarEmotes(rid)

                        when(this.emoji) {
                            emotes[0] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, like = true)
                            emotes[1] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, dislike = true)
                            emotes[2] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, heart = true)
                            is ReactionEmoji.Custom -> return@on
                            is ReactionEmoji.Unicode -> return@on
                        }
                    }
                }}
            }
        }

        client.on<ReactionRemoveEvent> {
            val message = this.message.fetchMessage()
            val user = this.user.asUser()

            if(message.author?.id != client.getSelf().id)
                return@on

            this.getChannelOrNull()?.let { ch -> cache.getRadarId(ch.getDbId())?.let { rid ->
                extractAlbumId(message)?.let { albumId ->
                    val emotes = cache.getRadarEmotes(rid)

                    when(this.emoji) {
                        emotes[0] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, like = false)
                        emotes[1] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, dislike = false)
                        emotes[2] -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, heart = false)
                        reminderEmoji -> db.logUserReact(client, user.id.asLong(), message.getGuild().id.asLong(), albumId, message.timestamp, clock = false)
                        is ReactionEmoji.Custom -> return@on
                        is ReactionEmoji.Unicode -> return@on
                    }
                }
            }}
        }
    }
}