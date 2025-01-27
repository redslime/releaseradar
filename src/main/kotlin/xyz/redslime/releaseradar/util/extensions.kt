package xyz.redslime.releaseradar

import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.Channel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.css.CssBuilder
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
        return "$year-%02d-%02d".format(month, day)
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

fun Album.getReleaseDateTime(): LocalDateTime {
    return LocalDateTime.of(LocalDate.parse(releaseDate.getFriendlyISO()).toJavaLocalDate(), LocalTime.of(0, 0, 0, 0))
}

suspend fun SimpleAlbum.toAlbum(): Album? {
    return spotify.toFullAlbum(this)
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
        return this.tracks.firstOrNull()?.externalUrls?.spotify
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
    success()

    if(actual == 0)
        error()
    if(actual < goal)
        warning()
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

suspend fun SimpleArtist.toArtist(): Artist? {
    val id = this.id
    return spotify.api { it.artists.getArtist(id) }
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

fun String.qapitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

fun Float.formatPercentage(): String {
    return String.format("%.0f", this * 100) + "%"
}

fun SpotifyRatelimitedException.getTime(): Long {
    val time = message?.replace(Regex(".*for ([0-9]*) seconds.*"), "$1")?.toLong()
    return time ?: 0L
}

fun Track.getDurationFriendly(): String {
    val dur = Duration.ofMillis(this.durationMs.toLong())

    if(dur.toHoursPart() > 0)
        return "%d:%01d:%02d".format(dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart())
    return "%01d:%02d".format(dur.toMinutesPart(), dur.toSecondsPart())
}

suspend fun SimpleTrack.toTrack(): Track? {
    val id = this.id
    return spotify.api { it.tracks.getTrack(id, Market.WS) }
}

/**
 * Int value = time in ms
 */
fun Int.getDurationFriendly(): String {
    val dur = Duration.ofMillis(this.toLong())

    if(dur.toHoursPart() > 0)
        return "%d:%01d:%02d".format(dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart())
    return "%01d:%02d".format(dur.toMinutesPart(), dur.toSecondsPart())
}

fun MessageBuilder.addEmbed(eb: EmbedBuilder) {
    embeds?.add(eb) ?: run { embeds = mutableListOf(eb) }
}