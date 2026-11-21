package com.himanshu.liftly.server

import com.himanshu.liftly.core.protocol.Account
import com.himanshu.liftly.core.protocol.ApiError
import com.himanshu.liftly.core.protocol.AuthTokens
import com.himanshu.liftly.core.protocol.PushAck
import com.himanshu.liftly.core.protocol.RefreshRequest
import com.himanshu.liftly.core.protocol.SignInRequest
import com.himanshu.liftly.core.protocol.SyncProtocol
import com.himanshu.liftly.core.protocol.SyncRecord
import com.himanshu.liftly.server.auth.AccessTokens
import com.himanshu.liftly.server.auth.AccountStore
import com.himanshu.liftly.server.auth.AuthService
import com.himanshu.liftly.server.auth.IdTokenVerifier
import com.himanshu.liftly.server.auth.Provider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonObject
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AuthTest {

    @BeforeTest
    fun reset() = TestDatabase.reset()

    private class TestClock(var millis: Long = System.currentTimeMillis()) : Clock() {
        override fun instant(): Instant = Instant.ofEpochMilli(millis)
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        fun advanceMinutes(minutes: Long) {
            millis += minutes * 60_000
        }
    }

    private val clock = TestClock()

    private fun withServer(
        service: AuthService = TestAuth.service(clock),
        perMinute: Int = 100,
        block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
    ) = testApplication {
        application { liftlyModule(TestDatabase.dataSource, service, devAuth = false, authRequestsPerMinute = perMinute) }
        block(createClient { install(ContentNegotiation) { json() } })
    }

    private suspend fun HttpClient.postJson(path: String, body: Any) = post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpClient.apple(token: String = TestAuth.appleToken(), nonce: String? = "nonce-1") =
        postJson("/auth/apple", SignInRequest(token, nonce))

    private suspend fun HttpClient.refresh(token: String) = postJson("/auth/refresh", RefreshRequest(token))

    private suspend fun HttpClient.account(access: String) = get("/account") { bearerAuth(access) }

    private suspend fun HttpResponse.error() = status to body<ApiError>().code

    @Test
    fun appleSignInCreatesOneAccountPerSubjectAndItsTokenSyncs() = withServer { client ->
        val tokens = client.apple().body<AuthTokens>()
        assertEquals("a@privaterelay.appleid.com", tokens.account.email)
        assertEquals(tokens.account, client.account(tokens.accessToken).body<Account>())

        val again = client.apple().body<AuthTokens>()
        assertEquals(tokens.account.userId, again.account.userId)
        val other = client.apple(TestAuth.appleToken(sub = "someone-else")).body<AuthTokens>()
        assertNotEquals(tokens.account.userId, other.account.userId)

        val pushed = client.post("/sync/${SyncProtocol.ROUTINES}/push") {
            bearerAuth(tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(listOf(SyncRecord(SyncProtocol.ROUTINES, "r1", 1, null, null, JsonObject(emptyMap()))))
        }
        assertEquals(HttpStatusCode.OK, pushed.status)
        assertEquals("r1", pushed.body<List<PushAck>>().single().id)
    }

    @Test
    fun badAppleTokensAreRejected() = withServer { client ->
        val invalid = HttpStatusCode.Unauthorized to "invalid_id_token"
        assertEquals(invalid, client.apple(nonce = "different-nonce").error())
        assertEquals(invalid, client.apple(nonce = null).error())
        assertEquals(invalid, client.apple(TestAuth.appleToken(audience = "com.someone.else")).error())
        assertEquals(invalid, client.apple(TestAuth.appleToken(expiresInSeconds = -600)).error())
        assertEquals(invalid, client.apple(TestAuth.appleToken(forged = true)).error())
        assertEquals(invalid, client.apple("not-a-jwt").error())
    }

    @Test
    fun googleKeepsOnlyVerifiedEmails() = withServer { client ->
        val verified = client.postJson("/auth/google", SignInRequest(TestAuth.googleToken())).body<AuthTokens>()
        assertEquals("g@example.com", verified.account.email)
        val unverified = client.postJson("/auth/google", SignInRequest(TestAuth.googleToken(sub = "g2", emailVerified = false)))
            .body<AuthTokens>()
        assertNull(unverified.account.email)
    }

    @Test
    fun unconfiguredProviderIsOff() {
        val noGoogle = AuthService(
            AccountStore(TestDatabase.dataSource),
            AccessTokens(TestAuth.SECRET),
            mapOf(Provider.GOOGLE to IdTokenVerifier.google(emptyList()) { null }),
        )
        withServer(service = noGoogle) { client ->
            assertEquals(
                HttpStatusCode.NotFound to "provider_disabled",
                client.postJson("/auth/google", SignInRequest(TestAuth.googleToken())).error(),
            )
        }
    }

    @Test
    fun refreshTokensRotateAndReuseAfterGraceSignsEverythingOut() = withServer { client ->
        val first = client.apple().body<AuthTokens>()
        val second = client.refresh(first.refreshToken).body<AuthTokens>()
        assertNotEquals(first.refreshToken, second.refreshToken)
        assertEquals(first.account, second.account)

        // A retry of the same refresh right away (lost response) still works.
        clock.advanceMinutes(1)
        val retried = client.refresh(first.refreshToken).body<AuthTokens>()

        // Much later, the old token showing up again looks like theft: every session ends.
        clock.advanceMinutes(5)
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(first.refreshToken).error())
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(second.refreshToken).error())
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(retried.refreshToken).error())
    }

    @Test
    fun logoutAndExpiryEndSessions() = withServer { client ->
        val a = client.apple().body<AuthTokens>()
        client.postJson("/auth/logout", RefreshRequest(a.refreshToken))
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(a.refreshToken).error())

        val b = client.apple().body<AuthTokens>()
        clock.advanceMinutes(16)
        assertEquals(HttpStatusCode.Unauthorized to "unauthorized", client.account(b.accessToken).error(), "access token expired")
        val renewed = client.refresh(b.refreshToken).body<AuthTokens>()
        assertEquals(HttpStatusCode.OK, client.account(renewed.accessToken).status)

        clock.advanceMinutes(31L * 24 * 60)
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(renewed.refreshToken).error())
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh("made-up").error())
    }

    @Test
    fun tokensSignedWithAnotherSecretAreRejected() = withServer { client ->
        val (forged, _) = AccessTokens("another-secret-another-secret-123", clock).issue("anyone")
        assertEquals(HttpStatusCode.Unauthorized to "unauthorized", client.account(forged).error())
    }

    @Test
    fun deletingTheAccountRemovesItsDataAndSessions() = withServer { client ->
        val tokens = client.apple().body<AuthTokens>()
        client.post("/sync/${SyncProtocol.ROUTINES}/push") {
            bearerAuth(tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(listOf(SyncRecord(SyncProtocol.ROUTINES, "r1", 1, null, null, JsonObject(emptyMap()))))
        }

        assertEquals(HttpStatusCode.NoContent, client.delete("/account") { bearerAuth(tokens.accessToken) }.status)

        assertEquals(0, countRows("SELECT count(*) FROM records"))
        assertEquals(0, countRows("SELECT count(*) FROM refresh_tokens"))
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.account(tokens.accessToken).error())
        assertEquals(HttpStatusCode.Unauthorized to "session_expired", client.refresh(tokens.refreshToken).error())
        // The access token is still unexpired, but writes can't recreate data for a deleted account.
        val late = client.post("/sync/${SyncProtocol.ROUTINES}/push") {
            bearerAuth(tokens.accessToken)
            contentType(ContentType.Application.Json)
            setBody(listOf(SyncRecord(SyncProtocol.ROUTINES, "r2", 2, null, null, JsonObject(emptyMap()))))
        }
        assertEquals(HttpStatusCode.Unauthorized to "unauthorized", late.error())
        // Signing in again starts a fresh, empty account.
        val fresh = client.apple().body<AuthTokens>()
        assertNotEquals(tokens.account.userId, fresh.account.userId)
    }

    @Test
    fun authEndpointsAreRateLimited() = withServer(perMinute = 2) { client ->
        client.refresh("x")
        client.refresh("x")
        assertEquals(HttpStatusCode.TooManyRequests to "rate_limited", client.refresh("x").error())
    }

    private fun countRows(sql: String): Int = TestDatabase.dataSource.connection.use { c ->
        c.createStatement().executeQuery(sql).use { rs ->
            rs.next()
            rs.getInt(1)
        }
    }
}
