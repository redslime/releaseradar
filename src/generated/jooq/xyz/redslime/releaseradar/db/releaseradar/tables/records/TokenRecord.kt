/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.tables.records


import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl

import xyz.redslime.releaseradar.db.releaseradar.tables.Token


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class TokenRecord() : UpdatableRecordImpl<TokenRecord>(Token.TOKEN) {

    open var id: String?
        set(value): Unit = set(0, value)
        get(): String? = get(0) as String?

    open var value: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<String?> = super.key() as Record1<String?>

    /**
     * Create a detached, initialised TokenRecord
     */
    constructor(id: String? = null, value: String? = null): this() {
        this.id = id
        this.value = value
        resetChangedOnNotNull()
    }
}
