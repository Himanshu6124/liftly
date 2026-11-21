package com.himanshu.liftly.core.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.protocol.SyncProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AccountSyncControllerTest {
    private val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
    private val server = FakeServer()
    private val tables = SyncTables(db)
    private val sql = SyncSql(db)
    private val routines = db.routineDao()

    private val auth = object : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut())
        override val providers = listOf(AuthProvider.APPLE)
        override suspend fun signIn(provider: AuthProvider) = Unit
        override suspend fun signOut() = Unit
        override suspend fun deleteAccount() = Unit
    }

    /** Bumped by every successful sync run (its timestamp). */
    @Volatile private var runs = 0L

    @AfterTest
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    /** Runs [trigger] and waits (real time: Room does real I/O) for the sync run it causes. */
    private suspend fun AccountSyncController.awaitRun(trigger: () -> Unit): SyncState = withTimeout(10_000) {
        val before = runs
        trigger()
        state.first { (it is SyncState.Synced && it.at.toEpochMilliseconds() > before) || it is SyncState.Failed }
    }

    /** Lets a signed-out request or a sign-out settle (they must not sync). */
    private suspend fun settle() = delay(200)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun controller() = AccountSyncController(
        auth,
        SyncEngine(db, server, tables, sql),
        sql,
        tables,
        TimeProvider { Instant.fromEpochMilliseconds(++runs) },
        scope,
        debounceMillis = 10,
        retryBaseMillis = 50,
    )

    private fun routine(id: String, name: String, updatedAt: Long) =
        RoutineEntity(id, name, null, 0, null, createdAt = 1, sync = SyncMeta.dirty(updatedAt))

    @Test
    fun signedOutNothingSyncsAndLocalDataStaysPending() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        sync.requestSync()
        settle()
        assertEquals(SyncState.LocalOnly, sync.state.first())
        assertEquals(0, server.pushCalls)
        assertEquals(SyncStatus.PENDING, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
    }

    @Test
    fun signingInUploadsWhatWasLoggedWhileSignedOut() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        assertIs<SyncState.Synced>(sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) })
        assertEquals("Push", (server.rows.getValue(SyncProtocol.ROUTINES to "r1").payload["name"] as JsonPrimitive).content)
        assertEquals(SyncStatus.SYNCED, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
    }

    @Test
    fun signingOutKeepsLocalData() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        auth.state.value = AuthState.SignedOut()
        settle()
        assertEquals(SyncState.LocalOnly, sync.state.first())
        assertEquals(listOf("Push"), routines.observeAll().first().map { it.name })
    }

    @Test
    fun aDifferentAccountGetsThisDevicesDataAsNew() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10), routine("r2", "Pull", 11)))
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        // Alice deletes a routine; the tombstone would be pushed to her account...
        sql.delete(SyncProtocol.ROUTINES, listOf("r2"), now = 20)
        auth.state.value = AuthState.SignedOut()
        settle()
        // ...but Bob signs in instead: no tombstone to tell his account, everything else is new to it.
        sql.bindAccount("bob", tables.all.map { it.name })
        val r1 = routines.getByIdsIncludingDeleted(listOf("r1")).single()
        assertEquals(SyncStatus.PENDING, r1.sync.syncStatus)
        assertNull(r1.sync.serverVersion)
        assertTrue(routines.getByIdsIncludingDeleted(listOf("r2")).isEmpty(), "tombstone dropped")
        assertNull(db.syncCursorDao().get(SyncProtocol.ROUTINES), "cursors cleared")
        assertEquals("bob", db.syncCursorDao().get(SyncSql.ACCOUNT_KEY))

        val pushesBefore = server.pushCalls
        assertIs<SyncState.Synced>(sync.awaitRun { auth.state.value = AuthState.SignedIn("bob", null) })
        assertEquals(pushesBefore + 1, server.pushCalls, "r1 uploaded again, to Bob's account")
        assertEquals(SyncStatus.SYNCED, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
    }

    @Test
    fun theSameAccountAgainKeepsItsSyncState() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        auth.state.value = AuthState.SignedOut()
        settle()
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        assertEquals(SyncStatus.SYNCED, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
        assertEquals(1, server.pushCalls, "nothing re-uploaded")
    }

    @Test
    fun libraryExercisesSyncOnlyTheUsersChanges() = runBlocking {
        val seeded = ExerciseEntity(
            id = "seed:bench", seedKey = "bench", name = "Bench Press (v2 text)", aliases = emptyList(),
            primaryMuscle = Muscle.entries.first(), secondaryMuscles = emptyList(), equipment = Equipment.entries.first(),
            trackingType = TrackingType.entries.first(), mechanic = null, difficulty = null, imagePaths = emptyList(),
            videoUrl = null, instructions = listOf("new instructions"), formCues = emptyList(), commonMistakes = emptyList(),
            userNote = null, isFavorite = false, isCustom = false, isArchived = false,
            sync = SyncMeta(
                updatedAt = 0,
                syncStatus = SyncStatus.SYNCED,
            ),
        )
        db.exerciseDao().upsertAll(listOf(seeded))
        // Another device, with an older library text, favorited it and wrote a note.
        val fromOtherDevice = seeded.copy(
            name = "Bench Press (v1 text)",
            instructions = listOf("old instructions"),
            isFavorite = true,
            userNote = "Elbows in",
            sync = SyncMeta.dirty(50),
        )
        server.remoteWrite(
            tables.all.first().let {
                @Suppress("UNCHECKED_CAST")
                (it as TableSync<ExerciseEntity>).toRecord(fromOtherDevice)
            },
        )
        val sync = controller()
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        val local = db.exerciseDao().getByIdsIncludingDeleted(listOf("seed:bench")).single()
        assertEquals("Bench Press (v2 text)", local.name)
        assertEquals(listOf("new instructions"), local.instructions)
        assertEquals(true, local.isFavorite)
        assertEquals("Elbows in", local.userNote)
    }

    @Test
    fun failedSyncRetriesByItselfWithBackoff() = runBlocking {
        val sync = controller()
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        server.failNext = 2 // offline for the first run and the first retry
        val first = sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        assertIs<SyncState.Failed>(first)
        // No more requests: the backoff retries (50 ms, then 100 ms) bring it through.
        withTimeout(10_000) { sync.state.first { it is SyncState.Synced } }
        assertEquals(SyncStatus.SYNCED, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
    }

    @Test
    fun syncNowWaitsForTheResult() = runBlocking {
        val sync = controller()
        assertTrue(sync.syncNow(), "signed out: nothing to do")
        assertEquals(0, server.pushCalls)
        sync.awaitRun { auth.state.value = AuthState.SignedIn("alice", null) }
        routines.upsertAll(listOf(routine("r1", "Push", 10)))
        assertTrue(sync.syncNow())
        assertEquals(SyncStatus.SYNCED, routines.getByIdsIncludingDeleted(listOf("r1")).single().sync.syncStatus)
        server.failNext = 1
        routines.upsertAll(listOf(routine("r2", "Pull", 11)))
        assertEquals(false, sync.syncNow())
    }
}
