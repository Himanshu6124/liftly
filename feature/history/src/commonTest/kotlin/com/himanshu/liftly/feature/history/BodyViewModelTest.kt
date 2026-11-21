package com.himanshu.liftly.feature.history

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.BodyMeasurement
import com.himanshu.liftly.core.domain.model.MeasurementType
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.MeasurementRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.feature.history.body.BodyEffect
import com.himanshu.liftly.feature.history.body.BodyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.round
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class FakeMeasurements : MeasurementRepository {
        val all = MutableStateFlow<List<BodyMeasurement>>(emptyList())
        private var n = 0
        override fun observe(type: MeasurementType) = all.map { list -> list.filter { it.type == type } }
        override fun observeUsedTypes() = all.map { list -> list.map { it.type }.toSet() }
        override suspend fun add(type: MeasurementType, value: Double): String {
            val id = "m${n++}"
            all.update { it + BodyMeasurement(id, type, value, Instant.fromEpochMilliseconds(n * 1000L)) }
            return id
        }
        override suspend fun delete(id: String) = all.value.find { it.id == id }?.also { m -> all.update { it - m } }
        override suspend fun restore(measurement: BodyMeasurement) = all.update { (it + measurement).sortedBy { m -> m.measuredAt } }
    }

    private class FakeSettings(unit: WeightUnit) : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings(weightUnit = unit))
        override suspend fun update(transform: (UserSettings) -> UserSettings) = Unit
        override suspend fun seedVersion() = 1
        override suspend fun setSeedVersion(version: Int) = Unit
    }

    private val repo = FakeMeasurements()

    private fun vm(unit: WeightUnit = WeightUnit.KG) = BodyViewModel(repo, FakeSettings(unit), AppScope(CoroutineScope(dispatcher)))

    @Test
    fun logsEntriesAndShowsChangeInTheDisplayUnit() = runTest(dispatcher) {
        val vm = vm(WeightUnit.LB)
        vm.state.test {
            awaitItem()
            assertTrue(vm.add("180"))
            assertTrue(vm.add("176,5")) // comma decimals work
            val s = expectMostRecentItem()
            assertEquals(listOf(180.0, 176.5), s.entries.map { round(it.value * 10) / 10 })
            assertEquals(-3.5, round(s.change!! * 10) / 10)
            assertEquals("lb", s.symbol)
        }
        // Stored in kg.
        assertEquals(81.6, round(repo.all.value.first().value * 10) / 10)
    }

    @Test
    fun rejectsImplausibleValues() = runTest(dispatcher) {
        val vm = vm()
        vm.select(MeasurementType.BODY_FAT)
        assertFalse(vm.add("185"), "185 % is a typo")
        assertFalse(vm.add("abc"))
        assertTrue(repo.all.value.isEmpty())
    }

    @Test
    fun deleteOffersUndo() = runTest(dispatcher) {
        val vm = vm()
        vm.add("80")
        val id = repo.all.value.single().id
        vm.effects.test {
            vm.delete(id)
            assertEquals(BodyEffect.Deleted, awaitItem())
        }
        assertTrue(repo.all.value.isEmpty())
        vm.undoDelete()
        assertEquals(listOf(80.0), repo.all.value.map { it.value })
    }
}
