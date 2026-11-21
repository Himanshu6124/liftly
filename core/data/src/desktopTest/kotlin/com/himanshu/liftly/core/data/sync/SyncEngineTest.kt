package com.himanshu.liftly.core.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.protocol.SyncProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncEngineTest {
    private val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val server = FakeServer()
    private val tables = SyncTables(db)
    private val sql = SyncSql(db)
    private val engine = SyncEngine(db, server, tables, sql)
    private val routines = db.routineDao()

    @AfterTest
    fun tearDown() = db.close()

    private fun routine(id: String, name: String, updatedAt: Long) =
        RoutineEntity(id, name, null, 0, null, createdAt = 1, sync = SyncMeta.dirty(updatedAt))

    @Test
    fun everySyncedTableIsInTheProtocolInTheSameOrder() {
        // The server only accepts SyncProtocol.TABLES: a table registered here but missing there would never sync.
        assertEquals(SyncProtocol.TABLES, tables.all.map { it.name })
    }

    @Test
    fun localChangesArePushedAndMarkedSynced() = runTest {
        routines.upsertAll(listOf(routine("r1", "Push Day", 10)))

        engine.syncOnce().getOrThrow()

        val stored = assertNotNull(server.rows["routines" to "r1"])
        assertEquals("Push Day", (stored.payload["name"] as JsonPrimitive).content)
        val local = routines.getByIdsIncludingDeleted(listOf("r1")).single()
        assertEquals(SyncStatus.SYNCED, local.sync.syncStatus)
        assertNotNull(local.sync.serverVersion)
        assertTrue(routines.pendingSync().isEmpty())
    }

    @Test
    fun deletingUnsyncedRowRemovesItWithoutTombstone() = runTest {
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        sql.delete("routines", listOf("r1"), now = 20)

        assertTrue(routines.getByIdsIncludingDeleted(listOf("r1")).isEmpty())
        engine.syncOnce().getOrThrow()
        assertNull(server.rows["routines" to "r1"])
    }

    @Test
    fun deletingSyncedRowPushesTombstoneThenPurgesIt() = runTest {
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        engine.syncOnce().getOrThrow()

        sql.delete("routines", listOf("r1"), now = 20)
        assertTrue(routines.observeAll().first().isEmpty(), "tombstone hidden from UI queries")
        engine.syncOnce().getOrThrow()

        assertEquals(20L, server.rows.getValue("routines" to "r1").deletedAt)
        assertTrue(routines.getByIdsIncludingDeleted(listOf("r1")).isEmpty(), "tombstone purged after ack")
    }

    @Test
    fun remoteChangesArePulled() = runTest {
        server.remoteWrite(tables.all[1].toRecordOf(routine("r9", "From other device", 50)))

        engine.syncOnce().getOrThrow()

        val local = routines.observeAll().first().single()
        assertEquals("From other device", local.name)
        assertEquals(SyncStatus.SYNCED, local.sync.syncStatus)
    }

    @Test
    fun newerLocalEditWinsOverOlderRemoteEdit() = runTest {
        routines.upsertAll(listOf(routine("r1", "Original", 10)))
        engine.syncOnce().getOrThrow()

        server.remoteWrite(tables.all[1].toRecordOf(routine("r1", "Remote edit", 20)))
        val local = routines.getByIdsIncludingDeleted(listOf("r1")).single()
        routines.upsertAll(listOf(local.copy(name = "Local edit", sync = local.sync.touch(30))))

        engine.syncOnce().getOrThrow()

        assertEquals("Local edit", routines.observeAll().first().single().name)
        assertEquals("Local edit", (server.rows.getValue("routines" to "r1").payload["name"] as JsonPrimitive).content)
    }

    @Test
    fun newerRemoteEditWinsOverOlderLocalEdit() = runTest {
        routines.upsertAll(listOf(routine("r1", "Original", 10)))
        engine.syncOnce().getOrThrow()

        val local = routines.getByIdsIncludingDeleted(listOf("r1")).single()
        routines.upsertAll(listOf(local.copy(name = "Local edit", sync = local.sync.touch(20))))
        server.remoteWrite(tables.all[1].toRecordOf(routine("r1", "Remote edit", 30)))

        engine.syncOnce().getOrThrow()

        assertEquals("Remote edit", routines.observeAll().first().single().name)
    }

    @Suppress("UNCHECKED_CAST")
    private fun TableSync<*>.toRecordOf(entity: RoutineEntity) = (this as TableSync<RoutineEntity>).toRecord(entity)
}
