package com.himanshu.liftly.core.data.db.dao

import androidx.room.Upsert

/**
 * Operations every syncable table exposes to the sync engine. Concrete DAOs implement the
 * queries with their own table name; generic bookkeeping SQL lives in [com.himanshu.liftly.core.data.sync.SyncSql].
 */
interface SyncDao<E> {
    @Upsert
    suspend fun upsertAll(items: List<E>)

    /** Rows with local changes the server hasn't acknowledged, including tombstones. */
    suspend fun pendingSync(): List<E>

    /** Lookup by id, including tombstoned rows. */
    suspend fun getByIdsIncludingDeleted(ids: List<String>): List<E>
}
