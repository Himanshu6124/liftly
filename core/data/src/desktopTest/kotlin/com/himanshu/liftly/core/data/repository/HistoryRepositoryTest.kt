package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
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

class HistoryRepositoryTest {
    private val db = inMemoryDb()
    private var clock = 1_000L
    private val time = TimeProvider { Instant.fromEpochMilliseconds(clock) }
    private var nextId = 0
    private val ids = IdGenerator { "id-${++nextId}" }
    private val sync = RecordingSyncController()
    private val exercises = RoomExerciseRepository(db, time, sync, ids)
    private val repo = RoomWorkoutRepository(db, time, ids, sync, SyncSql(db))
    private val log = LogExerciseSetsUseCase(repo)

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun ex(name: String) = exercises.createCustom(
        NewCustomExercise(name, Muscle.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
    )

    private suspend fun session(start: Long, sets: List<DraftSet>, extraPending: Boolean = false): String {
        clock = start
        val bench = ex("Bench$start")
        val list = if (extraPending) listOf(bench, ex("Row$start")) else listOf(bench)
        val w = repo.createWorkout("", null, list)
        log(repo.observeActiveWorkout().first()!!.exercises.first().workoutExercise.id, sets, InputMode.UNIFORM)
        clock = start + 3_600_000
        repo.finish(w)
        return w
    }

    @Test
    fun historyListsFinishedWorkoutsWithStatsNewestFirst() = runTest {
        val first =
            session(
                1_000,
                listOf(DraftSet(40.0, 10, setType = SetType.WARMUP), DraftSet(100.0, 5), DraftSet(100.0, 5)),
                extraPending = true,
            )
        val second = session(100_000_000, listOf(DraftSet(60.0, 10)))
        repo.createWorkout("", null, emptyList()) // in progress: not in history

        val history = repo.observeHistory().first()
        assertEquals(listOf(second, first), history.map { it.workout.id })
        val f = history[1]
        assertEquals(2, f.exerciseCount)
        assertEquals(1, f.loggedCount)
        assertEquals(2, f.workingSets, "warm-up excluded")
        assertEquals(1000.0, f.volumeKg)
    }

    @Test
    fun exerciseSessionsGroupSetsPerWorkoutNewestFirstAndSkipDeleted() = runTest {
        val bench = ex("Bench")
        suspend fun workout(start: Long, name: String, sets: List<DraftSet>): String {
            clock = start
            val w = repo.createWorkout(name, null, listOf(bench))
            log(repo.observeActiveWorkout().first()!!.exercises.single().workoutExercise.id, sets, InputMode.UNIFORM)
            repo.finish(w)
            return w
        }
        val a = workout(1_000, "Push A", listOf(DraftSet(60.0, 8), DraftSet(60.0, 8)))
        val b = workout(200_000_000, "Push B", listOf(DraftSet(40.0, 10, setType = SetType.WARMUP), DraftSet(70.0, 5)))
        val c = workout(300_000_000, "Push C", listOf(DraftSet(72.5, 5)))
        repo.deleteWorkout(c)

        val sessions = repo.observeExerciseSessions(bench).first()
        assertEquals(listOf(b, a), sessions.map { it.workoutId })
        assertEquals(listOf("Push B", "Push A"), sessions.map { it.workoutName })
        assertEquals(listOf(40.0, 70.0), sessions[0].sets.map { it.weightKg })
        assertEquals(Instant.fromEpochMilliseconds(200_000_000), sessions[0].performedAt)
    }

    @Test
    fun deleteAndUndoRestoresEverything() = runTest {
        val w = session(1_000, listOf(DraftSet(100.0, 5)))
        val before = assertNotNull(repo.observeWorkout(w).first())

        val deleted = assertNotNull(repo.deleteWorkout(w))
        assertTrue(repo.observeHistory().first().isEmpty())
        assertNull(repo.observeWorkout(w).first())

        repo.restoreWorkout(deleted)
        val after = assertNotNull(repo.observeWorkout(w).first())
        assertEquals(
            before.exercises.map { it.sets.map { s -> s.weightKg to s.reps } },
            after.exercises.map {
                it.sets.map { s ->
                    s.weightKg to
                        s.reps
                }
            },
        )
        assertEquals(1, repo.observeHistory().first().size)
    }

    @Test
    fun editingAPastWorkoutUpdatesItsStats() = runTest {
        val w = session(1_000, listOf(DraftSet(100.0, 5)))
        val weId = repo.observeWorkout(w).first()!!.exercises.first().workoutExercise.id
        log(weId, listOf(DraftSet(100.0, 5), DraftSet(100.0, 5)), InputMode.UNIFORM)
        assertEquals(1000.0, repo.observeHistory().first().single().volumeKg)
    }
}
