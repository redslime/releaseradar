/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables.records


import java.time.LocalDateTime

import org.jooq.Record3
import org.jooq.impl.UpdatableRecordImpl

import xyz.redslime.releaseradar.db.releaseradar.tables.UserStat


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class UserStatRecord() : UpdatableRecordImpl<UserStatRecord>(UserStat.USER_STAT) {

    open var userId: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open var serverId: Long?
        set(value): Unit = set(1, value)
        get(): Long? = get(1) as Long?

    open var albumId: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var like: Boolean?
        set(value): Unit = set(3, value)
        get(): Boolean? = get(3) as Boolean?

    open var dislike: Boolean?
        set(value): Unit = set(4, value)
        get(): Boolean? = get(4) as Boolean?

    open var heart: Boolean?
        set(value): Unit = set(5, value)
        get(): Boolean? = get(5) as Boolean?

    open var clock: Boolean?
        set(value): Unit = set(6, value)
        get(): Boolean? = get(6) as Boolean?

    open var timestamp: LocalDateTime?
        set(value): Unit = set(7, value)
        get(): LocalDateTime? = get(7) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record3<Long?, Long?, String?> = super.key() as Record3<Long?, Long?, String?>

    /**
     * Create a detached, initialised UserStatRecord
     */
    constructor(userId: Long? = null, serverId: Long? = null, albumId: String? = null, like: Boolean? = null, dislike: Boolean? = null, heart: Boolean? = null, clock: Boolean? = null, timestamp: LocalDateTime? = null): this() {
        this.userId = userId
        this.serverId = serverId
        this.albumId = albumId
        this.like = like
        this.dislike = dislike
        this.heart = heart
        this.clock = clock
        this.timestamp = timestamp
        resetChangedOnNotNull()
    }
}
