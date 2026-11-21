package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.platform.NativeSignIn
import com.himanshu.liftly.core.platform.SecureStore
import com.himanshu.liftly.core.platform.SignInCancelledException
import com.himanshu.liftly.core.platform.SignInProvider
import com.himanshu.liftly.core.protocol.AuthTokens
import com.himanshu.liftly.core.protocol.SignInRequest
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * The signed-in session: tokens kept in [SecureStore], access token refreshed shortly before it
 * expires or after a 401. Refresh is single-flight (one at a time, behind a mutex), because the
 * server signs out every session when an old refresh token is reused.
 *
 * Only a server answer of 401 ends the session; network errors keep it, so being offline at the
 * gym never signs anyone out.
 */
class AuthSession(
    private val api: AuthApi,
    private val store: SecureStore,
    private val nativeSignIn: NativeSignIn,
    private val now: () -> Long,
) : AuthRepository {
    private val mutex = Mutex()
    private var tokens: AuthTokens? = load()
    private val _state = MutableStateFlow(tokens.toState())
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override val providers: List<AuthProvider>
        get() = nativeSignIn.providers.map { if (it == SignInProvider.APPLE) AuthProvider.APPLE else AuthProvider.GOOGLE }

    override suspend fun signIn(provider: AuthProvider) {
        val native = if (provider == AuthProvider.APPLE) SignInProvider.APPLE else SignInProvider.GOOGLE
        val credential = try {
            nativeSignIn.signIn(native)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SignInCancelledException) {
            throw AuthException(AuthError.CANCELLED, cause = e)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw AuthException(AuthError.UNAVAILABLE, e.message, e)
        }
        val signedIn = guard { api.signIn(provider.name.lowercase(), SignInRequest(credential.idToken, credential.rawNonce)) }
        mutex.withLock { save(signedIn) }
    }

    override suspend fun signOut() {
        val refresh = mutex.withLock { tokens?.refreshToken.also { save(null) } } ?: return
        // Best effort: if offline, the refresh token just expires unused on the server.
        runCatching { api.logout(refresh) }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun deleteAccount() {
        authorized { access -> api.deleteAccount(access) }
        mutex.withLock { save(null) }
    }

    /**
     * Runs [call] with a valid access token, refreshing once and retrying on 401.
     * @throws AuthException NETWORK (try later) or REJECTED (signed out now: sign in again).
     */
    suspend fun <T> authorized(call: suspend (accessToken: String) -> T): T {
        val access = accessToken(stale = null)
        return try {
            guard { call(access) }
        } catch (e: AuthException) {
            if (e.error != AuthError.REJECTED) throw e
            val renewed = accessToken(stale = access)
            guard { call(renewed) } // a second 401 propagates as REJECTED
        }
    }

    /** A usable access token. [stale] is one the server just rejected: refresh unless already replaced. */
    private suspend fun accessToken(stale: String?): String = mutex.withLock {
        val current = tokens ?: throw AuthException(AuthError.REJECTED, "Not signed in")
        val fresh = current.accessToken != stale && current.accessExpiresAt - REFRESH_MARGIN_MILLIS > now()
        if (fresh) return@withLock current.accessToken
        val renewed = try {
            guard { api.refresh(current.refreshToken) }
        } catch (e: AuthException) {
            if (e.error == AuthError.REJECTED) save(null, expired = true)
            throw e
        }
        save(renewed)
        renewed.accessToken
    }

    private fun save(value: AuthTokens?, expired: Boolean = false) {
        tokens = value
        if (value == null) store.remove(KEY) else store.put(KEY, json.encodeToString(AuthTokens.serializer(), value))
        _state.value = if (value == null) AuthState.SignedOut(expired) else value.toState()
    }

    private fun load(): AuthTokens? = store.get(KEY)?.let { runCatching { json.decodeFromString(AuthTokens.serializer(), it) }.getOrNull() }

    private fun AuthTokens?.toState(): AuthState = if (this ==
        null
    ) {
        AuthState.SignedOut()
    } else {
        AuthState.SignedIn(account.userId, account.email)
    }

    private companion object {
        const val KEY = "session"
        const val REFRESH_MARGIN_MILLIS = 60_000L
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** Maps server failures to [AuthError]: 401/403 → REJECTED, 404 (provider off) → UNAVAILABLE, else NETWORK. */
internal suspend fun <T> guard(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: AuthException) {
    throw e
} catch (e: ResponseException) {
    val error = when (e.response.status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AuthError.REJECTED
        HttpStatusCode.NotFound -> AuthError.UNAVAILABLE
        else -> AuthError.NETWORK
    }
    throw AuthException(error, "HTTP ${e.response.status.value}", e)
} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
    throw AuthException(AuthError.NETWORK, e.message, e)
}
