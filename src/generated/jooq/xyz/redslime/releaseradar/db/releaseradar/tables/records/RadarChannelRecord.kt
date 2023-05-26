/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables.records


import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record6
import org.jooq.Row6
import org.jooq.impl.UpdatableRecordImpl

import xyz.redslime.releaseradar.db.releaseradar.tables.RadarChannel


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class RadarChannelRecord() : UpdatableRecordImpl<RadarChannelRecord>(RadarChannel.RADAR_CHANNEL), Record6<Int?, Long?, Long?, String?, String?, String?> {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var serverId: Long?
        set(value): Unit = set(1, value)
        get(): Long? = get(1) as Long?

    open var channelId: Long?
        set(value): Unit = set(2, value)
        get(): Long? = get(2) as Long?

    open var timezone: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var embedType: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    open var emotes: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    public override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    public override fun fieldsRow(): Row6<Int?, Long?, Long?, String?, String?, String?> = super.fieldsRow() as Row6<Int?, Long?, Long?, String?, String?, String?>
    public override fun valuesRow(): Row6<Int?, Long?, Long?, String?, String?, String?> = super.valuesRow() as Row6<Int?, Long?, Long?, String?, String?, String?>
    public override fun field1(): Field<Int?> = RadarChannel.RADAR_CHANNEL.ID
    public override fun field2(): Field<Long?> = RadarChannel.RADAR_CHANNEL.SERVER_ID
    public override fun field3(): Field<Long?> = RadarChannel.RADAR_CHANNEL.CHANNEL_ID
    public override fun field4(): Field<String?> = RadarChannel.RADAR_CHANNEL.TIMEZONE
    public override fun field5(): Field<String?> = RadarChannel.RADAR_CHANNEL.EMBED_TYPE
    public override fun field6(): Field<String?> = RadarChannel.RADAR_CHANNEL.EMOTES
    public override fun component1(): Int? = id
    public override fun component2(): Long? = serverId
    public override fun component3(): Long? = channelId
    public override fun component4(): String? = timezone
    public override fun component5(): String? = embedType
    public override fun component6(): String? = emotes
    public override fun value1(): Int? = id
    public override fun value2(): Long? = serverId
    public override fun value3(): Long? = channelId
    public override fun value4(): String? = timezone
    public override fun value5(): String? = embedType
    public override fun value6(): String? = emotes

    public override fun value1(value: Int?): RadarChannelRecord {
        set(0, value)
        return this
    }

    public override fun value2(value: Long?): RadarChannelRecord {
        set(1, value)
        return this
    }

    public override fun value3(value: Long?): RadarChannelRecord {
        set(2, value)
        return this
    }

    public override fun value4(value: String?): RadarChannelRecord {
        set(3, value)
        return this
    }

    public override fun value5(value: String?): RadarChannelRecord {
        set(4, value)
        return this
    }

    public override fun value6(value: String?): RadarChannelRecord {
        set(5, value)
        return this
    }

    public override fun values(value1: Int?, value2: Long?, value3: Long?, value4: String?, value5: String?, value6: String?): RadarChannelRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        return this
    }

    /**
     * Create a detached, initialised RadarChannelRecord
     */
    constructor(id: Int? = null, serverId: Long? = null, channelId: Long? = null, timezone: String? = null, embedType: String? = null, emotes: String? = null): this() {
        this.id = id
        this.serverId = serverId
        this.channelId = channelId
        this.timezone = timezone
        this.embedType = embedType
        this.emotes = emotes
        resetChangedOnNotNull()
    }
}
