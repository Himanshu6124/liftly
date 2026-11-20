package com.himanshu.liftly.core.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire format for one row. [payload] is the entity's own columns as JSON (without sync
 * bookkeeping), so the backend can stay schema-agnostic or map it to its own tables.
 */
@Serializable
data class SyncRecord(
    val table: String,
    val id: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    /** Server-assigned version; null when the client pushes a row the server hasn't versioned yet. */
    val serverVersion: Long?,
    val payload: JsonObject,
)

@Serializable
data class PushAck(val id: String, val updatedAt: Long, val serverVersion: Long)

@Serializable
data class PullPage(val records: List<SyncRecord>, val nextCursor: String?, val hasMore: Boolean)

/**
 * The only thing a backend integration has to implement (e.g. with Ktor + REST, Supabase,
 * Firestore...). Everything else (change tracking, conflict resolution, cursors) is already local.
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
