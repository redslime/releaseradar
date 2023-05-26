package xyz.redslime.releaseradar.listener

import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import xyz.redslime.releaseradar.DiscordClient
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.toAlbum

/**
 * @author redslime
 * @version 2023-05-26
 */
class ReactListener {

    private val albumRegex = Regex(".*album/([A-z0-9]{22}).*")
    private val trackRegex = Regex(".*track/([A-z0-9]{22}).*")

    fun register(client: Kord) {
        client.on<ReactionAddEvent> {
            val message = this.message.fetchMessage()
            val user = this.user.asUser()

            if(message.author?.id != client.getSelf().id)
                return@on
            if(this.emoji != ReactionEmoji.Unicode("\u23F0"))
                return@on

            message.data.embeds.forEach {
                it.url.value?.let { url -> processLine(url, user) } // this handles the standard spotify embed
                it.description.value?.let { desc -> // and this our custom embed
                    desc.lines().forEach { line ->
                        processLine(line, user)
                    }
                }
            }
        }
    }

    private suspend fun processLine(line: String, user: User) {
        if (line.matches(albumRegex)) {
            val albumId = line.replace(albumRegex, "$1")
            DiscordClient.postLaterTask.add(albumId, user)
        } else if (line.matches(trackRegex)) {
            val trackId = line.replace(trackRegex, "$1")
            spotify.getAlbumFromTrack(trackId)?.toAlbum()
                ?.let { DiscordClient.postLaterTask.add(it.id, user) }
        }
    }
}