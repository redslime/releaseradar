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
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_INFO_INFO_KEY_UINDEX
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_INFO_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.tables.records.InfoRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Info(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, InfoRecord>?,
    aliased: Table<InfoRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<InfoRecord>(
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
         * The reference instance of <code>releaseradar.info</code>
         */
        val INFO: Info = Info()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<InfoRecord> = InfoRecord::class.java

    /**
     * The column <code>releaseradar.info.key</code>.
     */
    val KEY: TableField<InfoRecord, String?> = createField(DSL.name("key"), SQLDataType.VARCHAR(32).nullable(false), this, "")

    /**
     * The column <code>releaseradar.info.value</code>.
     */
    val VALUE: TableField<InfoRecord, String?> = createField(DSL.name("value"), SQLDataType.VARCHAR(32), this, "")

    private constructor(alias: Name, aliased: Table<InfoRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<InfoRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.info</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.info</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.info</code> table reference
     */
    constructor(): this(DSL.name("info"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, InfoRecord>): this(Internal.createPathAlias(child, key), child, key, INFO, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getPrimaryKey(): UniqueKey<InfoRecord> = KEY_INFO_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<InfoRecord>> = listOf(KEY_INFO_INFO_KEY_UINDEX)
    public override fun `as`(alias: String): Info = Info(DSL.name(alias), this)
    public override fun `as`(alias: Name): Info = Info(alias, this)
    public override fun `as`(alias: Table<*>): Info = Info(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): Info = Info(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): Info = Info(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): Info = Info(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------
    public override fun fieldsRow(): Row2<String?, String?> = super.fieldsRow() as Row2<String?, String?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (String?, String?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (String?, String?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
