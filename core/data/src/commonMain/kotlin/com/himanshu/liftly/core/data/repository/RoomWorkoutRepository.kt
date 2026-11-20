package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncableEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.data.db.writeTransaction
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ActiveWorkoutExercise
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LastSession
import com.himanshu.liftly.core.domain.model.LogSheetData
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.RoutineTarget
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class RoomWorkoutRepository(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val sync: SyncController,
    private val sql: SyncSql,
) : WorkoutRepository {
    private val workouts = db.workoutDao()
    private val workoutExercises = db.workoutExerciseDao()
    private val sets = db.workoutSetDao()

    private fun now() = time.now().toEpochMilliseconds()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveWorkout(): Flow<ActiveWorkout?> = workouts.observeInProgress()
        .distinctUntilChanged()
        .flatMapLatest { workout ->
            if (workout == null) {
                flowOf(null)
            } else {
                combine(workoutExercises.observeWithExercise(workout.id), sets.observeForWorkout(workout.id)) { rows, allSets ->
                    val setsByExercise = allSets.groupBy { it.workoutExerciseId }
                    ActiveWorkout(
                        workout = workout.toDomain(),
                        exercises = rows.mapNotNull { row ->
                            val exercise = row.exercise ?: return@mapNotNull null
                            ActiveWorkoutExercise(
                                workoutExercise = row.workoutExercise.toDomain(),
                                exercise = exercise.toDomain(),
                                sets = setsByExercise[row.workoutExercise.id].orEmpty().map { it.toDomain() },
                            )
                        },
                    )
                }
            }
        }
        .distinctUntilChanged()

    override suspend fun getInProgressWorkout(): Workout? = workouts.getInProgress()?.toDomain()

    override suspend fun createWorkout(name: String, routineId: EntityId?, exerciseIds: List<EntityId>): EntityId = write {
        val id = ids.newId()
        val now = now()
        workouts.upsertAll(
            listOf(WorkoutEntity(id, name, routineId, startedAt = now, endedAt = null, notes = null, sync = SyncMeta.dirty(now))),
        )
        insertExercises(id, exerciseIds, startIndex = 0, now = now)
        id
    }

    override suspend fun addExercises(workoutId: EntityId, exerciseIds: List<EntityId>) = write {
        insertExercises(workoutId, exerciseIds, startIndex = workoutExercises.maxOrderIndex(workoutId) + 1, now = now())
    }

    private suspend fun insertExercises(workoutId: EntityId, exerciseIds: List<EntityId>, startIndex: Int, now: Long) {
        if (exerciseIds.isEmpty()) return
        workoutExercises.upsertAll(
            exerciseIds.mapIndexed { i, exerciseId ->
                WorkoutExerciseEntity(
                    id = ids.newId(),
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    orderIndex = startIndex + i,
                    supersetGroup = null,
                    status = ExerciseStatus.PENDING,
                    loggedAt = null,
                    inputMode = InputMode.UNIFORM,
                    notes = null,
                    sync = SyncMeta.dirty(now),
                )
            },
        )
    }

    override suspend fun rename(workoutId: EntityId, name: String) = write {
        val w = workouts.getById(workoutId) ?: return@write
        val trimmed = name.trim()
        if (w.name != trimmed) workouts.upsertAll(listOf(w.copy(name = trimmed, sync = w.sync.touch(now()))))
    }

    override suspend fun reorder(workoutId: EntityId, orderedWorkoutExerciseIds: List<EntityId>) = write {
        val now = now()
        val position = orderedWorkoutExerciseIds.withIndex().associate { (i, id) -> id to i }
        val changed = workoutExercises.getForWorkout(workoutId).mapNotNull { we ->
            val index = position[we.id] ?: return@mapNotNull null
            if (we.orderIndex == index) null else we.copy(orderIndex = index, sync = we.sync.touch(now))
        }
        workoutExercises.upsertAll(changed)
    }

    override suspend fun setStatus(workoutExerciseId: EntityId, status: ExerciseStatus) = write {
        val we = workoutExercises.getById(workoutExerciseId) ?: return@write
        if (we.status != status) workoutExercises.upsertAll(listOf(we.copy(status = status, sync = we.sync.touch(now()))))
    }

    override suspend fun removeExercise(workoutExerciseId: EntityId): RemovedWorkoutExercise? = write {
        val we = workoutExercises.getById(workoutExerciseId) ?: return@write null
        val weSets = sets.getForWorkoutExercise(workoutExerciseId)
        val now = now()
        sql.delete("workout_sets", weSets.map { it.id }, now)
        sql.delete("workout_exercises", listOf(we.id), now)
        RemovedWorkoutExercise(we.toDomain(), weSets.map { it.toDomain() })
    }

    override suspend fun restoreExercise(removed: RemovedWorkoutExercise) = write {
        val now = now()
        val we = removed.workoutExercise
        val tombstone = workoutExercises.getByIdsIncludingDeleted(listOf(we.id)).firstOrNull()
        workoutExercises.upsertAll(listOf(we.toEntity(revive(tombstone, now))))
        val setTombstones = sets.getByIdsIncludingDeleted(removed.sets.map { it.id }).associateBy { it.id }
        sets.upsertAll(removed.sets.map { it.toEntity(revive(setTombstones[it.id], now)) })
    }

    /** Un-deletes a row: keeps its server version (if the server knew it) so the revival syncs as an update. */
    private fun revive(existing: SyncableEntity?, now: Long): SyncMeta =
        existing?.sync?.copy(deletedAt = null)?.touch(now) ?: SyncMeta.dirty(now)

    override suspend fun finish(workoutId: EntityId) = write {
        val now = now()
        val w = workouts.getById(workoutId) ?: return@write
        val skipped = workoutExercises.getForWorkout(workoutId)
            .filter { it.status == ExerciseStatus.PENDING }
            .map { it.copy(status = ExerciseStatus.SKIPPED, sync = it.sync.touch(now)) }
        workoutExercises.upsertAll(skipped)
        workouts.upsertAll(listOf(w.copy(endedAt = now, sync = w.sync.touch(now))))
        // Drives the "next routine" rotation on Home.
        val routine = w.routineId?.let { db.routineDao().getById(it) }
        if (routine != null) {
            db.routineDao().upsertAll(listOf(routine.copy(lastPerformedAt = w.startedAt, sync = routine.sync.touch(now))))
        }
    }

    override suspend fun discard(workoutId: EntityId) = write {
        val now = now()
        val weIds = workoutExercises.idsForWorkout(workoutId)
        sql.delete("workout_sets", sets.idsForWorkoutExercises(weIds), now)
        sql.delete("workout_exercises", weIds, now)
        sql.delete("workouts", listOf(workoutId), now)
    }

    override suspend fun getLogSheetData(workoutExerciseId: EntityId): LogSheetData? {
        val we = workoutExercises.getById(workoutExerciseId) ?: return null
        val exercise = db.exerciseDao().getByIdsIncludingDeleted(listOf(we.exerciseId)).firstOrNull() ?: return null
        val workout = workouts.getById(we.workoutId)
        val target = workout?.routineId?.let { db.routineExerciseDao().getTarget(it, we.exerciseId) }
        return LogSheetData(
            workoutExercise = we.toDomain(),
            exercise = exercise.toDomain(),
            currentSets = sets.getForWorkoutExercise(we.id).map { it.toDomain() },
            lastSession = getLastSession(we.exerciseId, we.workoutId),
            routineTarget = target?.let { RoutineTarget(it.targetSets, it.targetReps, it.targetWeightKg) },
        )
    }

    override suspend fun getLastSession(exerciseId: EntityId, excludeWorkoutId: EntityId): LastSession? {
        val we = workoutExercises.getLastLogged(exerciseId, excludeWorkoutId) ?: return null
        val workout = workouts.getById(we.workoutId) ?: return null
        return LastSession(
            workoutId = workout.id,
            performedAt = workout.startedAt.toInstant(),
            inputMode = we.inputMode,
            sets = sets.getForWorkoutExercise(we.id).map { it.toDomain() },
        )
    }

    override suspend fun logSets(workoutExerciseId: EntityId, sets: List<DraftSet>, inputMode: InputMode): LogSnapshot = write {
        val now = now()
        val we = requireNotNull(workoutExercises.getById(workoutExerciseId)) { "Workout exercise $workoutExerciseId not found" }
        val previousSets = this.sets.getForWorkoutExercise(we.id)
        val snapshot = LogSnapshot(we.id, we.status, we.loggedAt?.toInstant(), we.inputMode, previousSets.map { it.toDomain() })

        this.sets.let { dao ->
            sql.delete("workout_sets", previousSets.map { it.id }, now)
            dao.upsertAll(
                sets.mapIndexed { i, s ->
                    WorkoutSetEntity(
                        id = ids.newId(),
                        workoutExerciseId = we.id,
                        orderIndex = i,
                        weightKg = s.weightKg,
                        reps = s.reps,
                        durationSec = s.durationSec,
                        distanceM = s.distanceM,
                        rpe = s.rpe,
                        setType = s.setType,
                        sync = SyncMeta.dirty(now),
                    )
                },
            )
        }
        workoutExercises.upsertAll(
            listOf(we.copy(status = ExerciseStatus.LOGGED, loggedAt = now, inputMode = inputMode, sync = we.sync.touch(now))),
        )
        snapshot
    }

    override suspend fun restoreLog(snapshot: LogSnapshot) = write {
        val now = now()
        val we = workoutExercises.getById(snapshot.workoutExerciseId) ?: return@write
        sql.delete("workout_sets", sets.getForWorkoutExercise(we.id).map { it.id }, now)
        val tombstones = sets.getByIdsIncludingDeleted(snapshot.sets.map { it.id }).associateBy { it.id }
        sets.upsertAll(snapshot.sets.map { it.toEntity(revive(tombstones[it.id], now)) })
        workoutExercises.upsertAll(
            listOf(
                we.copy(
                    status = snapshot.status,
                    loggedAt = snapshot.loggedAt?.toEpochMilliseconds(),
                    inputMode = snapshot.inputMode,
                    sync = we.sync.touch(now),
                ),
            ),
        )
    }

    override fun observeRecentExerciseIds(limit: Int): Flow<List<EntityId>> =
        workoutExercises.observeRecentExerciseIds(limit).distinctUntilChanged()

    /** One transaction per user action, then ask for a (debounced) sync. */
    private suspend fun <T> write(block: suspend () -> T): T = db.writeTransaction(block).also { sync.requestSync() }
}
