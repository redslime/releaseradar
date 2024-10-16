package xyz.redslime.releaseradar

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.endpoints.pub.ArtistApi
import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.spotifyAppApi
import com.adamratzman.spotify.utils.Market
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord
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
    private val workers = Cycler(mutableListOf<SpotifyWorker>())

    class SpotifyWorker(private val id: String, private val secret: String) {

        private val logger = LogManager.getLogger(javaClass)
        private val ratelimitLock = Mutex()
        private lateinit var api: SpotifyAppApi
        private var ratelimitedUntil: Long = 0

        suspend fun login(): SpotifyWorker {
            api = spotifyAppApi(id, secret) {
                options.requestTimeoutMillis = Duration.ofSeconds(10).toMillis()
                options.retryWhenRateLimited = false
            }.build()
            return this
        }

        suspend fun <T> execute(block: suspend CoroutineContext.(SpotifyAppApi) -> T): T {
            return execute0 { block.invoke(coroutineContext, api) }
        }

        private suspend fun <T> execute0(block: suspend () -> T): T {
            return ratelimitLock.withLock {
                try {
                    block.invoke()
                } catch(ex: SpotifyRatelimitedException) {
                    logger.warn("Spotify api rate limited on worker", ex)
                    ratelimit(ex.getTime())
                    throw ex
                }
            }
        }

        fun isAvailable(): Boolean {
            return !isRatedlimited()
        }

        fun isRatedlimited(): Boolean {
            return ratelimitedUntil > System.currentTimeMillis()
        }

        fun ratelimit(duration: Long) {
            ratelimitedUntil = System.currentTimeMillis() + (duration * 1000L)
        }
    }

    suspend fun login(credentials: Map<String, String>): SpotifyClient {
        logger.info("Setting up ${credentials.size} Spotify workers...")
        credentials.forEach { (id, secret) ->
            workers.add(SpotifyWorker(id, secret).login())
        }

        return this
    }

    suspend fun <T> api(block: suspend CoroutineContext.(SpotifyAppApi) -> T): T {
        return try {
            coroutineScope {
                val job = async {
                    var next = workers.next { it.isAvailable() }

                    while(next == null) {
                        delay(100)
                        next = workers.next { it.isAvailable() }
                    }

                    next.execute(block)
                }
                job.await()
            }
        } catch (ex: SpotifyRatelimitedException) {
            api(block)
        }
    }

    suspend fun getLatestRelease(artistId: String): SimpleAlbum? {
        val albums = getAllAlbums(artistId)
        logger.info("(1)$artistId: ${albums.size}")
        albums.sortByDescending { album -> album.getReleaseDate() }
        return albums.firstOrNull()
    }

    suspend fun getAlbumsAfter(artistId: String, date: LocalDateTime?): List<SimpleAlbum> {
        val albums = getAllAlbums(artistId)
        logger.info("(2)$artistId: ${albums.size}")
        return albums.filter { date?.let { it1 -> it.isReleasedAfter(it1) } == true }.toList().distinctBy { it.id }
    }

    suspend fun getAlbumsAfterFlow(artists: List<ArtistRecord>): Flow<SimpleAlbum> = flow {
        artists.forEach { rec ->
            getAlbumsAfter(rec.id!!, rec.lastRelease).forEach { a ->
                emit(a)
                logger.info("(2)${rec.id} NEW: ${a.name}")
            }
        }
    }

    suspend fun getAllAlbums(artistId: String): MutableList<SimpleAlbum> {
        return try {
            api { api ->
                api.artists.getArtistAlbums(
                    artistId,
                    include = arrayOf(ArtistApi.AlbumInclusionStrategy.Album, ArtistApi.AlbumInclusionStrategy.Single),
                    market = Market.WS
                ).getAllItemsNotNull().toMutableList()
            }
        } catch(ex: SpotifyException.TimeoutException) {
            logger.error("Timed out trying to get albums for $artistId, trying again", ex)
            getAllAlbums(artistId)
        } catch(ex: ConnectTimeoutException) {
            logger.error("Timed out trying to connect to api to get albums for $artistId, trying again", ex)
            getAllAlbums(artistId)
        } catch(ex: Exception) {
            logger.error("Error trying to get albums for $artistId, trying again", ex)
            getAllAlbums(artistId)
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

    suspend fun toFullAlbum(album: SimpleAlbum): Album {
        return api { api ->
            api.albums.getAlbum(album.id, Market.WS)!!
        }
    }
}
