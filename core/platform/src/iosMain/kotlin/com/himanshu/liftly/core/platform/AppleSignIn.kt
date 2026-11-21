package com.himanshu.liftly.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIView
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Result callback for [AppleSignInBridge]; exactly one method is called once. */
interface AppleSignInCallback {
    /** [rawNonce] is the value whose SHA-256 went into the Apple request (the token's `nonce`). */
    fun onSuccess(idToken: String, rawNonce: String)
    fun onFailure(cancelled: Boolean, message: String?)
}

/**
 * AuthenticationServices is Swift-first, so the iOS app implements this (`AppleSignIn.swift`) and
 * registers it at startup: `AppleSignInBridgeHolder.shared.delegate = AppleSignIn()`.
 */
interface AppleSignInBridge {
    fun signIn(callback: AppleSignInCallback)

    /** The system "Sign in with Apple" button (required look); [onTap] runs on the main thread. */
    fun makeButton(dark: Boolean, onTap: () -> Unit): UIView
}

object AppleSignInBridgeHolder {
    var delegate: AppleSignInBridge? = null
}

/**
 * Keychain access is simpler from Swift, so the iOS app registers its implementation at startup
 * (`SecureStoreHolder.shared.delegate = KeychainStore()`), before Koin starts.
 */
object SecureStoreHolder : SecureStore {
    var delegate: SecureStore? = null

    override fun get(key: String): String? = delegate?.get(key)
    override fun put(key: String, value: String) {
        delegate?.put(key, value)
    }
    override fun remove(key: String) {
        delegate?.remove(key)
    }
}

internal class IosNativeSignIn : NativeSignIn {
    override val providers: List<SignInProvider>
        get() = if (AppleSignInBridgeHolder.delegate != null) listOf(SignInProvider.APPLE) else emptyList()

    override suspend fun signIn(provider: SignInProvider): SignInCredential {
        val bridge = AppleSignInBridgeHolder.delegate
        require(provider == SignInProvider.APPLE && bridge != null) { "$provider sign-in is not available on iOS" }
        return suspendCancellableCoroutine { cont ->
            bridge.signIn(
                object : AppleSignInCallback {
                    override fun onSuccess(idToken: String, rawNonce: String) {
                        cont.resume(SignInCredential(SignInProvider.APPLE, idToken, rawNonce))
                    }

                    override fun onFailure(cancelled: Boolean, message: String?) {
                        cont.resumeWithException(if (cancelled) SignInCancelledException() else IllegalStateException(message))
                    }
                },
            )
        }
    }
}
