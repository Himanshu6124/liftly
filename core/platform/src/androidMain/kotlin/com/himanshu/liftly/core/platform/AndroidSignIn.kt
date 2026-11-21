package com.himanshu.liftly.core.platform

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.lang.ref.WeakReference

/**
 * Google sign-in through Credential Manager. Needs the current Activity (the sheet is shown over
 * it), which `MainActivity` registers, and our server's Google OAuth **Web** client id
 * ([serverClientId], from the `liftly.googleServerClientId` Gradle property). Without the client
 * id Google sign-in is hidden.
 */
object AndroidSignIn {
    var serverClientId: String = ""
    private var activity: WeakReference<Activity>? = null

    fun register(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun unregister(activity: Activity) {
        if (this.activity?.get() === activity) this.activity = null
    }

    internal fun currentActivity(): Activity? = activity?.get()
}

internal class AndroidNativeSignIn(private val context: Context) : NativeSignIn {
    override val providers: List<SignInProvider>
        get() = if (AndroidSignIn.serverClientId.isNotBlank()) listOf(SignInProvider.GOOGLE) else emptyList()

    override suspend fun signIn(provider: SignInProvider): SignInCredential {
        require(provider == SignInProvider.GOOGLE && AndroidSignIn.serverClientId.isNotBlank()) { "$provider sign-in is not available" }
        val activity = checkNotNull(AndroidSignIn.currentActivity()) { "No activity to show sign-in" }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(AndroidSignIn.serverClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(context).getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return SignInCredential(SignInProvider.GOOGLE, idToken, rawNonce = null)
    }
}
