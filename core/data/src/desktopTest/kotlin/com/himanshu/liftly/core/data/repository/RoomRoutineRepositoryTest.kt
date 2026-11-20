package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.RoutineExerciseDraft
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.usecase.StartRoutineUseCase
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
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

class RoomRoutineRepositoryTest {
    private val db = inMemoryDb()
    private var clock = 1_000L
    private val time = TimeProvider { Instant.fromEpochMilliseconds(clock) }
    private var nextId = 0
    private val ids = IdGenerator { "id-${++nextId}" }
    private val sync = RecordingSyncController()
    private val exercises = RoomExerciseRepository(db, time, sync, ids)
    private val workouts = RoomWorkoutRepository(db, time, ids, sync, SyncSql(db))
    private val routines = RoomRoutineRepository(db, time, ids, sync, SyncSql(db))

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun ex(name: String) = exercises.createCustom(
        NewCustomExercise(name, Muscle.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
    )

    @Test
    fun createEditAndListInOrder() = runTest {
        val bench = ex("Bench")
        val fly = ex("Fly")
        val id = routines.saveRoutine(null, " Push ", null, listOf(RoutineExerciseDraft(null, bench, 4, 8, 80.0)))
        routines.saveRoutine(null, "Pull", null, emptyList())

        val push = routines.observeRoutines().first().first()
        assertEquals("Push", push.routine.name)
        assertEquals(listOf(4 to 80.0), push.items.map { it.routineExercise.targetSets to it.routineExercise.targetWeightKg })

        // Edit: keep bench (by id), add fly in front.
        val benchLine = push.items.single().routineExercise.id
        routines.saveRoutine(
            id,
            "Push A",
            null,
            listOf(RoutineExerciseDraft(null, fly, 3, 12, null), RoutineExerciseDraft(benchLine, bench, 5, 5, 90.0)),
        )
        val edited = assertNotNull(routines.observeRoutine(id).first())
        assertEquals(listOf("Fly", "Bench"), edited.items.map { it.exercise.name })
        assertEquals(benchLine, edited.items[1].routineExercise.id)
        assertEquals(listOf("Push A", "Pull"), routines.observeRoutines().first().map { it.routine.name })
    }

    @Test
    fun deleteAndUndo() = runTest {
        val id = routines.saveRoutine(null, "Push", null, listOf(RoutineExerciseDraft(null, ex("Bench"), 3, 8, null)))
        val snapshot = assertNotNull(routines.deleteRoutine(id))
        assertNull(routines.observeRoutine(id).first())
        routines.restoreRoutine(snapshot)
        assertEquals(1, routines.observeRoutine(id).first()?.items?.size)
    }

    @Test
    fun reorder() = runTest {
        val a = routines.saveRoutine(null, "A", null, emptyList())
        val b = routines.saveRoutine(null, "B", null, emptyList())
        routines.reorderRoutines(listOf(b, a))
        assertEquals(listOf("B", "A"), routines.observeRoutines().first().map { it.routine.name })
    }

    @Test
    fun startFromRoutineThenSaveAndUpdateTargets() = runTest {
        val bench = ex("Bench")
        val row = ex("Row")
        val id = routines.saveRoutine(
            null,
            "Upper",
            null,
            listOf(RoutineExerciseDraft(null, bench, 3, 10, 60.0), RoutineExerciseDraft(null, row, 3, 10, 50.0)),
        )
        val routine = assertNotNull(routines.observeRoutine(id).first())

        val started = StartRoutineUseCase(StartWorkoutUseCase(workouts))(routine)
        val active = assertNotNull(workouts.observeActiveWorkout().first())
        assertEquals("Upper", active.workout.name)
        assertEquals(id, active.workout.routineId)
        assertEquals(listOf("Bench", "Row"), active.exercises.map { it.exercise.name })

        // Prefill comes from the routine target the first time.
        assertEquals(60.0, workouts.getLogSheetData(active.exercises.first().workoutExercise.id)?.routineTarget?.weightKg)

        LogExerciseSetsUseCase(workouts)(active.exercises.first().workoutExercise.id, List(4) { DraftSet(65.0, 8) }, InputMode.UNIFORM)
        clock = 5_000
        workouts.finish(started.workoutId)
        assertEquals(Instant.fromEpochMilliseconds(1_000), routines.observeRoutine(id).first()?.routine?.lastPerformedAt)

        routines.updateTargetsFromWorkout(id, started.workoutId)
        val updated = routines.observeRoutine(id).first()!!.items
        assertEquals(Triple(4, 8, 65.0), updated[0].routineExercise.let { Triple(it.targetSets, it.targetReps, it.targetWeightKg) })
        assertEquals(50.0, updated[1].routineExercise.targetWeightKg, "not logged → target unchanged")

        val copyId = routines.saveWorkoutAsRoutine(started.workoutId, "Upper copy")
        val copy = routines.observeRoutine(copyId).first()!!
        assertEquals(listOf("Bench", "Row"), copy.items.map { it.exercise.name })
        assertEquals(4, copy.items[0].routineExercise.targetSets)
        assertEquals(3, copy.items[1].routineExercise.targetSets, "unlogged exercise gets default sets")
        assertTrue(copy.items[1].routineExercise.targetWeightKg == null)
    }
}
