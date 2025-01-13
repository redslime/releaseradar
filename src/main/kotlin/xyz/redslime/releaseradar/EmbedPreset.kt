package xyz.redslime.releaseradar

import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.AlbumResultType
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.listener.InteractionListener.Companion.timezoneCallbacks
import xyz.redslime.releaseradar.util.Timezone
import xyz.redslime.releaseradar.util.getArtworkColor
import xyz.redslime.releaseradar.util.reminderEmoji

/**
 * @author redslime
 * @version 2023-05-19
 */

suspend fun postAlbum(album: Album, channel: MessageChannelBehavior, radarId: Int) {
    val msg: Message
    val reacts = cache.getRadarEmotes(radarId)

    if(cache.getEmbedType(radarId) == EmbedType.CUSTOM) {
        msg = channel.createEmbed {
            buildAlbum(album, this)
        }
    } else {
        msg = channel.createMessage("${album.getSmartLink()}")
    }

    try {
        msg.addReaction(reacts[0])
    } catch (ex: Exception) {
        msg.addReaction(ReactionEmoji.Unicode("\uD83D\uDC4D")) // Like
    }

    try {
        msg.addReaction(reacts[1])
    } catch (ex: Exception) {
        msg.addReaction(ReactionEmoji.Unicode("\uD83D\uDC4E")) // Dislike
    }

    try {
        msg.addReaction(reacts[2])
    } catch (ex: Exception) {
        msg.addReaction(ReactionEmoji.Unicode("\u2764\uFE0F")) // Heart
    }

    msg.addReaction(reminderEmoji) // Alarm clock
}

suspend fun postTimezonePrompt(user: User, block: Timezone.() -> Unit) {
    timezoneCallbacks.add(Pair(user.id.asLong(), block))
    user.getDmChannelOrNull()?.createMessage {
        embed {
            warning()
            title = "Please select your timezone"
            description = "This is necessary so you receive track reminders at the correct time"
            footer {
                text = "You can change this anytime by typing /mytimezone"
            }
        }
        actionRow {
            stringSelect("timezone-prompt") {
                Timezone.entries.forEach {
                    option(it.friendly, it.name)
                }
            }
        }
    }
}

fun buildAlbum(album: Album?, builder: EmbedBuilder, footer: Boolean = true) {
    if(album != null) {
        val artists = album.artists.filter { it.name != null }.joinToString(" & ") { it.name!! }
        val name = album.name
        val label = album.label
        val releaseDate = album.releaseDate.getFriendly()

        builder.color = EmbedColor.GREEN.color
        builder.title = "$artists\n$name"
        builder.thumbnail {
            url = album.images?.get(0)?.url ?: ""
        }
        builder.description = "Label: $label\n" +
                "Release Date: $releaseDate\n"

        if(album.totalTracks > 1) {
            builder.description += "\nTracklist:\n"
            album.tracks.forEachIndexed { index, simpleTrack ->
                if(simpleTrack != null) {
                    val extra = simpleTrack.artists.stream().filter { !album.artists.contains(it) }.toList()

                    if(simpleTrack.artists.size > 1 && extra.isNotEmpty()) {
                        builder.description += "${index+1}. ${simpleTrack.name} (w/ ${extra.filter { it.name != null }.joinToString(" & ") { it.name!! }})\n"
                    } else {
                        builder.description += "${index+1}. ${simpleTrack.name}\n"
                    }
                }
            }
        }

        builder.description += "\n\uD83C\uDFB6 [Spotify Link](${album.externalUrls.spotify})"

        if(footer) {
            builder.footer {
                text = "Hit ⏰ for a DM when it's out in your timezone"
            }
        }
    } else {
        builder.color = EmbedColor.RED.color
        builder.description = ":x: No album/single found"
    }
}

fun buildNoSuchArtist(name: String, builder: EmbedBuilder) {
    builder.error()
    builder.title = ":x: No artist named $name found"
}

suspend fun buildAlbumEmbed(albumId: String, builder: EmbedBuilder) {
    spotify.api { it.albums.getAlbum(albumId, Market.WS) }?.let { album ->
        if(album.albumType == AlbumResultType.Single && album.totalTracks == 1) {
            album.tracks.first()?.toFullTrack(Market.WS)?.let { track -> buildTrackEmbed(track, builder) }
            return
        }

        val artists = album.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
        val year = album.releaseDate.year
        val artworkUrl = album.images?.get(0)?.url ?: ""
        var type = album.albumType.name.qapitalize()

        if(album.albumType == AlbumResultType.Single && album.totalTracks > 1 && album.tracks.any { it?.name != album.name }) {
            type = "EP" // this isn't always accurate but works pretty well most of the time
        }

        builder.author {
            this.name = artists
            this.icon = album.artists.first().toFullArtist()?.images?.get(0)?.url ?: ""
        }
        builder.title = album.name
        builder.url = album.externalUrls.spotify
        builder.color = getArtworkColor(artworkUrl)
        builder.thumbnail {
            url = artworkUrl
        }
        builder.description = "$type • ${album.label} • ${album.totalTracks} tracks • $year"
    }
}

suspend fun buildTrackEmbed(track: Track, builder: EmbedBuilder) {
    val artists = track.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
    val year = track.album.releaseDate?.year
    val artworkUrl = track.album.images?.get(0)?.url ?: ""
    var type = when(track.album.albumType) {
        AlbumResultType.Single -> "Single"
        else -> track.album.name
    }

    if((track.album.totalTracks ?: 1) > 1 && track.name != track.album.name) {
        type = track.album.name
    }

    builder.author {
        this.name = artists
        this.icon = track.artists.first().toFullArtist()?.images?.get(0)?.url ?: ""
    }
    builder.title = track.name
    builder.url = track.externalUrls.spotify
    builder.color = getArtworkColor(artworkUrl)
    builder.thumbnail {
        url = artworkUrl
    }
    builder.description = "$type • ${track.album.toAlbum().label} • ${track.getDurationFriendly()} • $year"
}

suspend fun buildTrackEmbed(singleId: String, builder: EmbedBuilder) {
    spotify.api { it.tracks.getTrack(singleId, Market.WS) }?.let { track ->
        buildTrackEmbed(track, builder)
    }
}

suspend fun buildArtistEmbed(artistId: String, builder: EmbedBuilder) {
    spotify.api { it.artists.getArtist(artistId) }?.let { artist ->
        val coverUrl = artist.images?.get(0)?.url ?: ""
        builder.title = artist.name
        builder.url = artist.externalUrls.spotify
        builder.color = getArtworkColor(coverUrl)
        builder.thumbnail {
            url = coverUrl
        }

        val genres = if(artist.genres.isNotEmpty()) artist.genres.joinToString(", ", postfix = " • ") else ""
        builder.description = "$genres${"%,d".format(artist.followers.total)} followers"
    }
}