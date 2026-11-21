package com.himanshu.liftly.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire format for one row. [payload] is the entity's own columns as JSON (without sync
 * bookkeeping), so the backend can stay schema-agnostic or map it to its own tables.
 *
 * Field names are part of the API: renaming one breaks installed apps. `SyncProtocolTest` pins them.
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

/** Synced table names (the `table` in [SyncRecord] and in sync URLs) and request limits. */
object SyncProtocol {
    const val EXERCISES = "exercises"
    const val ROUTINES = "routines"
    const val ROUTINE_EXERCISES = "routine_exercises"
    const val WORKOUTS = "workouts"
    const val WORKOUT_EXERCISES = "workout_exercises"
    const val WORKOUT_SETS = "workout_sets"
    const val PERSONAL_RECORDS = "personal_records"
    const val BODY_MEASUREMENTS = "body_measurements"

    /** Dependency order: parents before children. The server accepts only these names. */
    val TABLES: List<String> = listOf(
        EXERCISES,
        ROUTINES,
        ROUTINE_EXERCISES,
        WORKOUTS,
        WORKOUT_EXERCISES,
        WORKOUT_SETS,
        PERSONAL_RECORDS,
        BODY_MEASUREMENTS,
    )

    /** Largest push batch and pull page the server accepts; the client never sends more. */
    const val MAX_BATCH = 500
}
