/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables.records


import org.jooq.Record2
import org.jooq.impl.UpdatableRecordImpl

import xyz.redslime.releaseradar.db.releaseradar.tables.ArtistRadarExclude


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ArtistRadarExcludeRecord() : UpdatableRecordImpl<ArtistRadarExcludeRecord>(ArtistRadarExclude.ARTIST_RADAR_EXCLUDE) {

    open var artistId: String?
        set(value): Unit = set(0, value)
        get(): String? = get(0) as String?

    open var radarId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record2<String?, Int?> = super.key() as Record2<String?, Int?>

    /**
     * Create a detached, initialised ArtistRadarExcludeRecord
     */
    constructor(artistId: String? = null, radarId: Int? = null): this() {
        this.artistId = artistId
        this.radarId = radarId
        resetChangedOnNotNull()
    }
}
