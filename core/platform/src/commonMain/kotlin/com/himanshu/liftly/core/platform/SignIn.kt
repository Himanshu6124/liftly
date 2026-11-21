package com.himanshu.liftly.core.platform

/** Identity providers the OS offers a native sign-in sheet for. */
enum class SignInProvider { APPLE, GOOGLE }

/** An OpenID Connect ID token from the provider, to be verified by our server. */
data class SignInCredential(val provider: SignInProvider, val idToken: String, val rawNonce: String?)

/** The user closed the sign-in sheet. */
class SignInCancelledException : Exception("Sign-in cancelled")

/** Native "Sign in with Apple" (iOS) and Credential Manager Google sign-in (Android). */
interface NativeSignIn {
    val providers: List<SignInProvider>

    /** @throws SignInCancelledException when dismissed; any other exception means it failed. */
    suspend fun signIn(provider: SignInProvider): SignInCredential
}

object NoNativeSignIn : NativeSignIn {
    override val providers: List<SignInProvider> = emptyList()
    override suspend fun signIn(
        provider: SignInProvider,
    ): SignInCredential = throw UnsupportedOperationException("No sign-in on this platform")
}

/**
 * Small secrets (the session tokens): Keychain on iOS, Keystore-encrypted preferences on Android.
 * Calls are synchronous and cheap.
 */
interface SecureStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

/** Desktop and tests: kept in memory only, so desktop sessions end when the app quits. */
class InMemorySecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) {
        values[key] = value
    }
    override fun remove(key: String) {
        values.remove(key)
    }
}
