package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
    suspend fun seedVersion(): Int
    suspend fun setSeedVersion(version: Int)
}
