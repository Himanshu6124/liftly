package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncableEntity
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.data.db.writeTransaction
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.Routine
import com.himanshu.liftly.core.domain.model.RoutineExercise
import com.himanshu.liftly.core.domain.model.RoutineExerciseDraft
import com.himanshu.liftly.core.domain.model.RoutineItem
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.usecase.TargetsFromSets
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.Supersets
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.toDraftSets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomRoutineRepository(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val sync: SyncController,
    private val sql: SyncSql,
) : RoutineRepository {
    private val routines = db.routineDao()
    private val routineExercises = db.routineExerciseDao()

    private fun now() = time.now().toEpochMilliseconds()

    override fun observeRoutines(): Flow<List<RoutineWithExercises>> = combine(
        routines.observeAll(),
        routineExercises.observeAll(),
        db.exerciseDao().observeAllIncludingArchived(),
    ) { rs, res, exercises ->
        val byId = exercises.associateBy { it.id }
        val byRoutine = res.groupBy { it.routineId }
        rs.map { r ->
            RoutineWithExercises(
                routine = r.toDomain(),
                items = byRoutine[r.id].orEmpty().mapNotNull { re ->
                    byId[re.exerciseId]?.let { RoutineItem(re.toDomain(), it.toDomain()) }
                },
            )
        }
    }.distinctUntilChanged()

    override fun observeRoutine(id: EntityId): Flow<RoutineWithExercises?> =
        observeRoutines().map { list -> list.find { it.routine.id == id } }.distinctUntilChanged()

    override suspend fun saveRoutine(
        id: EntityId?,
        name: String,
        notes: String?,
        exercises: List<RoutineExerciseDraft>,
    ): EntityId = write {
        val now = now()
        val existing = id?.let { routines.getById(it) }
        val routineId = existing?.id ?: ids.newId()
        routines.upsertAll(
            listOf(
                existing?.copy(name = name.trim(), notes = notes, sync = existing.sync.touch(now))
                    ?: RoutineEntity(routineId, name.trim(), notes, routines.maxOrderIndex() + 1, null, now, SyncMeta.dirty(now)),
            ),
        )
        val current = routineExercises.getForRoutine(routineId).associateBy { it.id }
        val keep = exercises.mapNotNull { it.id }.toSet()
        sql.delete("routine_exercises", current.keys.filter { it !in keep }, now)
        val groups = Supersets.normalize(exercises.map { d -> d.supersetGroup ?: d.id?.let { current[it] }?.supersetGroup })
        routineExercises.upsertAll(
            exercises.mapIndexed { i, d ->
                val old = d.id?.let { current[it] }
                RoutineExerciseEntity(
                    id = old?.id ?: ids.newId(),
                    routineId = routineId,
                    exerciseId = d.exerciseId,
                    orderIndex = i,
                    supersetGroup = groups[i],
                    targetSets = d.targetSets.coerceIn(1, MAX_SETS),
                    targetReps = d.targetReps,
                    targetWeightKg = d.targetWeightKg,
                    sync = old?.sync?.touch(now) ?: SyncMeta.dirty(now),
                )
            },
        )
        routineId
    }

    override suspend fun deleteRoutine(id: EntityId): RoutineWithExercises? = write {
        val r = routines.getById(id) ?: return@write null
        val res = routineExercises.getForRoutine(id)
        val exercises = db.exerciseDao().getByIdsIncludingDeleted(res.map { it.exerciseId }).associateBy { it.id }
        val snapshot = RoutineWithExercises(
            r.toDomain(),
            res.mapNotNull { re -> exercises[re.exerciseId]?.let { RoutineItem(re.toDomain(), it.toDomain()) } },
        )
        val now = now()
        sql.delete("routine_exercises", res.map { it.id }, now)
        sql.delete("routines", listOf(id), now)
        snapshot
    }

    override suspend fun restoreRoutine(snapshot: RoutineWithExercises) = write {
        val now = now()
        val r = snapshot.routine
        val oldRoutine = routines.getByIdsIncludingDeleted(listOf(r.id)).firstOrNull()
        routines.upsertAll(
            listOf(
                RoutineEntity(
                    r.id,
                    r.name,
                    r.notes,
                    r.orderIndex,
                    r.lastPerformedAt?.toEpochMilliseconds(),
                    r.createdAt.toEpochMilliseconds(),
                    revive(oldRoutine, now),
                ),
            ),
        )
        val old = routineExercises.getByIdsIncludingDeleted(snapshot.items.map { it.routineExercise.id }).associateBy { it.id }
        routineExercises.upsertAll(
            snapshot.items.map { item ->
                val re = item.routineExercise
                RoutineExerciseEntity(
                    re.id, re.routineId, re.exerciseId, re.orderIndex, re.supersetGroup, re.targetSets, re.targetReps,
                    re.targetWeightKg, revive(old[re.id], now),
                )
            },
        )
    }

    override suspend fun reorderRoutines(orderedIds: List<EntityId>) = write {
        val now = now()
        val position = orderedIds.withIndex().associate { (i, id) -> id to i }
        routines.upsertAll(
            routines.getAll().mapNotNull { r ->
                val index = position[r.id] ?: return@mapNotNull null
                if (r.orderIndex == index) null else r.copy(orderIndex = index, sync = r.sync.touch(now))
            },
        )
    }

    override suspend fun saveWorkoutAsRoutine(workoutId: EntityId, name: String): EntityId {
        val groups = db.workoutExerciseDao().getForWorkout(workoutId).map { it.supersetGroup }
        val drafts = workoutTargets(workoutId).mapIndexed { i, (exerciseId, targets) ->
            RoutineExerciseDraft(null, exerciseId, targets?.sets ?: DEFAULT_SETS, targets?.reps, targets?.weightKg, groups.getOrNull(i))
        }
        return saveRoutine(null, name, null, drafts)
    }

    override suspend fun updateTargetsFromWorkout(routineId: EntityId, workoutId: EntityId) = write {
        val now = now()
        val done = workoutTargets(workoutId).mapNotNull { (exerciseId, t) -> t?.let { exerciseId to it } }.toMap()
        routineExercises.upsertAll(
            routineExercises.getForRoutine(routineId).mapNotNull { re ->
                val t = done[re.exerciseId] ?: return@mapNotNull null
                re.copy(targetSets = t.sets, targetReps = t.reps, targetWeightKg = t.weightKg, sync = re.sync.touch(now))
            },
        )
    }

    /** Exercises of a workout in order, with targets derived from logged sets (null if not logged). */
    private suspend fun workoutTargets(workoutId: EntityId): List<Pair<EntityId, TargetsFromSets.Targets?>> {
        val wes = db.workoutExerciseDao().getForWorkout(workoutId)
        val sets = db.workoutSetDao().getForWorkoutExercises(wes.map { it.id }).groupBy { it.workoutExerciseId }
        return wes.map { we ->
            val targets = if (we.status == ExerciseStatus.LOGGED) {
                TargetsFromSets.derive(sets[we.id].orEmpty().map { it.toDomain() }.toDraftSets())
            } else {
                null
            }
            we.exerciseId to targets
        }
    }

    private fun revive(existing: SyncableEntity?, now: Long): SyncMeta =
        existing?.sync?.copy(deletedAt = null)?.touch(now) ?: SyncMeta.dirty(now)

    private suspend fun <T> write(block: suspend () -> T): T = db.writeTransaction(block).also { sync.requestSync() }

    private companion object {
        const val DEFAULT_SETS = 3
        const val MAX_SETS = 20
    }
}

internal fun RoutineEntity.toDomain() = Routine(
    id = id,
    name = name,
    notes = notes,
    orderIndex = orderIndex,
    lastPerformedAt = lastPerformedAt?.toInstant(),
    createdAt = createdAt.toInstant(),
)

internal fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    supersetGroup = supersetGroup,
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeightKg = targetWeightKg,
)
