package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.PrefillSource
import com.himanshu.liftly.core.domain.model.RoutineTarget
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class LogSetsTest {
    private val db = inMemoryDb()
    private var clock = 1_000L
    private val time = TimeProvider { Instant.fromEpochMilliseconds(clock) }
    private var nextId = 0
    private val ids = IdGenerator { "id-${++nextId}" }
    private val sync = RecordingSyncController()
    private val exercises = RoomExerciseRepository(db, time, sync, ids)
    private val repo = RoomWorkoutRepository(db, time, ids, sync, SyncSql(db))
    private val logSets = LogExerciseSetsUseCase(repo)

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun bench() = exercises.createCustom(
        NewCustomExercise("Bench", Muscle.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
    )

    private suspend fun firstWorkoutExercise() = repo.observeActiveWorkout().first()!!.exercises.first()

    private val fourSets = listOf(DraftSet(80.0, 8), DraftSet(80.0, 8), DraftSet(80.0, 7), DraftSet(80.0, 6))

    @Test
    fun savingReplacesSetsAndMarksLogged() = runTest {
        repo.createWorkout("", null, listOf(bench()))
        val weId = firstWorkoutExercise().workoutExercise.id

        assertIs<LogExerciseSetsUseCase.Result.Saved>(logSets(weId, fourSets, InputMode.UNIFORM))
        assertIs<LogExerciseSetsUseCase.Result.Saved>(logSets(weId, listOf(DraftSet(85.0, 5)), InputMode.PER_SET))

        val logged = firstWorkoutExercise()
        assertEquals(ExerciseStatus.LOGGED, logged.workoutExercise.status)
        assertEquals(InputMode.PER_SET, logged.workoutExercise.inputMode)
        assertEquals(Instant.fromEpochMilliseconds(1_000), logged.workoutExercise.loggedAt)
        assertEquals(listOf(85.0 to 5), logged.sets.map { it.weightKg to it.reps })
    }

    @Test
    fun reSavingTheSameSetsIsIdempotent() = runTest {
        repo.createWorkout("", null, listOf(bench()))
        val weId = firstWorkoutExercise().workoutExercise.id
        logSets(weId, fourSets, InputMode.UNIFORM)
        logSets(weId, fourSets, InputMode.UNIFORM)

        val sets = firstWorkoutExercise().sets
        assertEquals(4, sets.size)
        assertEquals(listOf(8, 8, 7, 6), sets.map { it.reps })
        assertEquals(listOf(0, 1, 2, 3), sets.map { it.orderIndex })
    }

    @Test
    fun emptySetsAreDroppedAndInvalidInputIsRejected() = runTest {
        repo.createWorkout("", null, listOf(bench()))
        val weId = firstWorkoutExercise().workoutExercise.id

        logSets(weId, listOf(DraftSet(80.0, 8), DraftSet(null, null)), InputMode.UNIFORM)
        assertEquals(1, firstWorkoutExercise().sets.size)

        assertIs<LogExerciseSetsUseCase.Result.Invalid>(logSets(weId, listOf(DraftSet(80.0, 0)), InputMode.UNIFORM))
        assertIs<LogExerciseSetsUseCase.Result.Invalid>(logSets(weId, listOf(DraftSet(1500.0, 5)), InputMode.UNIFORM))
        assertIs<LogExerciseSetsUseCase.Result.Invalid>(logSets(weId, listOf(DraftSet(null, null)), InputMode.UNIFORM))
        assertEquals(1, firstWorkoutExercise().sets.size, "rejected saves don't touch the DB")
    }

    @Test
    fun undoRestoresPendingStateWithNoSets() = runTest {
        repo.createWorkout("", null, listOf(bench()))
        val weId = firstWorkoutExercise().workoutExercise.id
        val saved = logSets(weId, fourSets, InputMode.UNIFORM) as LogExerciseSetsUseCase.Result.Saved

        repo.restoreLog(saved.undo)

        val restored = firstWorkoutExercise()
        assertEquals(ExerciseStatus.PENDING, restored.workoutExercise.status)
        assertNull(restored.workoutExercise.loggedAt)
        assertEquals(0, restored.sets.size)
    }

    @Test
    fun undoOfAnEditBringsBackThePreviousSets() = runTest {
        repo.createWorkout("", null, listOf(bench()))
        val weId = firstWorkoutExercise().workoutExercise.id
        logSets(weId, fourSets, InputMode.UNIFORM)
        val edit = logSets(weId, listOf(DraftSet(100.0, 1)), InputMode.UNIFORM) as LogExerciseSetsUseCase.Result.Saved

        repo.restoreLog(edit.undo)
        assertEquals(listOf(8, 8, 7, 6), firstWorkoutExercise().sets.map { it.reps })
    }

    @Test
    fun prefillComesFromTheLastSessionInAnotherWorkout() = runTest {
        val bench = bench()
        val w1 = repo.createWorkout("", null, listOf(bench))
        logSets(firstWorkoutExercise().workoutExercise.id, fourSets, InputMode.UNIFORM)
        repo.finish(w1)

        clock = 9_000
        repo.createWorkout("", null, listOf(bench))
        val data = assertNotNull(repo.getLogSheetData(firstWorkoutExercise().workoutExercise.id))

        assertEquals(w1, data.lastSession?.workoutId)
        val prefill = GetLogSheetPrefillUseCase()(data, defaultSetCount = 3)
        assertEquals(PrefillSource.LAST_SESSION, prefill.source)
        assertEquals(fourSets, prefill.sets)
    }

    @Test
    fun routineTargetIsLoadedForRoutineWorkouts() = runTest {
        val bench = bench()
        db.routineDao().upsertAll(listOf(RoutineEntity("r1", "Push", null, 0, null, 0, SyncMeta.dirty(0))))
        db.routineExerciseDao().upsertAll(listOf(RoutineExerciseEntity("re1", "r1", bench, 0, null, 4, 10, 60.0, SyncMeta.dirty(0))))
        repo.createWorkout("", "r1", listOf(bench))

        val data = assertNotNull(repo.getLogSheetData(firstWorkoutExercise().workoutExercise.id))
        assertEquals(RoutineTarget(4, 10, 60.0), data.routineTarget)
        assertNull(data.lastSession)
    }
}
