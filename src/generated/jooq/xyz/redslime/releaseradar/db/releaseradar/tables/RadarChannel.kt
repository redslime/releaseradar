/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables


import java.util.function.Function

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Identity
import org.jooq.Index
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row6
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
import xyz.redslime.releaseradar.db.releaseradar.indexes.RADAR_CHANNEL_RADAR_CHANNEL_CHANNEL_ID_INDEX
import xyz.redslime.releaseradar.db.releaseradar.indexes.RADAR_CHANNEL_RADAR_CHANNEL_SERVER_ID_INDEX
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_RADAR_CHANNEL_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_RADAR_CHANNEL_RADAR_CHANNEL_ID_UINDEX
import xyz.redslime.releaseradar.db.releaseradar.tables.records.RadarChannelRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class RadarChannel(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, RadarChannelRecord>?,
    aliased: Table<RadarChannelRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<RadarChannelRecord>(
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
         * The reference instance of <code>releaseradar.radar_channel</code>
         */
        val RADAR_CHANNEL: RadarChannel = RadarChannel()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<RadarChannelRecord> = RadarChannelRecord::class.java

    /**
     * The column <code>releaseradar.radar_channel.id</code>.
     */
    val ID: TableField<RadarChannelRecord, Int?> = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "")

    /**
     * The column <code>releaseradar.radar_channel.server_id</code>.
     */
    val SERVER_ID: TableField<RadarChannelRecord, Long?> = createField(DSL.name("server_id"), SQLDataType.BIGINT.nullable(false), this, "")

    /**
     * The column <code>releaseradar.radar_channel.channel_id</code>.
     */
    val CHANNEL_ID: TableField<RadarChannelRecord, Long?> = createField(DSL.name("channel_id"), SQLDataType.BIGINT.nullable(false), this, "")

    /**
     * The column <code>releaseradar.radar_channel.timezone</code>.
     */
    val TIMEZONE: TableField<RadarChannelRecord, String?> = createField(DSL.name("timezone"), SQLDataType.VARCHAR(32).nullable(false).defaultValue(DSL.inline("ASAP", SQLDataType.VARCHAR)), this, "")

    /**
     * The column <code>releaseradar.radar_channel.embed_type</code>.
     */
    val EMBED_TYPE: TableField<RadarChannelRecord, String?> = createField(DSL.name("embed_type"), SQLDataType.VARCHAR(32).nullable(false).defaultValue(DSL.inline("CUSTOM", SQLDataType.VARCHAR)), this, "")

    /**
     * The column <code>releaseradar.radar_channel.emotes</code>.
     */
    val EMOTES: TableField<RadarChannelRecord, String?> = createField(DSL.name("emotes"), SQLDataType.VARCHAR(128), this, "")

    private constructor(alias: Name, aliased: Table<RadarChannelRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<RadarChannelRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.radar_channel</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.radar_channel</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.radar_channel</code> table reference
     */
    constructor(): this(DSL.name("radar_channel"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, RadarChannelRecord>): this(Internal.createPathAlias(child, key), child, key, RADAR_CHANNEL, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getIndexes(): List<Index> = listOf(RADAR_CHANNEL_RADAR_CHANNEL_CHANNEL_ID_INDEX, RADAR_CHANNEL_RADAR_CHANNEL_SERVER_ID_INDEX)
    public override fun getIdentity(): Identity<RadarChannelRecord, Int?> = super.getIdentity() as Identity<RadarChannelRecord, Int?>
    public override fun getPrimaryKey(): UniqueKey<RadarChannelRecord> = KEY_RADAR_CHANNEL_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<RadarChannelRecord>> = listOf(KEY_RADAR_CHANNEL_RADAR_CHANNEL_ID_UINDEX)
    public override fun `as`(alias: String): RadarChannel = RadarChannel(DSL.name(alias), this)
    public override fun `as`(alias: Name): RadarChannel = RadarChannel(alias, this)
    public override fun `as`(alias: Table<*>): RadarChannel = RadarChannel(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): RadarChannel = RadarChannel(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): RadarChannel = RadarChannel(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): RadarChannel = RadarChannel(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------
    public override fun fieldsRow(): Row6<Int?, Long?, Long?, String?, String?, String?> = super.fieldsRow() as Row6<Int?, Long?, Long?, String?, String?, String?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (Int?, Long?, Long?, String?, String?, String?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (Int?, Long?, Long?, String?, String?, String?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
