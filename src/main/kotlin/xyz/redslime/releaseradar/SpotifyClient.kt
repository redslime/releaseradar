package xyz.redslime.releaseradar

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.endpoints.pub.ArtistApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimpleArtist
import com.adamratzman.spotify.spotifyAppApi
import com.adamratzman.spotify.utils.Market
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.exception.InvalidUrlException
import xyz.redslime.releaseradar.exception.TooManyNamesException
import xyz.redslime.releaseradar.util.*
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * @author redslime
 * @version 2023-05-18
 */
class SpotifyClient {

    private val logger = LogManager.getLogger(javaClass)
    private val mutex = Mutex()
    private lateinit var api: SpotifyAppApi

    suspend fun login(spotifyClientId: String, spotifySecret: String): SpotifyClient {
        logger.info("Logging into spotify api....")
        api = spotifyAppApi(spotifyClientId, spotifySecret) {
            options.requestTimeoutMillis = Duration.ofDays(1).toMillis()
        }.build()
        logger.info(api.toString())
        return this
    }

    suspend fun <T> api(block: suspend CoroutineContext.(SpotifyAppApi) -> T): T {
        mutex.withLock {
            return block.invoke(coroutineContext, api)
        }
    }

    suspend fun getLatestRelease(artistId: String): SimpleAlbum? {
        val albums = getAllAlbums(artistId)
        logger.info("(1)$artistId: ${albums.size}")
        albums.sortByDescending { album -> album.getReleaseDate() }
        return albums.firstOrNull()
    }

    suspend fun getAlbumsAfter(artistId: String, date: LocalDateTime?): List<SimpleAlbum> {
        return try {
            val albums = getAllAlbums(artistId)
            logger.info("(2)$artistId: ${albums.size}")
            albums.filter { date?.let { it1 -> it.isReleasedAfter(it1) } == true }.toList().distinctBy { it.id }
        } catch(ex: ConnectTimeoutException) {
            logger.error("Connection timed out trying to get albums for $artistId after $date, trying again", ex)
            getAlbumsAfter(artistId, date)
        }
    }

    suspend fun getAllAlbums(artistId: String): MutableList<SimpleAlbum> {
        return api { api ->
            try {
                return@api api.artists.getArtistAlbums(artistId,
                    include = arrayOf(ArtistApi.AlbumInclusionStrategy.Album, ArtistApi.AlbumInclusionStrategy.Single),
                    market = Market.WS).getAllItemsNotNull().toMutableList()
            } catch(ex: ConnectTimeoutException) {
                logger.error("Connection timed out trying to get albums for $artistId, trying again", ex)
                getAllAlbums(artistId)
            } catch (ex: CancellationException) {
                logger.error("Coroutine to get albums for $artistId cancelled, trying again", ex)
                getAllAlbums(artistId)
            }
        }
    }

    suspend fun findArtists(cache: NameCacheProvider, str: String, useCache: Boolean = true, artistLimit: Int): Map<String, Artist?> {
        val resultMap = HashMap<String, Artist?>()
        val names = str.split(", ").flatMap { s -> s.split(",") }.filter { s -> s.isNotEmpty() }.toMutableList()
        val removeNamesLater = ArrayList<String>()

        // process uids first
        val urlList = names.filter { it.matches(artistRegex) }.toMutableList()
        names.removeAll(urlList) // remove from name list so we don't process them again later

        // with cache we can possibly look up the uid from memory
        if(useCache) {
            names.forEach { name ->
                cache.findArtistRecByName(name, true).firstOrNull()?.let { rec ->
                    rec.id?.let { artistId ->
                        removeNamesLater.add(name) // dont need to lookup this name later
                        urlList.add("artist/$artistId") // todo this kinda scuffed
                    }
                }
            }
        }

        // resolve any spotify.link urls first
        names.filter { it.matches(shortLinkRegex) }.forEach {
            resolveShortenedLink(it, logger)?.let { uid ->
                if(uid.matches(artistRegex)) {
                    removeNamesLater.add(it)
                    urlList.add(uid)
                }
            }
        }

        names.removeAll(removeNamesLater)

        // fetch artists by uid list
        if(urlList.isNotEmpty()) {
            val uidList = urlList.map { it.replace(artistRegex, "$1") }
            val uidArtists = api { api ->
                return@api api.artists.getArtists(*uidList.toTypedArray())
            }

            uidArtists.forEachIndexed { index, artist ->
                val originalInput = urlList[index]
                resultMap[originalInput] = artist
            }
        }

        if(names.size > artistLimit) // only look at the limit down here cuz everything above can be batched
            throw TooManyNamesException(artistLimit)

        // fetch (remaining) artists by name
        names.forEach {
            val artist = findArtistByName(it)
            resultMap[it] = artist
        }

        return resultMap
    }

    suspend fun findArtistByName(name: String, exact: Boolean = true): Artist? {
        val list = api { api ->
            api.search.searchArtist(name)
        }
        val artists = list.getAllItemsNotNull().filter { !exact || it.name.equals(name, ignoreCase = true) }

        if (artists.isNotEmpty()) {
            return artists.firstOrNull()
        } else {
            if(exact) // 0 results with exact filter, try again unexact
                return findArtistByName(name, false)
            return null
        }
    }

    suspend fun getArtistsFromPlaylist(_playlistUrl: String): List<SimpleArtist> {
        var playlistUrl = _playlistUrl

        if(playlistUrl.matches(shortLinkRegex)) {
            playlistUrl = resolveShortenedLink(playlistUrl, logger) ?: ""
        }

        if(!playlistUrl.matches(playlistRegex))
            throw InvalidUrlException()

        val uid = playlistUrl.replace(playlistRegex, "$1")
        var artists = api { api ->
            api.playlists.getPlaylistTracks(uid).getAllItemsNotNull()
                .flatMap { it.track?.asTrack?.artists.orEmpty() }
        }
        artists = artists.distinctBy { it.id }
        return artists
    }

    suspend fun getAlbumsBatch(albumIds: List<String>): List<Album> {
        if(albumIds.isEmpty())
            return emptyList()

        return api { api ->
            api.albums.getAlbums(*albumIds.toTypedArray()).filterNotNull()
        }
    }

    suspend fun getAlbumFromTrack(trackId: String): SimpleAlbum? {
        return api { api ->
            api.tracks.getTrack(trackId)?.album
        }
    }

    suspend fun getAlbumInstance(albumId: String): Album? {
        return api { api ->
            api.albums.getAlbum(albumId, Market.WS)
        }
    }

    suspend fun getArtistsFromUrl(url: String): List<SimpleArtist>? {
        if(url.matches(albumRegex))
            return api { api ->
                api.albums.getAlbum(url.replace(albumRegex, "$1"))?.artists
            }
        if(url.matches(trackRegex))
            return api { api ->
                api.tracks.getTrack(url.replace(trackRegex, "$1"))?.artists
            }
        return null
    }
}
