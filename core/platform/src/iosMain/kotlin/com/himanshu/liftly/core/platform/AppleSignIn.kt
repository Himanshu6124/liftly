package com.himanshu.liftly.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIView
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Result callback for the Swift sign-in bridges; exactly one method is called, once. */
interface SignInCallback {
    /** Apple: [rawNonce] is the value whose SHA-256 went into the request (the token's `nonce`). Google: null. */
    fun onSuccess(idToken: String, rawNonce: String?)
    fun onFailure(cancelled: Boolean, message: String?)
}

/**
 * AuthenticationServices is Swift-first, so the iOS app implements this (`AppleSignIn.swift`) and
 * registers it at startup: `AppleSignInBridgeHolder.shared.delegate = AppleSignIn()`.
 */
interface AppleSignInBridge {
    fun signIn(callback: SignInCallback)

    /** The system "Sign in with Apple" button (required look); [onTap] runs on the main thread. */
    fun makeButton(dark: Boolean, onTap: () -> Unit): UIView
}

object AppleSignInBridgeHolder {
    var delegate: AppleSignInBridge? = null
}

/**
 * Google sign-in on iOS (`GoogleSignIn.swift`: OAuth with PKCE in an `ASWebAuthenticationSession`).
 * [isConfigured] is false until an iOS OAuth client id is set (`GOOGLE_IOS_CLIENT_ID`).
 */
interface GoogleSignInBridge {
    val isConfigured: Boolean
    fun signIn(callback: SignInCallback)
}

object GoogleSignInBridgeHolder {
    var delegate: GoogleSignInBridge? = null
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
    // Apple first: App Store rules want it at least as prominent as other providers.
    override val providers: List<SignInProvider>
        get() = listOfNotNull(
            SignInProvider.APPLE.takeIf { AppleSignInBridgeHolder.delegate != null },
            SignInProvider.GOOGLE.takeIf { GoogleSignInBridgeHolder.delegate?.isConfigured == true },
        )

    override suspend fun signIn(provider: SignInProvider): SignInCredential {
        val start: ((SignInCallback) -> Unit)? = when (provider) {
            SignInProvider.APPLE -> AppleSignInBridgeHolder.delegate?.let { it::signIn }
            SignInProvider.GOOGLE -> GoogleSignInBridgeHolder.delegate?.takeIf { it.isConfigured }?.let { it::signIn }
        }
        requireNotNull(start) { "$provider sign-in is not available" }
        return suspendCancellableCoroutine { cont ->
            start(
                object : SignInCallback {
                    override fun onSuccess(idToken: String, rawNonce: String?) {
                        cont.resume(SignInCredential(provider, idToken, rawNonce))
                    }

                    override fun onFailure(cancelled: Boolean, message: String?) {
                        cont.resumeWithException(if (cancelled) SignInCancelledException() else IllegalStateException(message))
                    }
                },
            )
        }
    }
}
