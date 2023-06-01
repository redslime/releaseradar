package xyz.redslime.releaseradar

import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.Channel
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import xyz.redslime.releaseradar.util.emojiRegex
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @author redslime
 * @version 2023-05-18
 */

fun ReleaseDate.getFriendly(): String {
    if(day != null && month != null)
        return "%02d-%02d-$year".format(day, month)
    return "$year"
}

fun ReleaseDate.getFriendlyISO(): String {
    if(day != null && month != null)
        return "$year-%02d-%02d".format(month, day)
    if(year == 0)
        return "1970-01-01"
    return "$year-01-01"
}

fun SimpleAlbum.getReleaseDate(): LocalDate {
    return LocalDate.parse(releaseDate!!.getFriendlyISO())
}

fun SimpleAlbum.getReleaseDateTime(): LocalDateTime {
    return LocalDateTime.of(LocalDate.parse(releaseDate!!.getFriendlyISO()).toJavaLocalDate(), LocalTime.of(0, 0, 0, 0))
}

suspend fun SimpleAlbum.toAlbum(): Album {
    return toFullAlbum(Market.WS)!!
}

fun SimpleAlbum.isReleasedAfter(date: LocalDate): Boolean {
    return getReleaseDate() > date
}

fun SimpleAlbum.isReleasedAfter(date: LocalDateTime): Boolean {
    return getReleaseDateTime() > date
}

fun SimpleAlbum.isFutureRelease(): Boolean {
    return getReleaseDate() > java.time.LocalDate.now().toKotlinLocalDate()
}

fun Album.getSmartLink(): String? {
    if(this.totalTracks == 1)
        return this.tracks.first()?.externalUrls?.spotify
    return this.externalUrls.spotify
}

fun EmbedBuilder.success() {
    color = EmbedColor.GREEN.color
}

fun EmbedBuilder.error() {
    color = EmbedColor.RED.color
}

fun EmbedBuilder.warning() {
    color = EmbedColor.YELLOW.color
}

fun EmbedBuilder.colorize(actual: Int, goal: Int) {
    if(actual == 0)
        error()
    if(actual < goal)
        warning()
    success()
}

fun Channel.getDbId(): Long {
    return id.asLong()
}

fun Snowflake.asLong(): Long {
    return value.toLong()
}

fun String.plural(count: Int, plural: String? = null): String {
    return if (count != 1) {
        plural ?: (this + 's')
    } else {
        this
    }
}

fun Artist.getExternalUrls(): Map<String, String> {
    val map = (externalUrls.otherExternalUrls.map { it.name to it.url }).toMap().toMutableMap()
    map["spotify"] = externalUrls.spotify!!
    return map
}

fun Artist.toSimpleArtist(): SimpleArtist {
    return SimpleArtist(getExternalUrls(), href, id, uri, name, type)
}

fun Duration.prettyPrint(): String {
    return this.toString()
        .substring(2)
        .replace(Regex("(\\d[HMS])(?!$)"), "$1 ")
        .lowercase()
}

fun ReactionEmoji.Companion.from(str: String): ReactionEmoji {
    if(str.matches(emojiRegex)) {
        val animated = str.replace(emojiRegex, "$1") == "a"
        val name = str.replace(emojiRegex, "$2")
        val id = Snowflake(str.replace(emojiRegex, "$3"))
        return ReactionEmoji.Custom(id, name, animated)
    }
    return ReactionEmoji.Unicode(str)
}