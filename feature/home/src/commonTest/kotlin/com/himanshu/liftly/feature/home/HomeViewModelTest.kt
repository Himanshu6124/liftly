package com.himanshu.liftly.feature.home

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val sync = object : SyncController {
        val flow = MutableStateFlow<SyncState>(SyncState.LocalOnly)
        override val state = flow
        override fun requestSync() = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun mapsSyncStateToLabel() = runTest {
        val vm = HomeViewModel(sync)
        vm.state.test {
            assertEquals(SyncLabel.LOCAL_ONLY, awaitItem().syncLabel)
            sync.flow.value = SyncState.Syncing
            assertEquals(SyncLabel.SYNCING, awaitItem().syncLabel)
        }
    }

    @Test
    fun startEmptyWorkoutEmitsNavigationEffect() = runTest {
        val vm = HomeViewModel(sync)
        vm.effects.test {
            vm.onIntent(HomeIntent.StartEmptyWorkout)
            assertEquals(HomeEffect.OpenActiveWorkout, awaitItem())
        }
    }
}
