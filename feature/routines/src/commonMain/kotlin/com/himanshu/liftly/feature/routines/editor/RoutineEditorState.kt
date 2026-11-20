package com.himanshu.liftly.feature.routines.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.RoutineExerciseDraft
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.Units
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** One exercise line in the editor; targets are optional text so the user only fills what they care about. */
@Stable
class EditorItem(
    val lineId: String?,
    val exerciseId: String,
    val name: String,
    val imagePaths: List<String>,
    sets: Int,
    reps: Int?,
    weight: String,
) {
    val sets = TextFieldState(sets.toString())
    val reps = TextFieldState(reps?.toString().orEmpty())
    val weight = TextFieldState(weight)
}

class RoutineEditorViewModel(
    private val routineId: String?,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val appScope: AppScope,
) : ViewModel() {
    val name = TextFieldState()
    val items = mutableStateListOf<EditorItem>()
    var unit by mutableStateOf(WeightUnit.KG)
        private set
    var isLoading by mutableStateOf(true)
        private set

    val canSave: Boolean get() = !isLoading && name.text.isNotBlank() && items.isNotEmpty()
    val isNew: Boolean get() = routineId == null

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            unit = settings.settings.first().weightUnit
            routineId?.let { id ->
                routines.observeRoutine(id).first()?.let { r ->
                    name.setTextAndPlaceCursorAtEnd(r.routine.name)
                    r.items.forEach { item ->
                        val re = item.routineExercise
                        items += EditorItem(
                            re.id,
                            item.exercise.id,
                            item.exercise.name,
                            item.exercise.imagePaths,
                            re.targetSets,
                            re.targetReps,
                            re.targetWeightKg?.let { SetFormatter.number(it, unit) }.orEmpty(),
                        )
                    }
                }
            }
            isLoading = false
        }
    }

    fun addExercises(ids: List<String>) {
        viewModelScope.launch {
            val library = exercises.observeLibrary().first().associateBy(Exercise::id)
            ids.mapNotNull { library[it] }.forEach { e ->
                items += EditorItem(null, e.id, e.name, e.imagePaths, DEFAULT_SETS, null, "")
            }
        }
    }

    fun remove(item: EditorItem) {
        items.remove(item)
    }

    fun move(item: EditorItem, by: Int) {
        val from = items.indexOf(item)
        val to = (from + by).coerceIn(0, items.lastIndex)
        if (from < 0 || from == to) return
        items.add(to, items.removeAt(from))
    }

    fun save() {
        if (!canSave) return
        val drafts = items.map { item ->
            RoutineExerciseDraft(
                id = item.lineId,
                exerciseId = item.exerciseId,
                targetSets = item.sets.text.toString().trim().toIntOrNull() ?: DEFAULT_SETS,
                targetReps = item.reps.text.toString().trim().toIntOrNull(),
                targetWeightKg = item.weight.text.toString().replace(',', '.').trim().toDoubleOrNull()?.let { Units.displayToKg(it, unit) },
            )
        }
        val title = name.text.toString()
        viewModelScope.launch {
            appScope.async { routines.saveRoutine(routineId, title, null, drafts) }.await()
            _saved.send(Unit)
        }
    }

    private companion object {
        const val DEFAULT_SETS = 3
    }
}
