package com.himanshu.liftly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.domain.util.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUi(
    val state: AuthState = AuthState.SignedOut(),
    val providers: List<AuthProvider> = emptyList(),
    /** A sign-in, sign-out or deletion is running; buttons are disabled. */
    val busy: Boolean = false,
    /** Last failure to show; never [AuthError.CANCELLED]. */
    val error: AuthError? = null,
)

/** The optional account at the top of Settings. Calls run in [AppScope] so leaving the tab doesn't cancel them. */
class AccountViewModel(private val auth: AuthRepository, private val appScope: AppScope) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<AuthError?>(null)

    val ui: StateFlow<AccountUi> = combine(auth.state, busy, error) { state, b, e -> AccountUi(state, auth.providers, b, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountUi(auth.state.value, auth.providers))

    fun signIn(provider: AuthProvider) = run { auth.signIn(provider) }

    fun signOut() = run { auth.signOut() }

    fun deleteAccount() = run { auth.deleteAccount() }

    private fun run(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        appScope.launch {
            try {
                block()
            } catch (e: AuthException) {
                if (e.error != AuthError.CANCELLED) error.value = e.error
            } finally {
                busy.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
