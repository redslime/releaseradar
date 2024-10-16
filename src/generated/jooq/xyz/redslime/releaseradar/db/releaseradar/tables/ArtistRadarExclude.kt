/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables


import kotlin.collections.Collection
import kotlin.collections.List

import org.jooq.Condition
import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.InverseForeignKey
import org.jooq.Name
import org.jooq.Path
import org.jooq.PlainSQL
import org.jooq.QueryPart
import org.jooq.Record
import org.jooq.SQL
import org.jooq.Schema
import org.jooq.Select
import org.jooq.Stringly
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
import xyz.redslime.releaseradar.db.releaseradar.tables.Artist.ArtistPath
import xyz.redslime.releaseradar.db.releaseradar.tables.RadarChannel.RadarChannelPath
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarExcludeRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ArtistRadarExclude(
    alias: Name,
    path: Table<out Record>?,
    childPath: ForeignKey<out Record, ArtistRadarExcludeRecord>?,
    parentPath: InverseForeignKey<out Record, ArtistRadarExcludeRecord>?,
    aliased: Table<ArtistRadarExcludeRecord>?,
    parameters: Array<Field<*>?>?,
    where: Condition?
): TableImpl<ArtistRadarExcludeRecord>(
    alias,
    Releaseradar.RELEASERADAR,
    path,
    childPath,
    parentPath,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table(),
    where,
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
    override fun getRecordType(): Class<ArtistRadarExcludeRecord> = ArtistRadarExcludeRecord::class.java

    /**
     * The column <code>releaseradar.artist_radar_exclude.artist_id</code>.
     */
    val ARTIST_ID: TableField<ArtistRadarExcludeRecord, String?> = createField(DSL.name("artist_id"), SQLDataType.VARCHAR(32).nullable(false), this, "")

    /**
     * The column <code>releaseradar.artist_radar_exclude.radar_id</code>.
     */
    val RADAR_ID: TableField<ArtistRadarExcludeRecord, Int?> = createField(DSL.name("radar_id"), SQLDataType.INTEGER.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>?): this(alias, null, null, null, aliased, null, null)
    private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, null, aliased, parameters, null)
    private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>?, where: Condition): this(alias, null, null, null, aliased, null, where)

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

    constructor(path: Table<out Record>, childPath: ForeignKey<out Record, ArtistRadarExcludeRecord>?, parentPath: InverseForeignKey<out Record, ArtistRadarExcludeRecord>?): this(Internal.createPathAlias(path, childPath, parentPath), path, childPath, parentPath, ARTIST_RADAR_EXCLUDE, null, null)

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    open class ArtistRadarExcludePath : ArtistRadarExclude, Path<ArtistRadarExcludeRecord> {
        constructor(path: Table<out Record>, childPath: ForeignKey<out Record, ArtistRadarExcludeRecord>?, parentPath: InverseForeignKey<out Record, ArtistRadarExcludeRecord>?): super(path, childPath, parentPath)
        private constructor(alias: Name, aliased: Table<ArtistRadarExcludeRecord>): super(alias, aliased)
        override fun `as`(alias: String): ArtistRadarExcludePath = ArtistRadarExcludePath(DSL.name(alias), this)
        override fun `as`(alias: Name): ArtistRadarExcludePath = ArtistRadarExcludePath(alias, this)
        override fun `as`(alias: Table<*>): ArtistRadarExcludePath = ArtistRadarExcludePath(alias.qualifiedName, this)
    }
    override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    override fun getPrimaryKey(): UniqueKey<ArtistRadarExcludeRecord> = KEY_ARTIST_RADAR_EXCLUDE_PRIMARY
    override fun getUniqueKeys(): List<UniqueKey<ArtistRadarExcludeRecord>> = listOf(KEY_ARTIST_RADAR_EXCLUDE_ARTIST_RADAR_EXCLUDE_PK)
    override fun getReferences(): List<ForeignKey<ArtistRadarExcludeRecord, *>> = listOf(ARTIST_RADAR_EXCLUDE_ARTIST_ID_FK, ARTIST_RADAR_EXCLUDE_RADAR_CHANNEL_ID_FK)

    private lateinit var _artist: ArtistPath

    /**
     * Get the implicit join path to the <code>releaseradar.artist</code> table.
     */
    fun artist(): ArtistPath {
        if (!this::_artist.isInitialized)
            _artist = ArtistPath(this, ARTIST_RADAR_EXCLUDE_ARTIST_ID_FK, null)

        return _artist;
    }

    val artist: ArtistPath
        get(): ArtistPath = artist()

    private lateinit var _radarChannel: RadarChannelPath

    /**
     * Get the implicit join path to the <code>releaseradar.radar_channel</code>
     * table.
     */
    fun radarChannel(): RadarChannelPath {
        if (!this::_radarChannel.isInitialized)
            _radarChannel = RadarChannelPath(this, ARTIST_RADAR_EXCLUDE_RADAR_CHANNEL_ID_FK, null)

        return _radarChannel;
    }

    val radarChannel: RadarChannelPath
        get(): RadarChannelPath = radarChannel()
    override fun `as`(alias: String): ArtistRadarExclude = ArtistRadarExclude(DSL.name(alias), this)
    override fun `as`(alias: Name): ArtistRadarExclude = ArtistRadarExclude(alias, this)
    override fun `as`(alias: Table<*>): ArtistRadarExclude = ArtistRadarExclude(alias.qualifiedName, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): ArtistRadarExclude = ArtistRadarExclude(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): ArtistRadarExclude = ArtistRadarExclude(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): ArtistRadarExclude = ArtistRadarExclude(name.qualifiedName, null)

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Condition): ArtistRadarExclude = ArtistRadarExclude(qualifiedName, if (aliased()) this else null, condition)

    /**
     * Create an inline derived table from this table
     */
    override fun where(conditions: Collection<Condition>): ArtistRadarExclude = where(DSL.and(conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(vararg conditions: Condition): ArtistRadarExclude = where(DSL.and(*conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Field<Boolean?>): ArtistRadarExclude = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(condition: SQL): ArtistRadarExclude = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String): ArtistRadarExclude = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg binds: Any?): ArtistRadarExclude = where(DSL.condition(condition, *binds))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg parts: QueryPart): ArtistRadarExclude = where(DSL.condition(condition, *parts))

    /**
     * Create an inline derived table from this table
     */
    override fun whereExists(select: Select<*>): ArtistRadarExclude = where(DSL.exists(select))

    /**
     * Create an inline derived table from this table
     */
    override fun whereNotExists(select: Select<*>): ArtistRadarExclude = where(DSL.notExists(select))
}
