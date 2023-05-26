package xyz.redslime.releaseradar

/**
 * @author redslime
 * @version 2023-05-26
 */
enum class EmbedType {

    CUSTOM,
    SPOTIFY;

    fun getFriendly(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}