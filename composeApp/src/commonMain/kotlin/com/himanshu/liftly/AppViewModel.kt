package com.himanshu.liftly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.ExerciseSeeder
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: theme, first-launch seeding; in-progress workout auto-resume in Milestone 3. */
class AppViewModel(settings: SettingsRepository, seeder: ExerciseSeeder) : ViewModel() {
    /** null until settings are loaded, so the first frame doesn't flash the wrong theme. */
    val settings: StateFlow<UserSettings?> = settings.settings
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure { Logger.withTag("App").e(it) { "Exercise seeding failed" } }
        }
    }
}
