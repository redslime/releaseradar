package xyz.redslime.releaseradar

import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.AlbumResultType
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import dev.kord.common.Color
import dev.kord.core.behavior.channel.MessageChannelBehavior
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

suspend fun postRadarAlbum(album: Album, embed: EmbedBuilder, ch: MessageChannelBehavior, radarId: Int) {
    val embedType = cache.getEmbedType(radarId)
    val reacts = cache.getRadarEmotes(radarId)

    when(embedType) {
        EmbedType.CUSTOM -> {
            val msg = ch.createMessage {
                addEmbed(embed)
            }
            addReactions(msg, reacts)
        }
        EmbedType.SPOTIFY -> ch.createMessage("${album.getSmartLink()}")
    }
}

suspend fun postRadarAlbum(album: Album, ch: MessageChannelBehavior, radarId: Int) {
    val embed = buildAlbumEmbed(album, true)
    postRadarAlbum(album, embed, ch, radarId)
}

suspend fun addReactions(msg: Message, reacts: List<ReactionEmoji>) {
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

fun buildNoSuchArtist(name: String, builder: EmbedBuilder) {
    builder.error()
    builder.title = ":x: No artist named $name found"
}

suspend fun buildAlbumEmbed(album: Album, builder: EmbedBuilder, radarPost: Boolean = false, color: Color? = null) {
    if(album.albumType == AlbumResultType.Single && album.totalTracks == 1) {
        album.tracks.first()?.toTrack()?.let { track -> buildTrackEmbed(track, builder, radarPost = radarPost, color = color) }
        return
    }

    val artists = album.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
    val date = album.releaseDate.getFriendly()
    val year = album.releaseDate.year
    val artworkUrl = album.images?.get(0)?.url ?: ""
    var type = album.albumType.name.qapitalize()
    val label = album.label

    if(album.albumType == AlbumResultType.Single && album.totalTracks > 1 && album.tracks.any { it?.name?.contains(album.name) == false }) {
        type = "EP" // this isn't always accurate but works pretty well most of the time
    }

    builder.author {
        this.name = artists
        this.icon = album.artists.first().toArtist()?.images?.get(0)?.url ?: ""
    }
    builder.title = album.name
    builder.url = album.externalUrls.spotify
    builder.color = color ?: getArtworkColor(artworkUrl)
    builder.thumbnail {
        url = artworkUrl
    }

    if(radarPost) {
        var tracklist = ""
        var durTotal = 0

        album.tracks.forEachIndexed { index, simpleTrack ->
            if(simpleTrack != null) {
                val extra = simpleTrack.artists.stream().filter { !album.artists.contains(it) }.toList()
                val dur = simpleTrack.durationMs.getDurationFriendly()
                durTotal += simpleTrack.durationMs

                tracklist += if(simpleTrack.artists.size > 1 && extra.isNotEmpty()) {
                    "${index+1}. ${simpleTrack.name} (w/ ${extra.filter { it.name != null }.joinToString(" & ") { it.name!! }}) ($dur)\n"
                } else {
                    "${index+1}. ${simpleTrack.name} ($dur)\n"
                }
            }
        }

//        builder.description = "$tracklist\n$date • $label • ${durTotal.getDurationFriendly()}"
        builder.description = tracklist +
                "\n$label\n" +
                "$date • ${durTotal.getDurationFriendly()}"
        builder.footer {
            text = "Hit ⏰ for a DM when it's out in your timezone"
        }
    } else {
        val dur = album.tracks.sumOf { it?.durationMs ?: 0 }.getDurationFriendly()
        builder.description = "$type • $label • ${album.totalTracks} tracks • $dur • $year"
    }
}

suspend fun buildAlbumEmbed(album: Album, radarPost: Boolean = false, color: Color? = null): EmbedBuilder {
    val eb = EmbedBuilder()
    buildAlbumEmbed(album, eb, radarPost = radarPost, color = color)
    return eb
}

suspend fun buildAlbumEmbed(albumId: String, builder: EmbedBuilder, radarPost: Boolean = false, color: Color? = null) {
    spotify.api { it.albums.getAlbum(albumId, Market.WS) }?.let { album ->
        buildAlbumEmbed(album, builder, radarPost = radarPost, color = color)
    }
}

suspend fun buildTrackEmbed(track: Track, builder: EmbedBuilder, radarPost: Boolean = false, color: Color? = null) {
    val artists = track.artists.filter { it.name != null }.joinToString(", ") { it.name!! }
    val date = track.album.releaseDate?.getFriendly()
    val year = track.album.releaseDate?.year
    val artworkUrl = track.album.images?.get(0)?.url ?: ""
    val label = track.album.toAlbum()?.label
    var type = when(track.album.albumType) {
        AlbumResultType.Single -> "Single"
        else -> track.album.name
    }

    if((track.album.totalTracks ?: 1) > 1 && track.name != track.album.name) {
        type = track.album.name
    }

    builder.author {
        this.name = artists
        this.icon = track.artists.first().toArtist()?.images?.get(0)?.url ?: ""
    }
    builder.title = track.name
    builder.url = if(radarPost) track.album.externalUrls.spotify else track.externalUrls.spotify
    builder.color = color ?: getArtworkColor(artworkUrl)
    builder.thumbnail {
        url = artworkUrl
    }

    if(radarPost) {
//        builder.description = "$date • $label • ${track.getDurationFriendly()}"
        builder.description = "$label\n" +
                "$date • ${track.getDurationFriendly()}"
        builder.footer {
            text = "Hit ⏰ for a DM when it's out in your timezone"
        }
    } else {
        builder.description = "$type • $label • ${track.getDurationFriendly()} • $year"
    }
}

suspend fun buildTrackEmbed(singleId: String, builder: EmbedBuilder) {
    spotify.api { it.tracks.getTrack(singleId, Market.WS) }?.let { track ->
        buildTrackEmbed(track, builder)
    }
}

suspend fun buildTrackEmbed(track: SimpleTrack): EmbedBuilder {
    val eb = EmbedBuilder()
    buildTrackEmbed(track.toTrack()!!, eb)
    return eb
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