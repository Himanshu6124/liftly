package com.himanshu.liftly.feature.settings

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.ThemeMode
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
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
class SettingsViewModelTest {
    private val repo = object : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings())
        override suspend fun update(transform: (UserSettings) -> UserSettings) {
            settings.value = transform(settings.value)
        }
        override suspend fun seedVersion() = 1
        override suspend fun setSeedVersion(version: Int) = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun updatesArePersisted() = runTest {
        val vm = SettingsViewModel(repo)
        vm.settings.test {
            awaitItem()
            vm.update {
                it.copy(
                    weightUnit = WeightUnit.LB,
                    weightIncrementLb = 10.0,
                    themeMode = ThemeMode.DARK,
                    openNextExerciseAutomatically = true,
                )
            }
            val s = expectMostRecentItem()
            assertEquals(WeightUnit.LB, s.weightUnit)
            assertEquals(10.0, s.weightIncrementLb)
            assertEquals(ThemeMode.DARK, s.themeMode)
            assertEquals(true, s.openNextExerciseAutomatically)
            assertEquals(2.5, s.weightIncrementKg, "kg step untouched")
        }
    }
}
