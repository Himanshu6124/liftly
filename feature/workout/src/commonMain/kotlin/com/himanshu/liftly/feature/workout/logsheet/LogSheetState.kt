package com.himanshu.liftly.feature.workout.logsheet

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase
import com.himanshu.liftly.core.domain.usecase.SetValidation
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.Units
import com.himanshu.liftly.core.domain.util.toDraftSets
import com.himanshu.liftly.core.platform.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Instant

enum class LogSheetError { WEIGHT_INVALID, WEIGHT_RANGE, REPS_INVALID, REPS_RANGE, REPS_MISSING }

@Immutable
data class LogSheetUiState(
    val isLoading: Boolean = true,
    val exerciseId: String = "",
    val exerciseName: String = "",
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val unit: WeightUnit = WeightUnit.KG,
    /** Weight step in the display unit (2.5 kg / 5 lb by default). */
    val increment: Double = 2.5,
    /** "77.5 kg × 8, 8, 8, 7" from the last session, shown in the header. */
    val lastHint: String? = null,
    val lastPerformedAt: Instant? = null,
    val inputMode: InputMode = InputMode.UNIFORM,
    val weightText: String = "",
    val setCount: Int = 3,
    val repsText: String = "",
    /** Warm-ups and other set types from the prefill are kept as-is in UNIFORM mode (edited in Milestone 5). */
    val extraSets: List<DraftSet> = emptyList(),
    val weightError: LogSheetError? = null,
    val repsError: LogSheetError? = null,
    val isSaving: Boolean = false,
) {
    val showsWeight: Boolean get() = trackingType == TrackingType.WEIGHT_REPS
    val canSave: Boolean get() = !isLoading && !isSaving && weightError == null && repsError == null && repsText.isNotBlank()
}

/**
 * Draft holder for the Batch Log Sheet. Everything stays in memory until [save];
 * closing the sheet discards the draft.
 */
