/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables.records


import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record5
import org.jooq.Row5
import org.jooq.impl.UpdatableRecordImpl

import xyz.redslime.releaseradar.db.releaseradar.tables.PostLater


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class PostLaterRecord() : UpdatableRecordImpl<PostLaterRecord>(PostLater.POST_LATER), Record5<Int?, String?, String?, Long?, Boolean?> {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var contentId: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var timezone: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var channelId: Long?
        set(value): Unit = set(3, value)
        get(): Long? = get(3) as Long?

    open var userChannel: Boolean?
        set(value): Unit = set(4, value)
        get(): Boolean? = get(4) as Boolean?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    public override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    public override fun fieldsRow(): Row5<Int?, String?, String?, Long?, Boolean?> = super.fieldsRow() as Row5<Int?, String?, String?, Long?, Boolean?>
    public override fun valuesRow(): Row5<Int?, String?, String?, Long?, Boolean?> = super.valuesRow() as Row5<Int?, String?, String?, Long?, Boolean?>
    public override fun field1(): Field<Int?> = PostLater.POST_LATER.ID
    public override fun field2(): Field<String?> = PostLater.POST_LATER.CONTENT_ID
    public override fun field3(): Field<String?> = PostLater.POST_LATER.TIMEZONE
    public override fun field4(): Field<Long?> = PostLater.POST_LATER.CHANNEL_ID
    public override fun field5(): Field<Boolean?> = PostLater.POST_LATER.USER_CHANNEL
    public override fun component1(): Int? = id
    public override fun component2(): String? = contentId
    public override fun component3(): String? = timezone
    public override fun component4(): Long? = channelId
    public override fun component5(): Boolean? = userChannel
    public override fun value1(): Int? = id
    public override fun value2(): String? = contentId
    public override fun value3(): String? = timezone
    public override fun value4(): Long? = channelId
    public override fun value5(): Boolean? = userChannel

    public override fun value1(value: Int?): PostLaterRecord {
        set(0, value)
        return this
    }

    public override fun value2(value: String?): PostLaterRecord {
        set(1, value)
        return this
    }

    public override fun value3(value: String?): PostLaterRecord {
        set(2, value)
        return this
    }

    public override fun value4(value: Long?): PostLaterRecord {
        set(3, value)
        return this
    }

    public override fun value5(value: Boolean?): PostLaterRecord {
        set(4, value)
        return this
    }

    public override fun values(value1: Int?, value2: String?, value3: String?, value4: Long?, value5: Boolean?): PostLaterRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        return this
    }

    /**
     * Create a detached, initialised PostLaterRecord
     */
    constructor(id: Int? = null, contentId: String? = null, timezone: String? = null, channelId: Long? = null, userChannel: Boolean? = null): this() {
        this.id = id
        this.contentId = contentId
        this.timezone = timezone
        this.channelId = channelId
        this.userChannel = userChannel
        resetChangedOnNotNull()
    }
}
