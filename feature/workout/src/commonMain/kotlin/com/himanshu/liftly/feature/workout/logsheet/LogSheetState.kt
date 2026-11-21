package com.himanshu.liftly.feature.workout.logsheet

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.usecase.ParseDurationInputUseCase
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase
import com.himanshu.liftly.core.domain.usecase.PrHit
import com.himanshu.liftly.core.domain.usecase.SetValidation
import com.himanshu.liftly.core.domain.util.PlateCalculator
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.Units
import com.himanshu.liftly.core.domain.util.WarmupGenerator
import com.himanshu.liftly.core.domain.util.toDraftSets
import com.himanshu.liftly.core.platform.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.time.Instant

enum class LogSheetError { WEIGHT_INVALID, WEIGHT_RANGE, REPS_INVALID, REPS_RANGE, TIME_INVALID, DISTANCE_INVALID, RPE_INVALID }

/** Read-only snapshot of everything the sheet shows besides the editable text. */
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
    val setCount: Int = 3,
    val weightText: String = "",
    val repsText: String = "",
    val weightError: LogSheetError? = null,
    val repsError: LogSheetError? = null,
    val otherError: LogSheetError? = null,
    val hasSets: Boolean = false,
    val isSaving: Boolean = false,
    /** Bar + plates (display unit) when the exercise uses a barbell; drives the plate hint and warm-up rounding. */
    val barbell: WarmupGenerator.Barbell? = null,
    /** How to load the bar for the uniform weight; null when not a barbell or no weight entered. */
    val loadout: PlateCalculator.Loadout? = null,
) {
    val showsWeight: Boolean get() = trackingType == TrackingType.WEIGHT_REPS
    val canSave: Boolean get() = !isLoading && !isSaving && hasSets && weightError == null && repsError == null && otherError == null
}

/** One editable set row (per-set mode and warm-ups). Text lives in [TextFieldState]s. */
@Stable
class SetRowState(val key: Long, type: SetType, weight: String = "", reps: String = "", rpe: String = "") {
    var type by mutableStateOf(type)
    val weight = TextFieldState(weight)
    val reps = TextFieldState(reps)
    val rpe = TextFieldState(rpe)
}

/**
 * Draft holder for the Batch Log Sheet. Everything stays in memory until [save]; closing the
 * sheet discards the draft. Text fields own their text ([TextFieldState]) so every keystroke is
 * applied immediately, even inside a bottom sheet (a separate window on iOS).
 */
