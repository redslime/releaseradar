/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables


import java.util.function.Function

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row2
import org.jooq.Schema
import org.jooq.SelectField
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl

import xyz.redslime.releaseradar.db.releaseradar.Releaseradar
import xyz.redslime.releaseradar.db.releaseradar.keys.ARTIST_RADAR_ARTIST_ID_FK
import xyz.redslime.releaseradar.db.releaseradar.keys.ARTIST_RADAR_RADAR_CHANNEL_ID_FK
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_ARTIST_RADAR_ARTIST_RADAR_ARTIST_ID_RADAR_ID_UINDEX
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_ARTIST_RADAR_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ArtistRadar(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, ArtistRadarRecord>?,
    aliased: Table<ArtistRadarRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<ArtistRadarRecord>(
    alias,
    Releaseradar.RELEASERADAR,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object {

        /**
         * The reference instance of <code>releaseradar.artist_radar</code>
         */
        val ARTIST_RADAR: ArtistRadar = ArtistRadar()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<ArtistRadarRecord> = ArtistRadarRecord::class.java

    /**
     * The column <code>releaseradar.artist_radar.artist_id</code>.
     */
    val ARTIST_ID: TableField<ArtistRadarRecord, String?> = createField(DSL.name("artist_id"), SQLDataType.VARCHAR(32).nullable(false), this, "")

    /**
     * The column <code>releaseradar.artist_radar.radar_id</code>.
     */
    val RADAR_ID: TableField<ArtistRadarRecord, Int?> = createField(DSL.name("radar_id"), SQLDataType.INTEGER.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<ArtistRadarRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<ArtistRadarRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.artist_radar</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.artist_radar</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.artist_radar</code> table reference
     */
    constructor(): this(DSL.name("artist_radar"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, ArtistRadarRecord>): this(Internal.createPathAlias(child, key), child, key, ARTIST_RADAR, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getPrimaryKey(): UniqueKey<ArtistRadarRecord> = KEY_ARTIST_RADAR_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<ArtistRadarRecord>> = listOf(KEY_ARTIST_RADAR_ARTIST_RADAR_ARTIST_ID_RADAR_ID_UINDEX)
    public override fun getReferences(): List<ForeignKey<ArtistRadarRecord, *>> = listOf(ARTIST_RADAR_ARTIST_ID_FK, ARTIST_RADAR_RADAR_CHANNEL_ID_FK)

    private lateinit var _artist: Artist
    private lateinit var _radarChannel: RadarChannel

    /**
     * Get the implicit join path to the <code>releaseradar.artist</code> table.
     */
    fun artist(): Artist {
        if (!this::_artist.isInitialized)
            _artist = Artist(this, ARTIST_RADAR_ARTIST_ID_FK)

        return _artist;
    }

    val artist: Artist
        get(): Artist = artist()

    /**
     * Get the implicit join path to the <code>releaseradar.radar_channel</code>
     * table.
     */
    fun radarChannel(): RadarChannel {
        if (!this::_radarChannel.isInitialized)
            _radarChannel = RadarChannel(this, ARTIST_RADAR_RADAR_CHANNEL_ID_FK)

        return _radarChannel;
    }

    val radarChannel: RadarChannel
        get(): RadarChannel = radarChannel()
    public override fun `as`(alias: String): ArtistRadar = ArtistRadar(DSL.name(alias), this)
    public override fun `as`(alias: Name): ArtistRadar = ArtistRadar(alias, this)
    public override fun `as`(alias: Table<*>): ArtistRadar = ArtistRadar(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): ArtistRadar = ArtistRadar(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): ArtistRadar = ArtistRadar(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): ArtistRadar = ArtistRadar(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------
    public override fun fieldsRow(): Row2<String?, Int?> = super.fieldsRow() as Row2<String?, Int?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (String?, Int?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (String?, Int?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
