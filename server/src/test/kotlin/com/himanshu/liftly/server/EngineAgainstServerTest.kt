package com.himanshu.liftly.server

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.data.sync.RemoteSyncDataSource
import com.himanshu.liftly.core.data.sync.SyncEngine
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.sync.SyncTables
import com.himanshu.liftly.core.protocol.PullPage
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.first
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The app's real [SyncEngine] (two "devices", each with its own in-memory Room database) syncing
 * through this server and PostgreSQL. Same scenarios the engine tests run against `FakeServer`.
 */
class EngineAgainstServerTest {

    @BeforeTest
    fun reset() = TestDatabase.reset()

    /** Minimal HTTP client for the test; the app's real one arrives in Milestone 23. */
    private class HttpRemote(private val client: HttpClient, private val user: String) : RemoteSyncDataSource {
        override suspend fun push(table: String, records: List<SyncRecord>): List<PushAck> =
            client.post("/sync/$table/push") {
                header(DevUserResolver.HEADER, user)
                contentType(ContentType.Application.Json)
                setBody(records)
            }.body()

        override suspend fun pull(table: String, cursor: String?, limit: Int): PullPage =
            client.get("/sync/$table/pull") {
                header(DevUserResolver.HEADER, user)
                cursor?.let { url.parameters.append("cursor", it) }
                url.parameters.append("limit", limit.toString())
            }.body()
    }

    private class Device(remote: RemoteSyncDataSource, pageSize: Int) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        val sql = SyncSql(db)
        val engine = SyncEngine(db, remote, SyncTables(db), sql, pageSize)
        val routines = db.routineDao()

        suspend fun sync() = engine.syncOnce().getOrThrow()
        suspend fun names() = routines.observeAll().first().map { it.name }.sorted()
    }

    private fun twoDevices(pageSize: Int = SyncProtocol.MAX_BATCH, block: suspend (Device, Device) -> Unit) = testApplication {
        application { liftlyModule(TestDatabase.dataSource, DevUserResolver(enabled = true)) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val a = Device(HttpRemote(client, "alice"), pageSize)
        val b = Device(HttpRemote(client, "alice"), pageSize)
        try {
            block(a, b)
        } finally {
            a.db.close()
            b.db.close()
        }
    }

    private fun routine(id: String, name: String, updatedAt: Long) =
        RoutineEntity(id, name, null, 0, null, createdAt = 1, sync = SyncMeta.dirty(updatedAt))

    @Test
    fun createEditAndDeleteReachTheOtherDevice() = twoDevices { a, b ->
        a.routines.upsertAll(listOf(routine("r1", "Push Day", 10)))
        a.sync()
        b.sync()
        assertEquals(listOf("Push Day"), b.names())
        assertEquals(SyncStatus.SYNCED, b.routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)

        val onB = b.routines.getByIdsIncludingDeleted(listOf("r1")).single()
        b.routines.upsertAll(listOf(onB.copy(name = "Push Day v2", sync = onB.sync.touch(20))))
        b.sync()
        a.sync()
        assertEquals(listOf("Push Day v2"), a.names())

        a.sql.delete(SyncProtocol.ROUTINES, listOf("r1"), now = 30)
        a.sync()
        b.sync()
        assertTrue(b.names().isEmpty())
        assertTrue(a.routines.getByIdsIncludingDeleted(listOf("r1")).isEmpty(), "tombstone purged on A")
        assertTrue(b.routines.getByIdsIncludingDeleted(listOf("r1")).isEmpty(), "tombstone purged on B")
    }

    @Test
    fun concurrentEditsResolveToTheNewestOnBothDevices() = twoDevices { a, b ->
        a.routines.upsertAll(listOf(routine("r1", "Original", 10)))
        a.sync()
        b.sync()

        val onA = a.routines.getByIdsIncludingDeleted(listOf("r1")).single()
        val onB = b.routines.getByIdsIncludingDeleted(listOf("r1")).single()
        a.routines.upsertAll(listOf(onA.copy(name = "A's edit", sync = onA.sync.touch(30))))
        b.routines.upsertAll(listOf(onB.copy(name = "B's older edit", sync = onB.sync.touch(20))))

        a.sync()
        b.sync() // B's push loses; its pull brings A's edit.
        a.sync()
        assertEquals(listOf("A's edit"), a.names())
        assertEquals(listOf("A's edit"), b.names())
        assertTrue(b.routines.pendingSync().isEmpty())
    }

    @Test
    fun manyRowsSyncAcrossSeveralPages() = twoDevices(pageSize = 3) { a, b ->
        a.routines.upsertAll((1..10).map { routine("r$it", "Routine $it", it.toLong()) })
        a.sync()
        b.sync()
        assertEquals(10, b.names().size)
    }
}