@Suppress("TooManyFunctions")
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
    private val parseDurations = ParseDurationInputUseCase()

    // UNIFORM working sets
    val weightField = TextFieldState()
    val repsField = TextFieldState()

    /** RPE for all working sets: one value, or one per set ("8 8.5 9"). */
    val rpeField = TextFieldState()

    // TIME: one duration per set ("1:00 0:45"); DISTANCE: metres + optional time.
    val durationField = TextFieldState()
    val distanceField = TextFieldState()
    val distanceTimeField = TextFieldState()
    val noteField = TextFieldState()

    /** PER_SET working rows. */
    val rows = mutableStateListOf<SetRowState>()

    /** Warm-up rows (both modes); saved as [SetType.WARMUP], excluded from PRs and volume. */
    val warmups = mutableStateListOf<SetRowState>()

    var showMore by mutableStateOf(false)
    private var base: LogSheetUiState by mutableStateOf(LogSheetUiState())
    private var nextKey = 0L

    val state: LogSheetUiState
        get() {
            val reps = repsField.text.toString()
            val count = when (base.trackingType) {
                TrackingType.TIME -> (
                    parseDurations(
                        durationField.text.toString(),
                    ) as? ParseDurationInputUseCase.Result.Valid
                    )?.seconds?.size

                TrackingType.DISTANCE -> 1

                else -> if (base.inputMode == InputMode.PER_SET) rows.size else parsedReps(reps)?.size
            }
            val weightText = weightField.text.toString()
            val s = base.copy(
                weightText = weightText,
                repsText = reps,
                setCount = count ?: base.setCount,
                loadout = base.barbell?.takeIf { base.inputMode == InputMode.UNIFORM }?.let { bar ->
                    parseNumber(weightText)?.takeIf { it > 0 }?.let { PlateCalculator.loadout(it, bar.bar, bar.plates) }
                },
            )
            return s.copy(
                weightError = weightError(s),
                repsError = repsError(s),
                otherError = otherError(s),
                hasSets = buildSets(s).any { it.setType != SetType.WARMUP },
            )
        }

    /** Called from a LaunchedEffect (after composition commits), not from init: no side effects in composition. */
    suspend fun load() {
        if (!base.isLoading) return
        val data = workouts.getLogSheetData(workoutExerciseId) ?: return
        val userSettings = settings.settings.first()
        val unit = userSettings.weightUnit
        val p = prefill(data, userSettings.defaultSetCount)
        val warm = p.sets.filter { it.setType == SetType.WARMUP }
        val working = p.sets.filter { it.setType != SetType.WARMUP }.ifEmpty { listOf(DraftSet()) }

        warm.forEach { warmups += row(it, unit) }
        fillWorkingFields(data.exercise.trackingType, working, p.inputMode, unit)
        data.workoutExercise.notes?.let(noteField::setTextAndPlaceCursorAtEnd)
        showMore = warm.isNotEmpty() || rpeField.text.isNotEmpty() || noteField.text.isNotEmpty()

        val lastSets = data.lastSession?.sets?.toDraftSets().orEmpty()
        base = LogSheetUiState(
            isLoading = false,
            exerciseId = data.exercise.id,
            exerciseName = data.exercise.name,
            trackingType = data.exercise.trackingType,
            unit = unit,
            increment = if (unit == WeightUnit.KG) userSettings.weightIncrementKg else userSettings.weightIncrementLb,
            lastHint = SetFormatter.hint(lastSets, unit),
            lastPerformedAt = data.lastSession?.performedAt,
            // Per-set mode only makes sense with a weight to vary.
            inputMode = if (data.exercise.trackingType == TrackingType.WEIGHT_REPS) p.inputMode else InputMode.UNIFORM,
            setCount = working.size,
            barbell = WarmupGenerator.Barbell(userSettings.barWeight(unit), userSettings.plates(unit))
                .takeIf { data.exercise.equipment == Equipment.BARBELL },
        )
    }

    private fun fillWorkingFields(tracking: TrackingType, working: List<DraftSet>, mode: InputMode, unit: WeightUnit) {
        when (tracking) {
            TrackingType.TIME -> durationField.setTextAndPlaceCursorAtEnd(
                ParseDurationInputUseCase.formatAll(
                    working.map {
                        it.durationSec
                    },
                ),
            )

            TrackingType.DISTANCE -> working.first().let { s ->
                distanceField.setTextAndPlaceCursorAtEnd(s.distanceM?.let { formatNumber(it) }.orEmpty())
                distanceTimeField.setTextAndPlaceCursorAtEnd(s.durationSec?.let(ParseDurationInputUseCase::format).orEmpty())
            }

            else -> {
                // UNIFORM shows one weight: the heaviest working weight (top set).
                val weightKg = working.mapNotNull { it.weightKg }.maxOrNull()
                weightField.setTextAndPlaceCursorAtEnd(weightKg?.let { SetFormatter.number(it, unit) }.orEmpty())
                repsField.setTextAndPlaceCursorAtEnd(ParseRepInputUseCase.format(working.map { it.reps }))
                if (mode == InputMode.PER_SET) working.forEach { rows += row(it, unit) }
            }
        }
        val rpes = working.mapNotNull { it.rpe }
        if (rpes.isNotEmpty()) {
            rpeField.setTextAndPlaceCursorAtEnd(
                rpes.distinct().singleOrNull()?.let(::formatNumber) ?: rpes.joinToString(" ") {
                    formatNumber(it)
                },
            )
        }
    }

    // ---- UNIFORM editing -------------------------------------------------------------------

    /** Programmatic text change (tests); the UI edits the fields directly. */
    fun onWeightChanged(text: String) = weightField.setTextAndPlaceCursorAtEnd(text)

    fun onRepsChanged(text: String) = repsField.setTextAndPlaceCursorAtEnd(text)

    fun nudgeWeight(direction: Int) = nudge(weightField, direction)

    fun nudge(field: TextFieldState, direction: Int) {
        val inc = base.increment
        val current = parseNumber(field.text.toString()) ?: 0.0
        field.setTextAndPlaceCursorAtEnd(formatNumber(Units.roundToIncrement(current + direction * inc, inc).coerceAtLeast(0.0)))
    }

    fun changeSetCount(delta: Int) {
        if (base.trackingType == TrackingType.TIME) return changeDurationCount(delta)
        val s = state
        val count = (s.setCount + delta).coerceIn(1, ParseRepInputUseCase.MAX_SETS)
        if (count == s.setCount) return
        base = base.copy(setCount = count)
        repsField.setTextAndPlaceCursorAtEnd(ParseRepInputUseCase.format(resize(parsedReps(s.repsText).orEmpty(), count)))
    }

    private fun changeDurationCount(delta: Int) {
        val current = (parseDurations(durationField.text.toString()) as? ParseDurationInputUseCase.Result.Valid)?.seconds.orEmpty()
        val count = (maxOf(current.size, base.setCount) + delta).coerceIn(1, ParseRepInputUseCase.MAX_SETS)
        base = base.copy(setCount = count)
        durationField.setTextAndPlaceCursorAtEnd(ParseDurationInputUseCase.formatAll(resize(current, count)))
    }

    // ---- PER_SET editing -------------------------------------------------------------------

    /** "Vary weight per set": converts between one weight + reps list and individual rows. */
    fun setPerSet(enabled: Boolean) {
        val s = state
        if (enabled == (s.inputMode == InputMode.PER_SET)) return
        if (enabled) {
            rows.clear()
            uniformWorking(s).ifEmpty { listOf(DraftSet(weightKg = weightKg(s.weightText, s.unit))) }.forEach { rows += row(it, s.unit) }
        } else {
            val working = perSetWorking(s)
            working.mapNotNull { it.weightKg }.maxOrNull()?.let { weightField.setTextAndPlaceCursorAtEnd(SetFormatter.number(it, s.unit)) }
            repsField.setTextAndPlaceCursorAtEnd(ParseRepInputUseCase.format(working.map { it.reps }))
        }
        base = base.copy(inputMode = if (enabled) InputMode.PER_SET else InputMode.UNIFORM)
    }

    /** New row copies the last one (people usually add "one more like that"). */
    fun addRow() {
        val last = rows.lastOrNull()
        rows += SetRowState(nextKey++, SetType.NORMAL, last?.weight?.text?.toString().orEmpty(), last?.reps?.text?.toString().orEmpty())
    }

    fun removeRow(row: SetRowState) {
        rows.remove(row)
    }

    /** Working rows cycle Normal → Drop → Failure. */
    fun cycleType(row: SetRowState) {
        row.type = when (row.type) {
            SetType.NORMAL -> SetType.DROP
            SetType.DROP -> SetType.FAILURE
            else -> SetType.NORMAL
        }
    }

    /** Warm-up suggestion: 50 % of the working weight, 10 reps, then the next one heavier. */
    fun addWarmup() {
        val s = state
        val (fraction, reps) = WarmupGenerator.STEPS.getOrElse(warmups.size) { WarmupGenerator.STEPS.last() }
        val kg = workingDisplay(s)?.let { Units.displayToKg(WarmupGenerator.weight(it * fraction, s.increment, s.barbell), s.unit) }
        warmups += row(DraftSet(weightKg = kg, reps = reps, setType = SetType.WARMUP), s.unit)
        showMore = true
    }

    /** Replaces the warm-ups with a 40 / 60 / 80 % ramp to the working weight (plate-loadable on a barbell). */
    fun generateWarmups() {
        val s = state
        val working = workingDisplay(s) ?: return
        warmups.clear()
        WarmupGenerator.generate(working, s.increment, s.barbell).forEach { (w, reps) ->
            warmups += row(DraftSet(weightKg = Units.displayToKg(w, s.unit), reps = reps, setType = SetType.WARMUP), s.unit)
        }
        showMore = true
    }

    /** Top working weight in the display unit (uniform field, or heaviest per-set row). */
    private fun workingDisplay(s: LogSheetUiState): Double? =
        (weightKg(s.weightText, s.unit) ?: perSetWorking(s).mapNotNull { it.weightKg }.maxOrNull())?.let { Units.kgToDisplay(it, s.unit) }

    fun removeWarmup(row: SetRowState) {
        warmups.remove(row)
    }

    // ---- Save ------------------------------------------------------------------------------

    /** Persists the draft. [onSaved] gets the summary line, any new records and an undo snapshot. */
    fun save(onSaved: (summary: String?, records: List<PrHit>, undo: LogSnapshot) -> Unit) {
        val s = state
        if (!s.canSave) return
        val sets = buildSets(s)
        base = base.copy(isSaving = true)
        scope.launch {
            when (val result = logSets(workoutExerciseId, sets, s.inputMode, noteField.text.toString())) {
                is LogExerciseSetsUseCase.Result.Saved -> {
                    if (result.records.isNotEmpty()) haptics.success() else haptics.light()
                    onSaved(SetFormatter.summary(sets, s.unit), result.records, result.undo)
                }

                is LogExerciseSetsUseCase.Result.Invalid -> base = base.copy(isSaving = false)
            }
        }
    }

    internal fun buildSets(s: LogSheetUiState = state): List<DraftSet> {
        val warm = warmups.mapNotNull { rowToDraft(it, s.unit, SetType.WARMUP) }
        val working = when (s.trackingType) {
            TrackingType.TIME -> (parseDurations(durationField.text.toString()) as? ParseDurationInputUseCase.Result.Valid)
                ?.seconds?.map { DraftSet(durationSec = it) }.orEmpty()

            TrackingType.DISTANCE -> {
                val metres = parseNumber(distanceField.text.toString())
                val time = ParseDurationInputUseCase.parseClock(distanceTimeField.text.toString().trim())
                if (metres == null && time == null) emptyList() else listOf(DraftSet(distanceM = metres, durationSec = time))
            }

            else -> if (s.inputMode == InputMode.PER_SET) perSetWorking(s) else uniformWorking(s)
        }
        return warm + applyRpe(working)
    }

    private fun uniformWorking(s: LogSheetUiState): List<DraftSet> {
        val kg = if (s.showsWeight) weightKg(s.weightText, s.unit) else null
        return parsedReps(s.repsText).orEmpty().map { DraftSet(weightKg = kg, reps = it) }
    }

    private fun perSetWorking(s: LogSheetUiState) = rows.mapNotNull { rowToDraft(it, s.unit, it.type) }

    private fun applyRpe(sets: List<DraftSet>): List<DraftSet> {
        val values = rpeValues() ?: return sets
        if (values.isEmpty()) return sets
        return sets.mapIndexed { i, set -> set.copy(rpe = if (values.size == 1) values.first() else values.getOrNull(i)) }
    }

    private fun rpeValues(): List<Double>? {
        val text = rpeField.text.toString().trim()
        if (text.isEmpty()) return emptyList()
        val values = text.split(Regex("[\\s,;+#/]+")).filter {
            it.isNotEmpty()
        }.map { it.replace(',', '.').toDoubleOrNull() ?: return null }
        return values.takeIf { v -> v.all { it in RPE_RANGE } }
    }

    private fun rowToDraft(row: SetRowState, unit: WeightUnit, type: SetType): DraftSet? {
        val reps = row.reps.text.toString().trim().toIntOrNull()
        val kg = weightKg(row.weight.text.toString(), unit)
        val rpe = row.rpe.text.toString().replace(',', '.').trim().toDoubleOrNull()
        if (reps == null && kg == null) return null
        return DraftSet(weightKg = kg, reps = reps, rpe = rpe, setType = type)
    }

    private fun row(set: DraftSet, unit: WeightUnit) = SetRowState(
        key = nextKey++,
        type = set.setType,
        weight = set.weightKg?.let { SetFormatter.number(it, unit) }.orEmpty(),
        reps = set.reps?.toString().orEmpty(),
        rpe = set.rpe?.let(::formatNumber).orEmpty(),
    )

    // ---- Validation ------------------------------------------------------------------------

    private fun weightError(s: LogSheetUiState): LogSheetError? {
        val texts = when {
            !s.showsWeight -> emptyList()
            s.inputMode == InputMode.PER_SET -> rows.map { it.weight.text.toString() }
            else -> listOf(s.weightText)
        } + warmups.map { it.weight.text.toString() }
        return texts.filter { it.isNotBlank() }.firstNotNullOfOrNull { t ->
            val kg = weightKg(t, s.unit)
            when {
                kg == null -> LogSheetError.WEIGHT_INVALID
                kg > SetValidation.MAX_WEIGHT_KG -> LogSheetError.WEIGHT_RANGE
                else -> null
            }
        }
    }

    private fun repsError(s: LogSheetUiState): LogSheetError? {
        if (s.trackingType == TrackingType.TIME || s.trackingType == TrackingType.DISTANCE) return null
        val rowReps = (if (s.inputMode == InputMode.PER_SET) rows else emptyList<SetRowState>()) + warmups
        rowReps.map { it.reps.text.toString().trim() }.filter { it.isNotEmpty() }.forEach { t ->
            val n = t.toIntOrNull() ?: return LogSheetError.REPS_INVALID
            if (n !in SetValidation.REPS) return LogSheetError.REPS_RANGE
        }
        if (s.inputMode == InputMode.PER_SET) return null
        return when (val r = parseReps(s.repsText)) {
            is ParseRepInputUseCase.Result.Invalid ->
                if (r.reason == ParseRepInputUseCase.Reason.NOT_A_NUMBER) LogSheetError.REPS_INVALID else LogSheetError.REPS_RANGE

            else -> null
        }
    }

    private fun otherError(s: LogSheetUiState): LogSheetError? = when {
        rpeValues() == null -> LogSheetError.RPE_INVALID

        s.trackingType == TrackingType.TIME &&
            parseDurations(durationField.text.toString()) is ParseDurationInputUseCase.Result.Invalid -> LogSheetError.TIME_INVALID

        s.trackingType == TrackingType.DISTANCE && distanceField.text.isNotBlank() &&
            parseNumber(distanceField.text.toString()) == null -> LogSheetError.DISTANCE_INVALID

        s.trackingType == TrackingType.DISTANCE && distanceTimeField.text.isNotBlank() &&
            ParseDurationInputUseCase.parseClock(distanceTimeField.text.toString().trim()) == null -> LogSheetError.TIME_INVALID

        else -> null
    }

    // ---- Helpers ---------------------------------------------------------------------------

    private fun parsedReps(text: String) = (parseReps(text) as? ParseRepInputUseCase.Result.Valid)?.reps

    private fun weightKg(text: String, unit: WeightUnit) = parseNumber(text)?.let { Units.displayToKg(it, unit) }

    private fun parseNumber(text: String): Double? = text.replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    private fun <T> resize(values: List<T>, count: Int): List<T> = when {
        values.isEmpty() -> emptyList()
        count > values.size -> values + List(count - values.size) { values.last() }
        else -> values.take(count)
    }

    private fun formatNumber(value: Double): String {
        val rounded = round(value * 100) / 100
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private companion object {
        val RPE_RANGE = 1.0..10.0
    }
}
