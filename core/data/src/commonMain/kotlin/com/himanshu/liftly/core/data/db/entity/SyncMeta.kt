package com.himanshu.liftly.core.data.db.entity

import kotlinx.serialization.Serializable

enum class SyncStatus {
    /** Local row equals the server copy (or was received from it). */
    SYNCED,

    /** Local change not yet acknowledged by the server. Every local write sets this. */
    PENDING,
}

/**
 * Bookkeeping columns embedded in every user-data table so the local DB can be reconciled
 * with a backend later without a schema rewrite.
 *
 * - [updatedAt]: client wall-clock millis of the last write; used for last-write-wins.
 * - [deletedAt]: soft-delete tombstone; rows the server has seen are tombstoned so the
 *   deletion can be pushed, rows the server has never seen are hard-deleted.
 * - [serverVersion]: opaque monotonically increasing version assigned by the server; null = never synced.
 */
@Serializable
data class SyncMeta(
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val serverVersion: Long? = null,
) {
    companion object {
        fun dirty(now: Long) = SyncMeta(updatedAt = now)
    }
}

/** Implemented by every entity that participates in sync. */
interface SyncableEntity {
    val id: String
    val sync: SyncMeta
}

fun SyncMeta.touch(now: Long) = copy(updatedAt = now, syncStatus = SyncStatus.PENDING)
