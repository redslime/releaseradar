import com.adamratzman.spotify.spotifyAppApi
import java.time.Duration

/**
 * @author redslime
 * @version 2023-05-22
 */

private const val spotifyClientId = ""
private const val spotifySecret = ""

suspend fun main() {
    val spotify = spotifyAppApi(spotifyClientId, spotifySecret) {
        options.requestTimeoutMillis = Duration.ofDays(1).toMillis()
    }.build()
    spotify.albums.getAlbum("79cwj69TMsoDhSLAPJzYqE")?.let {
        println(it.toString())
        println(it.type)
        println(it.albumType)
        println(it.totalTracks)
    }
}