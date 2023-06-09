/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables


import java.util.function.Function

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Identity
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row5
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
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_POST_LATER_POST_LATER_ID_UINDEX
import xyz.redslime.releaseradar.db.releaseradar.keys.KEY_POST_LATER_PRIMARY
import xyz.redslime.releaseradar.db.releaseradar.tables.records.PostLaterRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class PostLater(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, PostLaterRecord>?,
    aliased: Table<PostLaterRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<PostLaterRecord>(
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
         * The reference instance of <code>releaseradar.post_later</code>
         */
        val POST_LATER: PostLater = PostLater()
    }

    /**
     * The class holding records for this type
     */
    public override fun getRecordType(): Class<PostLaterRecord> = PostLaterRecord::class.java

    /**
     * The column <code>releaseradar.post_later.id</code>.
     */
    val ID: TableField<PostLaterRecord, Int?> = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "")

    /**
     * The column <code>releaseradar.post_later.content_id</code>.
     */
    val CONTENT_ID: TableField<PostLaterRecord, String?> = createField(DSL.name("content_id"), SQLDataType.VARCHAR(64).nullable(false), this, "")

    /**
     * The column <code>releaseradar.post_later.timezone</code>.
     */
    val TIMEZONE: TableField<PostLaterRecord, String?> = createField(DSL.name("timezone"), SQLDataType.VARCHAR(32).nullable(false), this, "")

    /**
     * The column <code>releaseradar.post_later.channel_id</code>.
     */
    val CHANNEL_ID: TableField<PostLaterRecord, Long?> = createField(DSL.name("channel_id"), SQLDataType.BIGINT.nullable(false), this, "")

    /**
     * The column <code>releaseradar.post_later.user_channel</code>.
     */
    val USER_CHANNEL: TableField<PostLaterRecord, Boolean?> = createField(DSL.name("user_channel"), SQLDataType.BIT.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<PostLaterRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<PostLaterRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>releaseradar.post_later</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>releaseradar.post_later</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>releaseradar.post_later</code> table reference
     */
    constructor(): this(DSL.name("post_later"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, PostLaterRecord>): this(Internal.createPathAlias(child, key), child, key, POST_LATER, null)
    public override fun getSchema(): Schema? = if (aliased()) null else Releaseradar.RELEASERADAR
    public override fun getIdentity(): Identity<PostLaterRecord, Int?> = super.getIdentity() as Identity<PostLaterRecord, Int?>
    public override fun getPrimaryKey(): UniqueKey<PostLaterRecord> = KEY_POST_LATER_PRIMARY
    public override fun getUniqueKeys(): List<UniqueKey<PostLaterRecord>> = listOf(KEY_POST_LATER_POST_LATER_ID_UINDEX)
    public override fun `as`(alias: String): PostLater = PostLater(DSL.name(alias), this)
    public override fun `as`(alias: Name): PostLater = PostLater(alias, this)
    public override fun `as`(alias: Table<*>): PostLater = PostLater(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    public override fun rename(name: String): PostLater = PostLater(DSL.name(name), null)

    /**
     * Rename this table
     */
    public override fun rename(name: Name): PostLater = PostLater(name, null)

    /**
     * Rename this table
     */
    public override fun rename(name: Table<*>): PostLater = PostLater(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------
    public override fun fieldsRow(): Row5<Int?, String?, String?, Long?, Boolean?> = super.fieldsRow() as Row5<Int?, String?, String?, Long?, Boolean?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (Int?, String?, String?, Long?, Boolean?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (Int?, String?, String?, Long?, Boolean?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
