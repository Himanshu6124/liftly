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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Binds one Room table to the generic sync engine. */
class TableSync<E : SyncableEntity>(
    val name: String,
    val dao: SyncDao<E>,
    private val serializer: KSerializer<E>,
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

/** Tables in dependency order: parents are pushed and pulled before children (FK safety). */
class SyncTables(db: AppDatabase) {
    val all: List<TableSync<*>> = listOf(
        TableSync("exercises", db.exerciseDao(), ExerciseEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("routines", db.routineDao(), RoutineEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("routine_exercises", db.routineExerciseDao(), RoutineExerciseEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("workouts", db.workoutDao(), WorkoutEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("workout_exercises", db.workoutExerciseDao(), WorkoutExerciseEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("workout_sets", db.workoutSetDao(), WorkoutSetEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("personal_records", db.personalRecordDao(), PersonalRecordEntity.serializer()) { e, m -> e.copy(sync = m) },
        TableSync("body_measurements", db.bodyMeasurementDao(), BodyMeasurementEntity.serializer()) { e, m -> e.copy(sync = m) },
    )
}
