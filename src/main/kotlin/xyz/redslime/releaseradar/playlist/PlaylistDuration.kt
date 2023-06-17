package xyz.redslime.releaseradar.playlist

import xyz.redslime.releaseradar.util.Timezone
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * @author redslime
 * @version 2023-06-16
 */
enum class PlaylistDuration(private val ly: String? = null) {

    NEVER,
    DAY("Daily"),
    WEEK("Weekly"),
    YEAR("Yearly");

    fun getDescription(): String {
        if(this == NEVER)
            return "Never - Always use the same playlist"
        return "$ly - Create a new playlist every ${name.lowercase()}"
    }

    fun getDefaultPlaylistName(): String {
        return when(this) {
            NEVER -> "New Releases"
            else -> "New Releases (${getTitleToday()})"
        }
    }

    fun getTitleToday(): String {
        val now = ZonedDateTime.now(Timezone.ASAP.zone)

        return when(this) {
            WEEK -> "Week ${now.format(getDateFormatter())}"
            else -> now.format(getDateFormatter())
        }
    }

    private fun getDateFormatter(): DateTimeFormatter {
        return when(this) {
            NEVER -> DateTimeFormatter.ISO_DATE
            DAY -> DateTimeFormatter.ofPattern("MMMM d, yyyy")
            WEEK -> DateTimeFormatter.ofPattern("w, yyyy")
            YEAR -> DateTimeFormatter.ofPattern("yyyy")
        }
    }
}