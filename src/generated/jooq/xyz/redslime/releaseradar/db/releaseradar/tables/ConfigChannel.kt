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
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_CONFIG_CHANNEL_CONFIG_CHANNEL_SERVER_ID_UINDEX
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_CONFIG_CHANNEL_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ConfigChannelRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ConfigChannel(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, ConfigChannelRecord>?,
    aliased: Table<ConfigChannelRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<ConfigChannelRecord>(
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
         * The reference instance of <code>releaseradar.config_channel</code>
         */
        val CONFIG_CHANNEL: ConfigChannel = ConfigChannel()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<ConfigChannelRecord> = ConfigChannelRecord::class.java

    /**
     * The column <code>releaseradar.config_channel.server_id</code>.
     */
    val SERVER_ID: TableField<ConfigChannelRecord, Long?> = createField(DSL.name("server_id"), SQLDataType.BIGINT.nullable(false), this, "")

    /**
     * The column <code>releaseradar.config_channel.channel_id</code>.
     */
    val CHANNEL_ID: TableField<ConfigChannelRecord, Long?> = createField(DSL.name("channel_id"), SQLDataType.BIGINT.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<ConfigChannelRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<ConfigChannelRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.config_channel</code> table
     * reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.config_channel</code> table
     * reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.config_channel</code> table reference
     */
    constructor(): this(DSL.name("config_channel"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, ConfigChannelRecord>): this(Internal.createPathAlias(child, key), child, key, CONFIG_CHANNEL, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getPrimaryKey(): UniqueKey<ConfigChannelRecord> = KEY_CONFIG_CHANNEL_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<ConfigChannelRecord>> = listOf(KEY_CONFIG_CHANNEL_CONFIG_CHANNEL_SERVER_ID_UINDEX)
    public override fun `as`(alias: String): ConfigChannel = ConfigChannel(DSL.name(alias), this)
    public override fun `as`(alias: Name): ConfigChannel = ConfigChannel(alias, this)
    public override fun `as`(alias: Table<*>): ConfigChannel = ConfigChannel(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): ConfigChannel = ConfigChannel(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): ConfigChannel = ConfigChannel(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): ConfigChannel = ConfigChannel(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------
    public override fun fieldsRow(): Row2<Long?, Long?> = super.fieldsRow() as Row2<Long?, Long?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (Long?, Long?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (Long?, Long?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
