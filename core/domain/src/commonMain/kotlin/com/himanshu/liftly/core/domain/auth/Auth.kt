package com.himanshu.liftly.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** Optional account for backup and sync. The app works fully while signed out. */
enum class AuthProvider { APPLE, GOOGLE }

sealed interface AuthState {
    /** [expired] when the server ended the session (sign in again), not when the user signed out. */
    data class SignedOut(val expired: Boolean = false) : AuthState

    data class SignedIn(val userId: String, val email: String?) : AuthState
}

/** Why a sign-in or account call didn't work. Cancelling the system sheet is not an error. */
enum class AuthError { CANCELLED, NETWORK, REJECTED, UNAVAILABLE }

class AuthException(val error: AuthError, message: String? = null, cause: Throwable? = null) : Exception(message ?: error.name, cause)

interface AuthRepository {
    val state: StateFlow<AuthState>

    /** Providers this device can use (Apple on iOS, Google on Android when configured). */
    val providers: List<AuthProvider>

    /** Shows the system sign-in sheet, then signs in with the server. @throws AuthException */
    suspend fun signIn(provider: AuthProvider)

    /** Ends this device's session. Never fails: local tokens are cleared even when offline. */
    suspend fun signOut()

    /** Deletes the account and all its data on the server, then signs out. @throws AuthException */
    suspend fun deleteAccount()
}
