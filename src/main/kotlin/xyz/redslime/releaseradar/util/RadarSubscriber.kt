package xyz.redslime.releaseradar.util

import com.adamratzman.spotify.models.Album

/**
 * @author redslime
 * @version 2026-03-06
 */
interface RadarSubscriber {

    fun onNewAlbum(album: Album, radarIds: List<Int>)
}