package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.SetWithDate
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
import com.himanshu.liftly.core.domain.model.DeletedWorkout
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LastSession
import com.himanshu.liftly.core.domain.model.LogSheetData
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.model.PersonalRecord
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.RoutineTarget
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.model.WorkoutSummaryLine
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.usecase.PrHit
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.Strength
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.toDraftSets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
    private val records = db.personalRecordDao()

    private fun now() = time.now().toEpochMilliseconds()

    override fun observeActiveWorkout(): Flow<ActiveWorkout?> = assemble(workouts.observeInProgress())

    override fun observeWorkout(workoutId: EntityId): Flow<ActiveWorkout?> = assemble(workouts.observeById(workoutId))

    /** Joins a workout with its exercises, sets and records; re-emits on any change to them. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun assemble(workoutFlow: Flow<WorkoutEntity?>): Flow<ActiveWorkout?> = workoutFlow
        .distinctUntilChanged()
        .flatMapLatest { workout ->
            if (workout == null) {
                flowOf(null)
            } else {
                combine(
                    workoutExercises.observeWithExercise(workout.id),
                    sets.observeForWorkout(workout.id),
                    records.observeForWorkout(workout.id),
                ) { rows, allSets, prs ->
                    val setsByExercise = allSets.groupBy { it.workoutExerciseId }
                    val prsByExercise = prs.groupBy({ it.workoutExerciseId }, { it.record.toDomain() })
                    ActiveWorkout(
                        workout = workout.toDomain(),
                        exercises = rows.mapNotNull { row ->
                            val exercise = row.exercise ?: return@mapNotNull null
                            ActiveWorkoutExercise(
                                workoutExercise = row.workoutExercise.toDomain(),
                                exercise = exercise.toDomain(),
                                sets = setsByExercise[row.workoutExercise.id].orEmpty().map { it.toDomain() },
                                personalRecords = prsByExercise[row.workoutExercise.id].orEmpty(),
                            )
                        },
                    )
                }
            }
        }
        .distinctUntilChanged()

    override fun observeHistory(): Flow<List<WorkoutHistoryItem>> = workouts.observeHistory().map { rows ->
        rows.map { r ->
            WorkoutHistoryItem(r.workout.toDomain(), r.exerciseCount, r.loggedCount, r.workingSets, r.volumeKg, r.prCount)
        }
    }.distinctUntilChanged()

    override suspend fun deleteWorkout(workoutId: EntityId): DeletedWorkout? = write {
        val w = workouts.getById(workoutId) ?: return@write null
        val wes = workoutExercises.getForWorkout(workoutId)
        val allSets = sets.getForWorkoutExercises(wes.map { it.id })
        val prs = records.getForSets(allSets.map { it.id })
        val now = now()
        sql.delete("personal_records", prs.map { it.id }, now)
        sql.delete("workout_sets", allSets.map { it.id }, now)
        sql.delete("workout_exercises", wes.map { it.id }, now)
        sql.delete("workouts", listOf(workoutId), now)
        DeletedWorkout(w.toDomain(), wes.map { it.toDomain() }, allSets.map { it.toDomain() }, prs.map { it.toDomain() })
    }

    override suspend fun restoreWorkout(deleted: DeletedWorkout) = write {
        val now = now()
        val w = deleted.workout
        val oldW = workouts.getByIdsIncludingDeleted(listOf(w.id)).firstOrNull()
        workouts.upsertAll(
            listOf(
                WorkoutEntity(
                    w.id,
                    w.name,
                    w.routineId,
                    w.startedAt.toEpochMilliseconds(),
                    w.endedAt?.toEpochMilliseconds(),
                    w.notes,
                    revive(oldW, now),
                ),
            ),
        )
        val oldWe = workoutExercises.getByIdsIncludingDeleted(deleted.exercises.map { it.id }).associateBy { it.id }
        workoutExercises.upsertAll(deleted.exercises.map { it.toEntity(revive(oldWe[it.id], now)) })
        val oldSets = sets.getByIdsIncludingDeleted(deleted.sets.map { it.id }).associateBy { it.id }
        sets.upsertAll(deleted.sets.map { it.toEntity(revive(oldSets[it.id], now)) })
        val oldPrs = records.getByIdsIncludingDeleted(deleted.personalRecords.map { it.id }).associateBy { it.id }
        records.upsertAll(
            deleted.personalRecords.map { pr ->
                PersonalRecordEntity(
                    pr.id,
                    pr.exerciseId,
                    pr.type,
                    pr.value,
                    pr.workoutSetId,
                    pr.achievedAt.toEpochMilliseconds(),
                    revive(oldPrs[pr.id], now),
                )
            },
        )
    }

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
        sql.delete("personal_records", records.getForSets(weSets.map { it.id }).map { it.id }, now)
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
        val setIds = sets.idsForWorkoutExercises(weIds)
        sql.delete("personal_records", records.getForSets(setIds).map { it.id }, now)
        sql.delete("workout_sets", setIds, now)
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

    override suspend fun logSets(
        workoutExerciseId: EntityId,
        sets: List<DraftSet>,
        inputMode: InputMode,
        notes: String?,
        personalRecords: List<PrHit>,
    ): LogSnapshot = write {
        val now = now()
        val we = requireNotNull(workoutExercises.getById(workoutExerciseId)) { "Workout exercise $workoutExerciseId not found" }
        val previousSets = this.sets.getForWorkoutExercise(we.id)
        val previousRecords = records.getForSets(previousSets.map { it.id })
        val snapshot = LogSnapshot(
            workoutExerciseId = we.id,
            status = we.status,
            loggedAt = we.loggedAt?.toInstant(),
            inputMode = we.inputMode,
            sets = previousSets.map { it.toDomain() },
            notes = we.notes,
            personalRecords = previousRecords.map { it.toDomain() },
        )
        val newSetIds = sets.map { ids.newId() }

        this.sets.let { dao ->
            // Records hang off sets: replacing the sets replaces their records too.
            sql.delete("personal_records", previousRecords.map { it.id }, now)
            sql.delete("workout_sets", previousSets.map { it.id }, now)
            dao.upsertAll(
                sets.mapIndexed { i, s ->
                    WorkoutSetEntity(
                        id = newSetIds[i],
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
        insertRecords(we.exerciseId, personalRecords, newSetIds, now)
        workoutExercises.upsertAll(
            listOf(
                we.copy(
                    status = ExerciseStatus.LOGGED,
                    loggedAt = now,
                    inputMode = inputMode,
                    notes = notes?.trim()?.takeIf(String::isNotEmpty),
                    sync = we.sync.touch(now),
                ),
            ),
        )
        snapshot
    }

    private suspend fun insertRecords(exerciseId: EntityId, hits: List<PrHit>, setIds: List<String>, now: Long) {
        records.upsertAll(
            hits.map { hit ->
                PersonalRecordEntity(
                    id = ids.newId(),
                    exerciseId = exerciseId,
                    type = hit.type,
                    value = hit.value,
                    workoutSetId = setIds[hit.setIndex],
                    achievedAt = now,
                    sync = SyncMeta.dirty(now),
                )
            },
        )
    }

    override suspend fun getSessionHistory(exerciseId: EntityId, excludeWorkoutExerciseId: EntityId): List<List<WorkoutSet>> {
        val sessions = workoutExercises.getLoggedForExercise(exerciseId, excludeWorkoutExerciseId)
        if (sessions.isEmpty()) return emptyList()
        val bySession = sets.getForWorkoutExercises(sessions.map { it.id }).groupBy { it.workoutExerciseId }
        return sessions.map { s -> bySession[s.id].orEmpty().map { it.toDomain() } }
    }

    /**
     * Bests are computed from every logged working set (not from stored PR rows), so they're right
     * even for the very first session and after edits or deletions.
     */
    override fun observeRecords(exerciseId: EntityId): Flow<List<PersonalRecord>> =
        sets.observeLoggedForExercise(exerciseId).map { rows ->
            val working = rows.filter { it.set.setType != SetType.WARMUP }
            fun best(type: PrType, value: (SetWithDate) -> Double?): PersonalRecord? {
                // Earliest set reaching the best value (when the record was first set).
                val withValue = working.mapNotNull { r -> value(r)?.let { r to it } }
                val top = withValue.maxOfOrNull { it.second } ?: return null
                val (row, v) = withValue.first { it.second >= top - 1e-9 }
                return PersonalRecord(row.set.id, exerciseId, type, v, row.set.id, row.performedAt.toInstant())
            }
            val bestVolume = working.groupBy { it.set.workoutExerciseId }.values
                .map { session -> session to session.sumOf { (it.set.weightKg ?: 0.0) * (it.set.reps ?: 0) } }
                .filter { it.second > 0 }
                .maxByOrNull { it.second }
                ?.let { (session, v) ->
                    val first = session.first()
                    PersonalRecord(first.set.id, exerciseId, PrType.MAX_VOLUME, v, first.set.id, first.performedAt.toInstant())
                }
            listOfNotNull(
                best(PrType.EST_1RM) { r -> r.set.weightKg?.let { w -> r.set.reps?.let { Strength.estimatedOneRepMax(w, it) } } },
                best(PrType.MAX_WEIGHT) { it.set.weightKg },
                best(PrType.MAX_REPS) { it.set.reps?.toDouble() },
                bestVolume,
            )
        }.distinctUntilChanged()

    override suspend fun restoreLog(snapshot: LogSnapshot) = write {
        val now = now()
        val we = workoutExercises.getById(snapshot.workoutExerciseId) ?: return@write
        val currentSets = sets.getForWorkoutExercise(we.id)
        sql.delete("personal_records", records.getForSets(currentSets.map { it.id }).map { it.id }, now)
        sql.delete("workout_sets", currentSets.map { it.id }, now)
        val tombstones = sets.getByIdsIncludingDeleted(snapshot.sets.map { it.id }).associateBy { it.id }
        sets.upsertAll(snapshot.sets.map { it.toEntity(revive(tombstones[it.id], now)) })
        val recordTombstones = records.getByIdsIncludingDeleted(snapshot.personalRecords.map { it.id }).associateBy { it.id }
        records.upsertAll(
            snapshot.personalRecords.map { pr ->
                PersonalRecordEntity(
                    pr.id,
                    pr.exerciseId,
                    pr.type,
                    pr.value,
                    pr.workoutSetId,
                    pr.achievedAt.toEpochMilliseconds(),
                    revive(recordTombstones[pr.id], now),
                )
            },
        )
        workoutExercises.upsertAll(
            listOf(
                we.copy(
                    status = snapshot.status,
                    loggedAt = snapshot.loggedAt?.toEpochMilliseconds(),
                    inputMode = snapshot.inputMode,
                    notes = snapshot.notes,
                    sync = we.sync.touch(now),
                ),
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLastFinished(): Flow<WorkoutSummaryLine?> = workouts.observeLastFinished()
        .distinctUntilChanged()
        .flatMapLatest { w ->
            if (w == null) {
                flowOf(null)
            } else {
                combine(workoutExercises.observeWithExercise(w.id), sets.observeForWorkout(w.id)) { rows, allSets ->
                    WorkoutSummaryLine(
                        workout = w.toDomain(),
                        exerciseCount = rows.size,
                        loggedCount = rows.count { it.workoutExercise.status == ExerciseStatus.LOGGED },
                        volumeKg = Strength.volume(allSets.map { it.toDomain() }.toDraftSets()),
                    )
                }
            }
        }

    override fun observeRecentExerciseIds(limit: Int): Flow<List<EntityId>> =
        workoutExercises.observeRecentExerciseIds(limit).distinctUntilChanged()

    /** One transaction per user action, then ask for a (debounced) sync. */
    private suspend fun <T> write(block: suspend () -> T): T = db.writeTransaction(block).also { sync.requestSync() }
}
