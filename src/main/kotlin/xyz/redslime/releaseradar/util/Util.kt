package xyz.redslime.releaseradar.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import kotlinx.datetime.toKotlinInstant
import org.apache.logging.log4j.Logger
import xyz.redslime.releaseradar.DiscordClient
import xyz.redslime.releaseradar.plural
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.toAlbum
import java.net.HttpURLConnection
import java.net.URL
import java.time.*

/**
 * @author redslime
 * @version 2023-05-19
 */

val emojiRegex = Regex("<(a)?:(.*):([0-9]*)>")
val albumRegex = Regex(".*album/([A-z0-9]{22}).*")
val trackRegex = Regex(".*track/([A-z0-9]{22}).*")
val labelRegex = Regex(".*Label: (.*)")
val reminderEmoji = ReactionEmoji.Unicode("\u23F0")

fun plural(str: String, count: Int): String {
    return str.plural(count)
}

fun pluralPrefixed(str: String, count: Int): String {
    return "$count ${str.plural(count)}"
}

fun getNextMidnightNZ(): ZonedDateTime {
    // this is actually midnight in Samoa (GMT-14 exists too but this is the earliest when songs become available)
    val date = LocalDate.now(ZoneId.of("Etc/GMT-13"))
    val time = LocalTime.MIDNIGHT
    return LocalDateTime.of(date, time).plusDays(1).atZone(ZoneId.of("Etc/GMT-13"))
}

fun getMillisUntilMidnightNZ(): Long {
    return getNextMidnightNZ().toInstant().toEpochMilli() - System.currentTimeMillis()
}

fun getMillisUntilTopOfTheHour(): Long {
    val now = LocalDateTime.now().atZone(ZoneId.of("UTC"))
    val date = LocalDateTime.now().atZone(ZoneId.of("UTC"))
        .plusHours(1).withMinute(0).withSecond(0).withNano(0)

    return date.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
}

fun getStartOfToday(): kotlinx.datetime.Instant {
    return LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant()
}

fun extractSpotifyLink(msg: Message): String? {
    msg.data.embeds.forEach {
        it.url.value?.let { url -> return url } // this handles the standard spotify embed
        it.description.value?.let { desc -> // and this our custom embed
            desc.lines().forEach { line ->
                if (line.matches(albumRegex) || line.matches(trackRegex)) {
                    return line
                }
            }
        }
    }

    return null
}

fun extractEmbedArtistTitle(msg: Message): String? {
    return msg.data.embeds.firstOrNull()?.title?.value
}

fun extractEmbedLabel(msg: Message): String? {
    msg.data.embeds.firstOrNull()?.description?.value?.lines()?.forEach { line ->
        if(line.matches(labelRegex)) {
            return line.replace(labelRegex, "$1")
        }
    }

    return null
}

suspend fun addPostLater(line: String, user: User): Boolean {
    if (line.matches(albumRegex)) {
        val albumId = line.replace(albumRegex, "$1")
        return DiscordClient.postLaterTask.add(albumId, user)
    } else if (line.matches(trackRegex)) {
        val trackId = line.replace(trackRegex, "$1")
        return spotify.getAlbumFromTrack(trackId)?.toAlbum()
            ?.let { DiscordClient.postLaterTask.add(it.id, user) } ?: false
    }

    return false
}

suspend fun printToDiscord(client: Kord, logger: Logger, str: String) {
    logger.info(str)
    val channel = client.getChannel(Snowflake(1108804483938525325L)) as TextChannel
    channel.createMessage(str)
}

fun resolveShortenedLink(shortenedLink: String, logger: Logger): String? {
    val url = URL(shortenedLink)

    val connection = url.openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    connection.connect()

    when (val responseCode = connection.responseCode) {
        in 300 until 400 -> {
            val redirectUrl = connection.getHeaderField("Location")
            return if (redirectUrl != null) {
                // Recursively resolve the redirected URL
                resolveShortenedLink(redirectUrl, logger)
            } else {
                // If no 'Location' header is found, return the original shortened link
                shortenedLink
            }
        }
        HttpURLConnection.HTTP_OK -> {
            // If the response code is 200, it means it's not a shortened link or it's already resolved
            return shortenedLink
        }
        else -> {
            logger.error("Failed to resolve the shortened link. Response code: $responseCode")
            return null
        }
    }
}