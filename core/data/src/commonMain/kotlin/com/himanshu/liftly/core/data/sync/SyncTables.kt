package com.himanshu.liftly.core.data.sync

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.dao.SyncDao
import com.himanshu.liftly.core.data.db.entity.BodyMeasurementEntity
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.db.entity.SyncableEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Binds one Room table to the generic sync engine. */
class TableSync<E : SyncableEntity>(
    val name: String,
    val dao: SyncDao<E>,
    private val serializer: KSerializer<E>,
    /** Combines the local row (if any) with the pulled winner; default: the winner as is. */
    val merge: (local: E?, remote: E) -> E = { _, remote -> remote },
    private val withSync: (E, SyncMeta) -> E,
) {
    fun toRecord(entity: E): SyncRecord {
        val full = json.encodeToJsonElement(serializer, entity).jsonObject
        return SyncRecord(
            table = name,
            id = entity.id,
            updatedAt = entity.sync.updatedAt,
            deletedAt = entity.sync.deletedAt,
            serverVersion = entity.sync.serverVersion,
            payload = JsonObject(full - SYNC_KEY),
        )
    }

    fun fromRecord(record: SyncRecord): E {
        val meta = SyncMeta(
            updatedAt = record.updatedAt,
            deletedAt = record.deletedAt,
            syncStatus = SyncStatus.SYNCED,
            serverVersion = record.serverVersion,
        )
        val withMeta = JsonObject(record.payload + (SYNC_KEY to json.encodeToJsonElement(SyncMeta.serializer(), meta)))
        return withSync(json.decodeFromJsonElement(serializer, withMeta), meta)
    }

    private companion object {
        const val SYNC_KEY = "sync"
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/** Tables in [SyncProtocol.TABLES] order: parents are pushed and pulled before children (FK safety). */
class SyncTables(db: AppDatabase) {
    val all: List<TableSync<*>> = listOf(
        TableSync(SyncProtocol.EXERCISES, db.exerciseDao(), ExerciseEntity.serializer(), ::mergeExercise) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.ROUTINES, db.routineDao(), RoutineEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.ROUTINE_EXERCISES, db.routineExerciseDao(), RoutineExerciseEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.WORKOUTS, db.workoutDao(), WorkoutEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.WORKOUT_EXERCISES, db.workoutExerciseDao(), WorkoutExerciseEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.WORKOUT_SETS, db.workoutSetDao(), WorkoutSetEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.PERSONAL_RECORDS, db.personalRecordDao(), PersonalRecordEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync(SyncProtocol.BODY_MEASUREMENTS, db.bodyMeasurementDao(), BodyMeasurementEntity.serializer()) { e, m -> e.copy(sync = m) },
    )
}

/**
 * Library (seeded) exercises sync only what the user changed: favorite, note, archived, rest time.
 * The library text stays this device's, which may come from a newer seed than the other device's.
 */
internal fun mergeExercise(local: ExerciseEntity?, remote: ExerciseEntity): ExerciseEntity =
    if (local?.seedKey == null) {
        remote
    } else {
        local.copy(
            userNote = remote.userNote,
            isFavorite = remote.isFavorite,
            isArchived = remote.isArchived,
            restSeconds = remote.restSeconds,
            sync = remote.sync,
        )
    }
