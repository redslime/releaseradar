package xyz.redslime.releaseradar.util

import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord

/**
 * @author redslime
 * @version 2023-09-19
 */
interface NameCacheProvider {

    suspend fun findArtistRecByName(name: String, ignoreCase: Boolean = false): List<ArtistRecord>

}