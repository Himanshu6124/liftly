package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import com.himanshu.liftly.core.domain.model.PersonalRecord
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutExercise
import com.himanshu.liftly.core.domain.model.WorkoutSet
import kotlin.time.Instant

internal fun Long.toInstant() = Instant.fromEpochMilliseconds(this)

internal fun WorkoutEntity.toDomain() = Workout(
    id = id,
    name = name,
    routineId = routineId,
    startedAt = startedAt.toInstant(),
    endedAt = endedAt?.toInstant(),
    notes = notes,
)

internal fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    supersetGroup = supersetGroup,
    status = status,
    loggedAt = loggedAt?.toInstant(),
    inputMode = inputMode,
    notes = notes,
)

internal fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    orderIndex = orderIndex,
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    distanceM = distanceM,
    rpe = rpe,
    setType = setType,
)

internal fun PersonalRecordEntity.toDomain() = PersonalRecord(
    id = id,
    exerciseId = exerciseId,
    type = type,
    value = value,
    workoutSetId = workoutSetId,
    achievedAt = achievedAt.toInstant(),
)

internal fun WorkoutExercise.toEntity(sync: SyncMeta) = WorkoutExerciseEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    supersetGroup = supersetGroup,
    status = status,
    loggedAt = loggedAt?.toEpochMilliseconds(),
    inputMode = inputMode,
    notes = notes,
    sync = sync,
)

internal fun WorkoutSet.toEntity(sync: SyncMeta) = WorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    orderIndex = orderIndex,
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    distanceM = distanceM,
    rpe = rpe,
    setType = setType,
    sync = sync,
)
