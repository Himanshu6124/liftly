package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.platform.InMemorySecureStore
import com.himanshu.liftly.core.platform.NativeSignIn
import com.himanshu.liftly.core.platform.SignInCancelledException
import com.himanshu.liftly.core.platform.SignInCredential
import com.himanshu.liftly.core.platform.SignInProvider
import com.himanshu.liftly.core.protocol.Account
import com.himanshu.liftly.core.protocol.AuthTokens
import com.himanshu.liftly.core.protocol.RefreshRequest
import com.himanshu.liftly.core.protocol.SignInRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthSessionTest {
    private var now = 1_000_000L
    private val store = InMemorySecureStore()
    private var native: suspend () -> SignInCredential = { SignInCredential(SignInProvider.APPLE, "apple-id-token", "raw") }
    private val calls = mutableListOf<String>()

    /** Server behaviour per "METHOD path"; default: a fake but faithful auth server. */
    private var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req -> fakeServer(req) }
    private var issued = 0

    private suspend fun MockRequestHandleScope.fakeServer(req: HttpRequestData): HttpResponseData = when (
        req.method.value + " " +
            req.url.encodedPath
    ) {
        "POST /auth/apple" -> {
            assertEquals(SignInRequest("apple-id-token", "raw"), req.decode(SignInRequest.serializer()))
            json(tokens())
        }

        "POST /auth/refresh" -> json(tokens())

        "POST /auth/logout" -> respond("", HttpStatusCode.NoContent)

        "DELETE /account", "GET /account" -> if (req.headers[HttpHeaders.Authorization] == "Bearer access-$issued") {
            if (req.method == HttpMethod.Get) json(Account("u1", "a@b.c")) else respond("", HttpStatusCode.NoContent)
        } else {
            respondError(HttpStatusCode.Unauthorized)
        }

        else -> respondError(HttpStatusCode.NotFound)
    }

    private fun tokens() = (++issued).let { AuthTokens("access-$it", now + 15 * 60_000, "refresh-$it", Account("u1", "a@b.c")) }

    private fun MockRequestHandleScope.json(tokens: AuthTokens) = respond(
        Json.encodeToString(AuthTokens.serializer(), tokens),
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun MockRequestHandleScope.json(account: Account) = respond(
        Json.encodeToString(Account.serializer(), account),
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun <T> HttpRequestData.decode(s: kotlinx.serialization.KSerializer<T>): T = Json.decodeFromString(
        s,
        (body as TextContent).text,
    )

    private val client = liftlyHttpClient(
        ApiConfig("http://test"),
        HttpClient(
            MockEngine { req ->
                calls += req.method.value + " " + req.url.encodedPath
                handler(req)
            },
        ),
    )

    private fun session() = AuthSession(
        AuthApi(client),
        store,
        object : NativeSignIn {
            override val providers = listOf(SignInProvider.APPLE)
            override suspend fun signIn(provider: SignInProvider) = native()
        },
        now = { now },
    )

    @Test
    fun signInStoresTheSessionAcrossRestarts() = runTest {
        val session = session()
        assertEquals(AuthState.SignedOut(), session.state.value)
        session.signIn(AuthProvider.APPLE)
        assertEquals(AuthState.SignedIn("u1", "a@b.c"), session.state.value)
        assertEquals(AuthState.SignedIn("u1", "a@b.c"), session().state.value, "restored from the secure store")
    }

    @Test
    fun cancellingOrFailingSignInLeavesTheUserSignedOut() = runTest {
        val session = session()
        native = { throw SignInCancelledException() }
        assertEquals(AuthError.CANCELLED, assertFailsWith<AuthException> { session.signIn(AuthProvider.APPLE) }.error)
        native = { SignInCredential(SignInProvider.APPLE, "apple-id-token", "raw") }
        handler = { respondError(HttpStatusCode.Unauthorized) }
        assertEquals(AuthError.REJECTED, assertFailsWith<AuthException> { session.signIn(AuthProvider.APPLE) }.error)
        handler = { throw IllegalStateException("offline") }
        assertEquals(AuthError.NETWORK, assertFailsWith<AuthException> { session.signIn(AuthProvider.APPLE) }.error)
        assertEquals(AuthState.SignedOut(), session.state.value)
    }

    @Test
    fun accessTokenIsRefreshedBeforeExpiryOnlyOnceForConcurrentCalls() = runTest {
        val session = session()
        session.signIn(AuthProvider.APPLE)
        now += 14 * 60_000 + 30_000 // 30 s before expiry: inside the refresh margin
        calls.clear()
        val tokens = (1..5).map { async { session.authorized { it } } }.awaitAll()
        assertEquals(listOf("POST /auth/refresh"), calls)
        assertEquals(setOf("access-2"), tokens.toSet())
    }

    @Test
    fun a401RefreshesAndRetriesOnce() = runTest {
        val session = session()
        session.signIn(AuthProvider.APPLE)
        issued = 7 // the server no longer accepts access-1, e.g. its secret was rotated
        calls.clear()
        val account = session.authorized { access -> AuthApi(client).account(access) }
        assertEquals("u1", account.userId)
        assertEquals(listOf("GET /account", "POST /auth/refresh", "GET /account"), calls)
    }

    @Test
    fun rejectedRefreshEndsTheSessionButNetworkErrorsDoNot() = runTest {
        val session = session()
        session.signIn(AuthProvider.APPLE)
        now += 20 * 60_000
        handler = { throw IllegalStateException("offline") }
        assertEquals(AuthError.NETWORK, assertFailsWith<AuthException> { session.authorized { it } }.error)
        assertEquals(AuthState.SignedIn("u1", "a@b.c"), session.state.value, "offline keeps the session")

        handler = { req -> if (req.url.encodedPath == "/auth/refresh") respondError(HttpStatusCode.Unauthorized) else fakeServer(req) }
        assertEquals(AuthError.REJECTED, assertFailsWith<AuthException> { session.authorized { it } }.error)
        assertEquals(AuthState.SignedOut(expired = true), session.state.value)
        assertNull(store.get("session"))
    }

    @Test
    fun signOutWorksOfflineAndRevokesWhenOnline() = runTest {
        val session = session()
        session.signIn(AuthProvider.APPLE)
        calls.clear()
        session.signOut()
        assertEquals(listOf("POST /auth/logout"), calls)
        assertEquals(AuthState.SignedOut(), session.state.value)

        session.signIn(AuthProvider.APPLE)
        handler = { throw IllegalStateException("offline") }
        session.signOut()
        assertEquals(AuthState.SignedOut(), session.state.value)
        assertNull(store.get("session"))
    }

    @Test
    fun deletingTheAccountSignsOut() = runTest {
        val session = session()
        session.signIn(AuthProvider.APPLE)
        calls.clear()
        session.deleteAccount()
        assertEquals(listOf("DELETE /account"), calls)
        assertEquals(AuthState.SignedOut(), session.state.value)
    }

    @Test
    fun clientSendsJsonToTheBaseUrl() = runTest {
        handler = { req ->
            assertEquals("http://test/auth/refresh", req.url.toString())
            assertEquals(RefreshRequest("r"), req.decode(RefreshRequest.serializer()))
            json(tokens())
        }
        assertEquals("access-1", AuthApi(client).refresh("r").accessToken)
        handler = { respondError(HttpStatusCode.NotFound) }
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> { client.get("x") }
    }
}
