package com.himanshu.liftly.core.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.himanshu.liftly.core.domain.model.Difficulty
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Mechanic
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.TrackingType
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "exercises",
    indices = [Index("name"), Index(value = ["seedKey"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey override val id: String,
    val seedKey: String?,
    val name: String,
    val aliases: List<String>,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val mechanic: Mechanic?,
    val difficulty: Difficulty?,
    val imagePaths: List<String>,
    val videoUrl: String?,
    val instructions: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>,
    val userNote: String?,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val isArchived: Boolean,
    @Embedded override val sync: SyncMeta,
    /** Rest time the user last picked for this exercise (seconds); added in schema v2. */
    val restSeconds: Int? = null,
) : SyncableEntity

@Serializable
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val notes: String?,
    val orderIndex: Int,
    val lastPerformedAt: Long?,
    val createdAt: Long,
    @Embedded override val sync: SyncMeta,
) : SyncableEntity

@Serializable
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"]),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey override val id: String,
    val routineId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val supersetGroup: Int?,
    val targetSets: Int,
    val targetReps: Int?,
    val targetWeightKg: Double?,
    @Embedded override val sync: SyncMeta,
) : SyncableEntity

@Serializable
@Entity(tableName = "workouts", indices = [Index("startedAt"), Index("endedAt")])
data class WorkoutEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val routineId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val notes: String?,
    @Embedded override val sync: SyncMeta,
    /** When the running rest timer ends (epoch millis), null when none; added in schema v2. */
    val restEndsAt: Long? = null,
) : SyncableEntity

@Serializable
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(WorkoutEntity::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"]),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey override val id: String,
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val supersetGroup: Int?,
    val status: ExerciseStatus,
    val loggedAt: Long?,
    val inputMode: InputMode,
    val notes: String?,
    @Embedded override val sync: SyncMeta,
) : SyncableEntity

@Serializable
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(WorkoutExerciseEntity::class, ["id"], ["workoutExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey override val id: String,
    val workoutExerciseId: String,
    val orderIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rpe: Double?,
    val setType: SetType,
    @Embedded override val sync: SyncMeta,
) : SyncableEntity

@Serializable
@Entity(
    tableName = "personal_records",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"])],
    indices = [Index("exerciseId"), Index("workoutSetId")],
)
data class PersonalRecordEntity(
    @PrimaryKey override val id: String,
    val exerciseId: String,
    val type: PrType,
    val value: Double,
    val workoutSetId: String,
    val achievedAt: Long,
    @Embedded override val sync: SyncMeta,
) : SyncableEntity

/** Per-table pull cursor returned by the server (opaque string). */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey val tableName: String,
    val cursor: String,
)
