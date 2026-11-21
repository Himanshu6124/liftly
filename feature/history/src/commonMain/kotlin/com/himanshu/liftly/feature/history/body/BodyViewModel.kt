package com.himanshu.liftly.feature.history.body

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.BodyMeasurement
import com.himanshu.liftly.core.domain.model.MeasurementType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.MeasurementRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.Measurements
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** One entry, already in the display unit. */
@Immutable
data class BodyEntryUi(val id: String, val value: Double, val measuredAt: Instant)

@Immutable
data class BodyUiState(
    val isLoading: Boolean = true,
    val type: MeasurementType = MeasurementType.BODY_WEIGHT,
    /** Types with entries; shown first in the picker. */
    val usedTypes: ImmutableSet<MeasurementType> = persistentSetOf(),
    /** Oldest first (chart order). */
    val entries: ImmutableList<BodyEntryUi> = persistentListOf(),
    /** "kg", "lb", "cm", "in" or "%". */
    val symbol: String = "kg",
) {
    val latest: BodyEntryUi? get() = entries.lastOrNull()

    /** Latest minus first, in the display unit; null with fewer than 2 entries. */
    val change: Double? get() = if (entries.size < 2) null else entries.last().value - entries.first().value
}

sealed interface BodyEffect {
    data object Deleted : BodyEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModel(
    private val measurements: MeasurementRepository,
    settings: SettingsRepository,
    private val appScope: AppScope,
) : ViewModel() {
    private val type = MutableStateFlow(MeasurementType.BODY_WEIGHT)
    private var unit = WeightUnit.KG
    private var lastDeleted: BodyMeasurement? = null

    private val _effects = Channel<BodyEffect>(Channel.BUFFERED)
    val effects: Flow<BodyEffect> = _effects.receiveAsFlow()

    val state: StateFlow<BodyUiState> = combine(
        type.flatMapLatest { t -> measurements.observe(t) },
        type,
        measurements.observeUsedTypes(),
        settings.settings,
    ) { list, t, used, s ->
        unit = s.weightUnit
        BodyUiState(
            isLoading = false,
            type = t,
            usedTypes = used.toImmutableSet(),
            entries = list.filter { it.type == t }
                .map { BodyEntryUi(it.id, Measurements.toDisplay(it.value, t.kind, s.weightUnit), it.measuredAt) }
                .toImmutableList(),
            symbol = Measurements.symbol(t.kind, s.weightUnit),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    fun select(type: MeasurementType) {
        this.type.value = type
    }

    /** Logs [text] (display unit) for the selected type; false when it isn't a plausible number. */
    fun add(text: String): Boolean {
        val t = type.value
        val value = text.replace(',', '.').trim().toDoubleOrNull()?.takeIf { Measurements.isPlausible(it, t.kind) } ?: return false
        appScope.launch { measurements.add(t, Measurements.fromDisplay(value, t.kind, unit)) }
        return true
    }

    fun delete(id: String) = viewModelScope.launch {
        lastDeleted = appScope.async { measurements.delete(id) }.await() ?: return@launch
        _effects.send(BodyEffect.Deleted)
    }

    fun undoDelete() {
        val m = lastDeleted ?: return
        lastDeleted = null
        appScope.launch { measurements.restore(m) }
    }
}
