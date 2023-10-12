package xyz.redslime.releaseradar

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.endpoints.pub.ArtistApi
import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.spotifyAppApi
import com.adamratzman.spotify.utils.Market
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.exception.InvalidUrlException
import xyz.redslime.releaseradar.exception.TooManyNamesException
import xyz.redslime.releaseradar.util.*
import java.time.Duration
import java.time.LocalDateTime

/**
 * @author redslime
 * @version 2023-05-18
 */
class SpotifyClient(private val spotifyClientId: String, private val spotifySecret: String) {

    private val logger = LogManager.getLogger(javaClass)
    private val coroutine = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    lateinit var api: SpotifyAppApi

    suspend fun login(): SpotifyClient {
        logger.info("Logging into spotify api....")
        api = spotifyAppApi(spotifyClientId, spotifySecret) {
            options.requestTimeoutMillis = Duration.ofDays(1).toMillis()
        }.build()
        logger.info(api.toString())
        return this
    }

    suspend fun getLatestRelease(artistId: String): SimpleAlbum? {
        val albums = getAllAlbums(artistId)
        albums.sortByDescending { album -> album.getReleaseDate() }
        return albums.firstOrNull()
    }

    suspend fun getAlbumsAfter(artistId: String, date: LocalDateTime?): List<SimpleAlbum> {
        return try {
            val albums = getAllAlbums(artistId)
            albums.filter { date?.let { it1 -> it.isReleasedAfter(it1) } == true }.toList().distinctBy { it.id }
        } catch(ex: ConnectTimeoutException) {
            logger.error("Connection timed out trying to get albums for $artistId after $date, trying again", ex)
            getAlbumsAfter(artistId, date)
        }
    }

    suspend fun getAllAlbums(artistId: String, offset: Int = 0): ArrayList<SimpleAlbum> {
        job?.join()
        val albums: PagingObject<SimpleAlbum>

        try {
            albums = withContext(coroutine.coroutineContext) {
                job = coroutineContext.job
                api.artists.getArtistAlbums(
                    artistId, offset = offset * 50,
                    include = arrayOf(ArtistApi.AlbumInclusionStrategy.Album, ArtistApi.AlbumInclusionStrategy.Single),
                    market = Market.WS
                )
            }
        } catch(ex: ConnectTimeoutException) {
            logger.error("Connection timed out trying to get albums for $artistId, trying again", ex)
            return getAllAlbums(artistId, offset)
        } catch (ex: CancellationException) {
            logger.error("Coroutine to get albums for $artistId cancelled, trying again", ex)
            return getAllAlbums(artistId, offset)
        }

        logger.info("$artistId: ${albums.size}")

        if (offset == 0 && albums.total == albums.getAllItemsNotNull().size) // spotify api be weird
            return ArrayList(albums.getAllItemsNotNull())

        val list = ArrayList(albums.getAllItemsNotNull())

        if (albums.total > 50 + (offset * 50)) {
            val more: ArrayList<SimpleAlbum>

            try {
                more = getAllAlbums(artistId, offset + 1)
            } catch(ex: ConnectTimeoutException) {
                logger.error("Connection timed out trying to get albums for $artistId (offset $offset), trying again", ex)
                return getAllAlbums(artistId, offset)
            }

            list.addAll(more)
            return list
        }

        return list
    }

    suspend fun findArtists(cache: NameCacheProvider, str: String, useCache: Boolean = true, artistLimit: Int): Map<String, Artist?> {
        val resultMap = HashMap<String, Artist?>()
        val names = str.split(", ").flatMap { s -> s.split(",") }.toList().toMutableList()
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

            job?.join()
            val uidArtists = withContext(coroutine.coroutineContext) {
                job = coroutineContext.job
                api.artists.getArtists(*uidList.toTypedArray())
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
        job?.join()
        val list = withContext(coroutine.coroutineContext) {
            job = coroutineContext.job
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
        job?.join()
        var artists = withContext(coroutine.coroutineContext) {
            job = coroutineContext.job
            api.playlists.getPlaylistTracks(uid).getAllItemsNotNull()
                .flatMap { it.track?.asTrack?.artists.orEmpty() }
        }
        artists = artists.distinctBy { it.id }
        return artists
    }

    suspend fun getAlbumsBatch(albumIds: List<String>): List<Album> {
        if(albumIds.isEmpty())
            return emptyList()

        return api.albums.getAlbums(*albumIds.toTypedArray()).filterNotNull()
    }

    suspend fun getAlbumFromTrack(trackId: String): SimpleAlbum? {
        return api.tracks.getTrack(trackId)?.album
    }

    suspend fun getAlbumInstance(albumId: String): Album? {
        return api.albums.getAlbum(albumId, Market.WS)
    }
}
