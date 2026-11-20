package com.himanshu.liftly.core.data.seed

import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.repository.RoomExerciseRepository
import com.himanshu.liftly.core.data.testing.FakeSettingsRepository
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExerciseSeederTest {
    private val db = inMemoryDb()
    private val settings = FakeSettingsRepository()
    private val sync = RecordingSyncController()
    private val repo = RoomExerciseRepository(db, { Instant.fromEpochMilliseconds(1_000) }, sync)
    private var json = seedJson(version = 1, "Bench Press" to "bench_press", "Squat" to "squat")
    private val seeder = RoomExerciseSeeder(db, settings, { json })

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun firstLaunchInsertsLibraryAsSyncedWithDeterministicIds() = runTest {
        seeder.seedIfNeeded()

        val all = repo.observeLibrary().first()
        assertEquals(listOf("Bench Press", "Squat"), all.map { it.name })
        assertEquals("seed:bench_press", all.first().id)
        assertTrue(db.exerciseDao().pendingSync().isEmpty(), "seed rows are not pushed to a backend")
        assertEquals(1, settings.seed)
    }

    @Test
    fun secondLaunchIsANoOp() = runTest {
        seeder.seedIfNeeded()
        json = "not even json" // would throw if it were read again
        seeder.seedIfNeeded()
        assertEquals(2, repo.observeLibrary().first().size)
    }

    @Test
    fun upgradeUpdatesContentButKeepsUserData() = runTest {
        seeder.seedIfNeeded()
        repo.setFavorite("seed:bench_press", true)
        repo.setUserNote("seed:bench_press", "  seat height 4 ")

        settings.seed = 0 // simulate an app update shipping a newer seed
        json = seedJson(version = 2, "Barbell Bench Press" to "bench_press", "Squat" to "squat", "Row" to "row")
        seeder.seedIfNeeded()

        val bench = repo.observeExercise("seed:bench_press").first()!!
        assertEquals("Barbell Bench Press", bench.name)
        assertTrue(bench.isFavorite)
        assertEquals("seat height 4", bench.userNote)
        assertEquals(3, repo.observeLibrary().first().size)
        assertEquals(2, settings.seed)
    }

    @Test
    fun userEditsMarkRowPendingAndRequestSync() = runTest {
        seeder.seedIfNeeded()
        repo.setFavorite("seed:squat", true)

        val pending = db.exerciseDao().pendingSync().single()
        assertEquals("seed:squat", pending.id)
        assertEquals(SyncStatus.PENDING, pending.sync.syncStatus)
        assertEquals(1_000, pending.sync.updatedAt)
        assertEquals(1, sync.requests)
    }

    @Test
    fun noOpEditDoesNotDirtyTheRow() = runTest {
        seeder.seedIfNeeded()
        repo.setFavorite("seed:squat", false)
        repo.setUserNote("seed:squat", "   ")
        assertTrue(db.exerciseDao().pendingSync().isEmpty())
        assertEquals(0, sync.requests)
        assertNull(repo.observeExercise("seed:squat").first()!!.userNote)
    }

    @Test
    fun bundledSeedFileIsValid() = runTest {
        val resources = File("../resources/src/commonMain/composeResources")
        val text = File(resources, "files/exercises.json").readText()
        json = text
        seeder.seedIfNeeded()

        val all = repo.observeLibrary().first()
        assertTrue(all.size >= 100, "need at least 100 exercises, got ${all.size}")
        assertEquals(all.size, all.map { it.seedKey }.toSet().size)
        all.forEach { e ->
            assertTrue(e.formCues.size in 3..4, "${e.name} cues")
            assertTrue(e.commonMistakes.size in 2..4, "${e.name} mistakes")
            assertTrue(e.instructions.size in 1..6, "${e.name} instructions")
            assertFalse(e.imagePaths.isEmpty(), "${e.name} images")
            e.imagePaths.forEach { path -> assertTrue(File(resources, path).exists(), "missing $path") }
        }
    }

    private fun seedJson(version: Int, vararg exercises: Pair<String, String>): String {
        val items = exercises.joinToString { (name, key) -> seedItem(name, key) }
        return """{"version": $version, "exercises": [$items]}"""
    }

    private fun seedItem(name: String, key: String) =
        """{"seedKey":"$key","name":"$name","primaryMuscle":"CHEST","equipment":"BARBELL","trackingType":"WEIGHT_REPS"}"""
}
