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
import xyz.redslime.releaseradar.db.releaseradar.keys.ARTIST_RADAR_EXCLUDE_ARTIST_ID_FK
import xyz.redslime.releaseradar.db.releaseradar.keys.ARTIST_RADAR_EXCLUDE_RADAR_CHANNEL_ID_FK
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_ARTIST_RADAR_EXCLUDE_ARTIST_RADAR_EXCLUDE_PK
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_ARTIST_RADAR_EXCLUDE_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarExcludeRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ArtistRadarExclude(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, ArtistRadarExcludeRecord>?,
    aliased: Table<ArtistRadarExcludeRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<ArtistRadarExcludeRecord>(
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
         * The reference instance of
         * <code>releaseradar.artist_radar_exclude</code>
         */
        val ARTIST_RADAR_EXCLUDE: ArtistRadarExclude = ArtistRadarExclude()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<ArtistRadarExcludeRecord> = ArtistRadarExcludeRecord::class.java

    /**
     * The column <code>releaseradar.artist_radar_exclude.artist_id</code>.
     */
    val ARTIST_ID: TableField<ArtistRadarExcludeRecord, String?> = createField(DSL.name("artist_id"), SQLDataType.VARCHAR(32).nullable(false), this, "")

    /**
     * The column <code>releaseradar.artist_radar_exclude.radar_id</code>.
     */
    val RADAR_ID: TableField<ArtistRadarExcludeRecord, Int?> = createField(DSL.name("radar_id"), SQLDataType.INTEGER.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.artist_radar_exclude</code> table
     * reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.artist_radar_exclude</code> table
     * reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.artist_radar_exclude</code> table reference
     */
    constructor(): this(DSL.name("artist_radar_exclude"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, ArtistRadarExcludeRecord>): this(Internal.createPathAlias(child, key), child, key, ARTIST_RADAR_EXCLUDE, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getPrimaryKey(): UniqueKey<ArtistRadarExcludeRecord> = KEY_ARTIST_RADAR_EXCLUDE_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<ArtistRadarExcludeRecord>> = listOf(KEY_ARTIST_RADAR_EXCLUDE_ARTIST_RADAR_EXCLUDE_PK)
    public override fun getReferences(): List<ForeignKey<ArtistRadarExcludeRecord, *>> = listOf(ARTIST_RADAR_EXCLUDE_ARTIST_ID_FK, ARTIST_RADAR_EXCLUDE_RADAR_CHANNEL_ID_FK)

    private lateinit var _artist: Artist
    private lateinit var _radarChannel: RadarChannel

    /**
     * Get the implicit join path to the <code>releaseradar.artist</code> table.
     */
    fun artist(): Artist {
        if (!this::_artist.isInitialized)
            _artist = Artist(this, ARTIST_RADAR_EXCLUDE_ARTIST_ID_FK)

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
            _radarChannel = RadarChannel(this, ARTIST_RADAR_EXCLUDE_RADAR_CHANNEL_ID_FK)

        return _radarChannel;
    }

    val radarChannel: RadarChannel
        get(): RadarChannel = radarChannel()
    public override fun `as`(alias: String): ArtistRadarExclude = ArtistRadarExclude(DSL.name(alias), this)
    public override fun `as`(alias: Name): ArtistRadarExclude = ArtistRadarExclude(alias, this)
    public override fun `as`(alias: Table<*>): ArtistRadarExclude = ArtistRadarExclude(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): ArtistRadarExclude = ArtistRadarExclude(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): ArtistRadarExclude = ArtistRadarExclude(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): ArtistRadarExclude = ArtistRadarExclude(name.getQualifiedName(), null)

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
