package com.himanshu.liftly.feature.settings

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthException
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.domain.util.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private class FakeAuth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut())
        override val providers = listOf(AuthProvider.APPLE)
        var failWith: AuthError? = null

        override suspend fun signIn(provider: AuthProvider) {
            failWith?.let { throw AuthException(it) }
            state.value = AuthState.SignedIn("u1", "a@b.c")
        }

        override suspend fun signOut() {
            state.value = AuthState.SignedOut()
        }

        override suspend fun deleteAccount() {
            failWith?.let { throw AuthException(it) }
            state.value = AuthState.SignedOut()
        }
    }

    private val auth = FakeAuth()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun signInSignOutAndDelete() = runTest(UnconfinedTestDispatcher()) {
        val vm = AccountViewModel(auth, AppScope(this))
        vm.ui.test {
            assertEquals(AccountUi(AuthState.SignedOut(), listOf(AuthProvider.APPLE)), awaitItem())
            vm.signIn(AuthProvider.APPLE)
            assertEquals(AuthState.SignedIn("u1", "a@b.c"), expectMostRecentItem().state)
            vm.deleteAccount()
            val ui = expectMostRecentItem()
            assertEquals(AuthState.SignedOut(), ui.state)
            assertEquals(false, ui.busy)
        }
    }

    @Test
    fun errorsAreShownButCancellingIsNot() = runTest(UnconfinedTestDispatcher()) {
        val vm = AccountViewModel(auth, AppScope(this))
        vm.ui.test {
            awaitItem()
            auth.failWith = AuthError.CANCELLED
            vm.signIn(AuthProvider.APPLE)
            assertNull(expectMostRecentItem().error)
            auth.failWith = AuthError.NETWORK
            vm.signIn(AuthProvider.APPLE)
            assertEquals(AuthError.NETWORK, expectMostRecentItem().error)
            auth.failWith = null
            vm.signIn(AuthProvider.APPLE)
            assertNull(expectMostRecentItem().error, "cleared on the next attempt")
        }
    }
}
