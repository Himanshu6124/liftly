package com.himanshu.liftly.server

import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.network.ApiConfig
import com.himanshu.liftly.core.network.AuthApi
import com.himanshu.liftly.core.network.AuthSession
import com.himanshu.liftly.core.network.liftlyHttpClient
import com.himanshu.liftly.core.platform.InMemorySecureStore
import com.himanshu.liftly.core.platform.NativeSignIn
import com.himanshu.liftly.core.platform.SignInCredential
import com.himanshu.liftly.core.platform.SignInProvider
import io.ktor.server.testing.testApplication
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
}
