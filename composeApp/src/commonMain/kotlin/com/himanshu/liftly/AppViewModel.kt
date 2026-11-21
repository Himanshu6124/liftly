package com.himanshu.liftly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.ExerciseSeeder
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.usecase.StartNextWorkoutUseCase
import com.himanshu.liftly.navigation.AppAction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: theme, first-launch seeding, in-progress workout auto-resume, deep-link actions. */
class AppViewModel(
    settings: SettingsRepository,
    seeder: ExerciseSeeder,
    private val workouts: WorkoutRepository,
    private val startNextWorkout: StartNextWorkoutUseCase,
) : ViewModel() {
    /** null until settings are loaded, so the first frame doesn't flash the wrong theme. */
    val settings: StateFlow<UserSettings?> = settings.settings
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Launching with a workout in progress jumps straight back into it. */
    suspend fun hasWorkoutInProgress(): Boolean = workouts.getInProgressWorkout() != null

    /** Runs a deep-link action; true when the workout screen should open. */
    suspend fun run(action: AppAction): Boolean = when (action) {
        AppAction.START_NEXT_WORKOUT -> {
            startNextWorkout()
            true
        }

        AppAction.OPEN_WORKOUT -> hasWorkoutInProgress()
    }

    init {
        viewModelScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure { Logger.withTag("App").e(it) { "Exercise seeding failed" } }
        }
    }
}
