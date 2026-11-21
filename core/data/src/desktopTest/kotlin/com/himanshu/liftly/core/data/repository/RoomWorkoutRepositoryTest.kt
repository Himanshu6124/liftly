package com.himanshu.liftly.core.data.repository

import app.cash.turbine.test
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RoomWorkoutRepositoryTest {
    private val db = inMemoryDb()
    private var clock = 1_000L
    private val time = TimeProvider { Instant.fromEpochMilliseconds(clock) }
    private var nextId = 0
    private val ids = IdGenerator { "id-${++nextId}" }
    private val sync = RecordingSyncController()
    private val exercises = RoomExerciseRepository(db, time, sync, ids)
    private val repo = RoomWorkoutRepository(db, time, ids, sync, SyncSql(db))

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun exercise(name: String) =
        exercises.createCustom(NewCustomExercise(name, Muscle.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS))

    @Test
    fun createdWorkoutIsObservedWithExercisesInOrder() = runTest {
        val bench = exercise("Bench")
        val fly = exercise("Fly")
        repo.createWorkout("", null, listOf(bench, fly))

        val active = assertNotNull(repo.observeActiveWorkout().first())
        assertEquals(listOf("Bench", "Fly"), active.exercises.map { it.exercise.name })
        assertEquals(2, active.pendingCount)
        assertNull(active.workout.endedAt)
    }

    @Test
    fun addAppendsAndReorderPersists() = runTest {
        val a = exercise("A")
        val b = exercise("B")
        val c = exercise("C")
        val w = repo.createWorkout("", null, listOf(a))
        repo.addExercises(w, listOf(b, c))

        val weIds = repo.observeActiveWorkout().first()!!.exercises.map { it.workoutExercise.id }
        repo.reorder(w, listOf(weIds[2], weIds[0], weIds[1]))

        assertEquals(listOf("C", "A", "B"), repo.observeActiveWorkout().first()!!.exercises.map { it.exercise.name })
    }

    @Test
    fun supersetsAreCreatedSetAndDissolvedWhenReorderSplitsThem() = runTest {
        val (a, b, c) = listOf(exercise("A"), exercise("B"), exercise("C"))
        val w = repo.createWorkout("", null, listOf(a, b, c), supersetGroups = listOf(4, 4, null))
        suspend fun groups() = repo.observeActiveWorkout().first()!!.exercises.map { it.exercise.name to it.workoutExercise.supersetGroup }
        assertEquals(listOf("A" to 1, "B" to 1, "C" to null), groups(), "normalized to group 1")

        val wes = repo.observeActiveWorkout().first()!!.exercises.map { it.workoutExercise.id }
        repo.setSupersets(w, mapOf(wes[1] to 1, wes[2] to 1))
        assertEquals(listOf("A" to 1, "B" to 1, "C" to 1), groups())

        repo.reorder(w, listOf(wes[0], wes[2], wes[1]))
        assertEquals(listOf("A" to 1, "C" to 1, "B" to 1), groups(), "reordered within the group: still one superset")

        // Take C (now in the middle) out: A and B are no longer next to each other, so they're not a superset.
        repo.setSupersets(w, mapOf(wes[2] to null))
        repo.reorder(w, listOf(wes[0], wes[2], wes[1]))
        assertEquals(listOf("A" to null, "C" to null, "B" to null), groups(), "A and B no longer adjacent → dissolved")
    }

    @Test
    fun removeThenUndoRestoresTheExercise() = runTest {
        val a = exercise("A")
        val b = exercise("B")
        repo.createWorkout("", null, listOf(a, b))
        val target = repo.observeActiveWorkout().first()!!.exercises.first().workoutExercise.id

        val removed = assertNotNull(repo.removeExercise(target))
        assertEquals(listOf("B"), repo.observeActiveWorkout().first()!!.exercises.map { it.exercise.name })

        repo.restoreExercise(removed)
        assertEquals(listOf("A", "B"), repo.observeActiveWorkout().first()!!.exercises.map { it.exercise.name })
    }

    @Test
    fun undoOfASyncedExerciseRevivesTheTombstone() = runTest {
        val a = exercise("A")
        repo.createWorkout("", null, listOf(a))
        val weId = repo.observeActiveWorkout().first()!!.exercises.single().workoutExercise.id
        // Pretend a server already has this row.
        val dao = db.workoutExerciseDao()
        dao.upsertAll(listOf(dao.getById(weId)!!.let { it.copy(sync = it.sync.copy(syncStatus = SyncStatus.SYNCED, serverVersion = 7)) }))

        val removed = repo.removeExercise(weId)!!
        assertNotNull(dao.getByIdsIncludingDeleted(listOf(weId)).single().sync.deletedAt, "tombstoned, not hard-deleted")

        repo.restoreExercise(removed)
        val revived = dao.getById(weId)!!
        assertNull(revived.sync.deletedAt)
        assertEquals(7, revived.sync.serverVersion)
        assertEquals(SyncStatus.PENDING, revived.sync.syncStatus)
    }

    @Test
    fun finishSkipsPendingEndsWorkoutAndStampsRoutine() = runTest {
        val a = exercise("A")
        val b = exercise("B")
        db.routineDao().upsertAll(listOf(RoutineEntity("r1", "Push", null, 0, null, 0, SyncMeta.dirty(0))))
        val w = repo.createWorkout("", "r1", listOf(a, b))
        val first = repo.observeActiveWorkout().first()!!.exercises.first().workoutExercise.id
        repo.setStatus(first, ExerciseStatus.LOGGED)

        clock = 5_000
        repo.finish(w)

        assertNull(repo.observeActiveWorkout().first())
        assertNull(repo.getInProgressWorkout())
        val statuses = db.workoutExerciseDao().getForWorkout(w).map { it.status }
        assertEquals(listOf(ExerciseStatus.LOGGED, ExerciseStatus.SKIPPED), statuses)
        assertEquals(5_000, db.workoutDao().getById(w)!!.endedAt)
        assertEquals(1_000, db.routineDao().getById("r1")!!.lastPerformedAt)
    }

    @Test
    fun discardRemovesEverything() = runTest {
        val w = repo.createWorkout("", null, listOf(exercise("A")))
        repo.discard(w)
        assertNull(repo.getInProgressWorkout())
        assertTrue(db.workoutExerciseDao().getForWorkout(w).isEmpty())
        assertTrue(db.workoutDao().getByIdsIncludingDeleted(listOf(w)).isEmpty(), "never synced → hard delete")
    }

    @Test
    fun recentExercisesAreDistinctNewestFirst() = runTest {
        val a = exercise("A")
        val b = exercise("B")
        val dao = db.workoutExerciseDao()
        suspend fun logAll(workoutId: String, at: Long) {
            dao.upsertAll(dao.getForWorkout(workoutId).map { it.copy(status = ExerciseStatus.LOGGED, loggedAt = at) })
        }
        val w1 = repo.createWorkout("", null, listOf(a, b))
        logAll(w1, 100)
        repo.finish(w1)
        val w2 = repo.createWorkout("", null, listOf(a))
        logAll(w2, 200)

        repo.observeRecentExerciseIds(10).test {
            assertEquals(listOf(a, b), awaitItem())
        }
    }

    @Test
    fun writesRequestSync() = runTest {
        repo.createWorkout("", null, emptyList())
        assertTrue(sync.requests > 0)
    }
}
