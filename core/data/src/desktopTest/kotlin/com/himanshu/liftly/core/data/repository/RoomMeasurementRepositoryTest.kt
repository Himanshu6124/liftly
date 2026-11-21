package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.sync.SyncTables
import com.himanshu.liftly.core.data.testing.RecordingSyncController
import com.himanshu.liftly.core.data.testing.inMemoryDb
import com.himanshu.liftly.core.domain.model.MeasurementType
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RoomMeasurementRepositoryTest {
    private val db = inMemoryDb()
    private var clock = 1_000L
    private var nextId = 0
    private val sync = RecordingSyncController()
    private val repo = RoomMeasurementRepository(
        db,
        TimeProvider { Instant.fromEpochMilliseconds(clock) },
        IdGenerator { "m-${++nextId}" },
        sync,
        SyncSql(db),
    )

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun entriesArePerTypeOldestFirst() = runTest {
        repo.add(MeasurementType.BODY_WEIGHT, 80.0)
        clock = 2_000
        repo.add(MeasurementType.WAIST, 84.0)
        clock = 3_000
        repo.add(MeasurementType.BODY_WEIGHT, 79.2)

        assertEquals(listOf(80.0, 79.2), repo.observe(MeasurementType.BODY_WEIGHT).first().map { it.value })
        assertEquals(Instant.fromEpochMilliseconds(3_000), repo.observe(MeasurementType.BODY_WEIGHT).first().last().measuredAt)
        assertEquals(setOf(MeasurementType.BODY_WEIGHT, MeasurementType.WAIST), repo.observeUsedTypes().first())
        assertEquals(3, sync.requests)
    }

    @Test
    fun deleteThenUndoRestoresTheEntry() = runTest {
        val id = repo.add(MeasurementType.BODY_FAT, 18.5)
        val deleted = assertNotNull(repo.delete(id))
        assertTrue(repo.observe(MeasurementType.BODY_FAT).first().isEmpty())
        repo.restore(deleted)
        assertEquals(listOf(18.5), repo.observe(MeasurementType.BODY_FAT).first().map { it.value })
    }

    @Test
    fun measurementsTakePartInSync() = runTest {
        repo.add(MeasurementType.CHEST, 100.0)
        val table = SyncTables(db).all.single { it.name == "body_measurements" }
        assertEquals(1, table.dao.pendingSync().size, "new entries are pending for the backend")
    }
}