class LogSheetState(
    private val workoutExerciseId: String,
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository,
    private val prefill: GetLogSheetPrefillUseCase,
    private val parseReps: ParseRepInputUseCase,
    private val logSets: LogExerciseSetsUseCase,
    private val haptics: Haptics,
    private val scope: CoroutineScope,
) {
    /**
     * The text fields own their text ([TextFieldState]) so every keystroke is applied immediately,
     * even inside a bottom sheet (a separate window on iOS). Everything else derives from them.
     */
    val weightField = TextFieldState()
    val repsField = TextFieldState()

    /** Non-text state; the public [state] combines it with the current field text. */
    private var base: LogSheetUiState by mutableStateOf(LogSheetUiState())

    val state: LogSheetUiState
        get() {
            val reps = repsField.text.toString()
            // Typing "8 8 7 6" (or "8x4") sets the set count to 4.
            val parsedCount = (parseReps(reps) as? ParseRepInputUseCase.Result.Valid)?.reps?.size
            return base.copy(
                weightText = weightField.text.toString(),
                repsText = reps,
                setCount = parsedCount ?: base.setCount,
            ).validated()
        }

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val data = workouts.getLogSheetData(workoutExerciseId) ?: return
        val userSettings = settings.settings.first()
        val unit = userSettings.weightUnit
        val p = prefill(data, userSettings.defaultSetCount)
        val working = p.sets.filter { it.setType != SetType.WARMUP }.ifEmpty { p.sets }
        val extra = p.sets - working.toSet()
        val lastSets = data.lastSession?.sets?.toDraftSets().orEmpty()
        // UNIFORM shows one weight: the heaviest working weight of the prefill (top set).
        val weightKg = working.mapNotNull { it.weightKg }.maxOrNull()
        weightField.setTextAndPlaceCursorAtEnd(weightKg?.let { SetFormatter.number(it, unit) }.orEmpty())
        repsField.setTextAndPlaceCursorAtEnd(ParseRepInputUseCase.format(working.map { it.reps }))
        base = LogSheetUiState(
            isLoading = false,
            exerciseId = data.exercise.id,
            exerciseName = data.exercise.name,
            trackingType = data.exercise.trackingType,
            unit = unit,
            increment = if (unit == WeightUnit.KG) userSettings.weightIncrementKg else userSettings.weightIncrementLb,
            lastHint = SetFormatter.hint(lastSets, unit),
            lastPerformedAt = data.lastSession?.performedAt,
            inputMode = p.inputMode,
            setCount = working.size,
            extraSets = extra,
        )
    }

    /** Programmatic text change (tests, paste-like updates). The UI edits [weightField] directly. */
    fun onWeightChanged(text: String) = weightField.setTextAndPlaceCursorAtEnd(text)

    fun onRepsChanged(text: String) = repsField.setTextAndPlaceCursorAtEnd(text)

    fun nudgeWeight(direction: Int) {
        val s = state
        val current = parseWeight(s.weightText) ?: 0.0
        val next = Units.roundToIncrement(current + direction * s.increment, s.increment).coerceAtLeast(0.0)
        weightField.setTextAndPlaceCursorAtEnd(formatDisplay(next))
    }

    fun changeSetCount(delta: Int) {
        val s = state
        val count = (s.setCount + delta).coerceIn(1, ParseRepInputUseCase.MAX_SETS)
        if (count == s.setCount) return
        val reps = (parseReps(s.repsText) as? ParseRepInputUseCase.Result.Valid)?.reps.orEmpty()
        // Pad with the last value, or trim.
        val adjusted = when {
            reps.isEmpty() -> emptyList()
            count > reps.size -> reps + List(count - reps.size) { reps.last() }
            else -> reps.take(count)
        }
        base = base.copy(setCount = count)
        repsField.setTextAndPlaceCursorAtEnd(ParseRepInputUseCase.format(adjusted))
    }

    /** Persists the draft. [onSaved] gets the summary line and an undo snapshot. */
    fun save(onSaved: (summary: String?, undo: LogSnapshot) -> Unit) {
        val s = state
        if (!s.canSave) return
        val sets = s.extraSets + draftSets(s)
        base = base.copy(isSaving = true)
        scope.launch {
            when (val result = logSets(workoutExerciseId, sets, s.inputMode)) {
                is LogExerciseSetsUseCase.Result.Saved -> {
                    haptics.light()
                    onSaved(SetFormatter.summary(sets, s.unit), result.undo)
                }

                is LogExerciseSetsUseCase.Result.Invalid -> base = base.copy(isSaving = false)
            }
        }
    }

    internal fun draftSets(s: LogSheetUiState = state): List<DraftSet> {
        val reps = (parseReps(s.repsText) as? ParseRepInputUseCase.Result.Valid)?.reps.orEmpty()
        val weightKg = if (s.showsWeight) parseWeight(s.weightText)?.let { Units.displayToKg(it, s.unit) } else null
        return reps.map { DraftSet(weightKg = weightKg, reps = it) }
    }

    private fun LogSheetUiState.validated(): LogSheetUiState {
        val weight = parseWeight(weightText)
        val weightError = when {
            !showsWeight || weightText.isBlank() -> null
            weight == null -> LogSheetError.WEIGHT_INVALID
            Units.displayToKg(weight, unit) > SetValidation.MAX_WEIGHT_KG -> LogSheetError.WEIGHT_RANGE
            else -> null
        }
        val repsError = when (val r = parseReps(repsText)) {
            is ParseRepInputUseCase.Result.Valid, ParseRepInputUseCase.Result.Empty -> null

            is ParseRepInputUseCase.Result.Invalid -> when (r.reason) {
                ParseRepInputUseCase.Reason.NOT_A_NUMBER -> LogSheetError.REPS_INVALID
                else -> LogSheetError.REPS_RANGE
            }
        }
        return copy(weightError = weightError, repsError = repsError)
    }

    private fun parseWeight(text: String): Double? = text.replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    private fun formatDisplay(value: Double): String {
        val rounded = kotlin.math.round(value * 100) / 100
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
