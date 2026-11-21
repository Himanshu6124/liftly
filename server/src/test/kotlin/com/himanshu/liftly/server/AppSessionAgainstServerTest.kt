package com.himanshu.liftly.server

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.sync.SyncEngine
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.sync.SyncTables
import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.network.ApiConfig
import com.himanshu.liftly.core.network.AuthApi
import com.himanshu.liftly.core.network.AuthSession
import com.himanshu.liftly.core.network.HttpSyncDataSource
import com.himanshu.liftly.core.network.liftlyHttpClient
import com.himanshu.liftly.core.platform.InMemorySecureStore
import com.himanshu.liftly.core.platform.NativeSignIn
import com.himanshu.liftly.core.platform.SignInCredential
import com.himanshu.liftly.core.platform.SignInProvider
import io.ktor.client.HttpClient
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.first
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** The app's real session code (core/network) signing in, refreshing and deleting against this server. */
class AppSessionAgainstServerTest {

    @BeforeTest
    fun reset() = TestDatabase.reset()

    private val apple = object : NativeSignIn {
        override val providers = listOf(SignInProvider.APPLE)
        override suspend fun signIn(provider: SignInProvider) =
            SignInCredential(SignInProvider.APPLE, TestAuth.appleToken(rawNonce = "n1"), rawNonce = "n1")
    }

    @Test
    fun signInRefreshAndDeleteThroughTheAppClient() = testApplication {
        application { liftlyModule(TestDatabase.dataSource, TestAuth.service(), devAuth = false) }
        val http = liftlyHttpClient(ApiConfig("http://localhost"), client)
        var now = System.currentTimeMillis()
        val session = AuthSession(AuthApi(http), InMemorySecureStore(), apple, now = { now })

        session.signIn(AuthProvider.APPLE)
        val signedIn = assertIs<AuthState.SignedIn>(session.state.value)
        assertEquals("a@privaterelay.appleid.com", signedIn.email)

        // Past the refresh margin: the session refreshes with the server before calling.
        now += 14 * 60_000 + 30_000
        val account = session.authorized { AuthApi(http).account(it) }
        assertEquals(signedIn.userId, account.userId)

        session.deleteAccount()
        assertEquals(AuthState.SignedOut(), session.state.value)
        assertEquals(AuthError.REJECTED, assertFailsWith<AuthException> { session.authorized { it } }.error)
    }

    private fun appleAs(sub: String) = object : NativeSignIn {
        override val providers = listOf(SignInProvider.APPLE)
        override suspend fun signIn(provider: SignInProvider) =
            SignInCredential(SignInProvider.APPLE, TestAuth.appleToken(sub = sub, rawNonce = "n"), rawNonce = "n")
    }

    /** One phone: its own database and session, syncing through the app's HTTP transport. */
    private class Phone(http: HttpClient, signIn: NativeSignIn) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        val session = AuthSession(AuthApi(http), InMemorySecureStore(), signIn, now = System::currentTimeMillis)
        private val sql = SyncSql(db)
        private val tables = SyncTables(db)
        private val engine = SyncEngine(db, HttpSyncDataSource(http, session), tables, sql)

        suspend fun sync() {
            val user = (session.state.value as AuthState.SignedIn).userId
            sql.bindAccount(user, tables.all.map { it.name })
            engine.syncOnce().getOrThrow()
        }

        suspend fun routineNames() = db.routineDao().observeAll().first().map { it.name }
    }

    @Test
    fun phonesOnTheSameAccountShareDataAndOtherAccountsDoNot() = testApplication {
        application { liftlyModule(TestDatabase.dataSource, TestAuth.service(), devAuth = false) }
        val http = liftlyHttpClient(ApiConfig("http://localhost"), client)
        val iphone = Phone(http, appleAs("alice"))
        val ipad = Phone(http, appleAs("alice"))
        val bobs = Phone(http, appleAs("bob"))
        try {
            // Logged before signing in: uploaded on the first sync.
            iphone.db.routineDao().upsertAll(listOf(RoutineEntity("r1", "Push Day", null, 0, null, 1, SyncMeta.dirty(10))))
            listOf(iphone, ipad, bobs).forEach { it.session.signIn(AuthProvider.APPLE) }
            iphone.sync()
            ipad.sync()
            bobs.sync()
            assertEquals(listOf("Push Day"), ipad.routineNames())
            assertEquals(emptyList(), bobs.routineNames())
        } finally {
            listOf(iphone, ipad, bobs).forEach { it.db.close() }
        }
    }
}
