package com.himanshu.liftly.core.data.sync

import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncRecord

/**
 * The only thing a backend integration has to implement (e.g. with Ktor + REST, Supabase,
 * Firestore...). Everything else (change tracking, conflict resolution, cursors) is already local.
 * The wire types live in `core/protocol`, shared with the server.
 *
 * Contract:
 * - [push] stores the records (last-write-wins on [SyncRecord.updatedAt] is recommended) and
 *   returns one ack per accepted record with the new server version.
 * - [pull] returns records changed after [cursor] for [table] (including tombstones), oldest first.
 */
interface RemoteSyncDataSource {
    suspend fun push(table: String, records: List<SyncRecord>): List<PushAck>
    suspend fun pull(table: String, cursor: String?, limit: Int): PullPage
}
