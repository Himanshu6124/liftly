package com.himanshu.liftly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide state: theme now, in-progress workout auto-resume in Milestone 3. */
class AppViewModel(settings: SettingsRepository) : ViewModel() {
    /** null until settings are loaded, so the first frame doesn't flash the wrong theme. */
    val settings: StateFlow<UserSettings?> = settings.settings
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
