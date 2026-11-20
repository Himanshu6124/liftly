package com.himanshu.liftly.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.himanshu.liftly.core.domain.model.ThemeMode
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Device-local preferences. These are intentionally not part of backend sync (unit, theme etc. are
 * per-device); if cross-device settings are wanted later, mirror [UserSettings] into a synced table.
 */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<UserSettings> = store.data.map { p ->
        val defaults = UserSettings()
        UserSettings(
            weightUnit = p[Keys.UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: defaults.weightUnit,
            weightIncrementKg = p[Keys.INCREMENT_KG] ?: defaults.weightIncrementKg,
            weightIncrementLb = p[Keys.INCREMENT_LB] ?: defaults.weightIncrementLb,
            defaultSetCount = p[Keys.DEFAULT_SETS] ?: defaults.defaultSetCount,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            openNextExerciseAutomatically = p[Keys.AUTO_OPEN_NEXT] ?: defaults.openNextExerciseAutomatically,
            weeklyGoal = p[Keys.WEEKLY_GOAL] ?: defaults.weeklyGoal,
        )
    }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        val next = transform(settings.first())
        store.edit { p ->
            p[Keys.UNIT] = next.weightUnit.name
            p[Keys.INCREMENT_KG] = next.weightIncrementKg
            p[Keys.INCREMENT_LB] = next.weightIncrementLb
            p[Keys.DEFAULT_SETS] = next.defaultSetCount
            p[Keys.THEME] = next.themeMode.name
            p[Keys.DYNAMIC_COLOR] = next.dynamicColor
            p[Keys.AUTO_OPEN_NEXT] = next.openNextExerciseAutomatically
            p[Keys.WEEKLY_GOAL] = next.weeklyGoal
        }
    }

    override suspend fun seedVersion(): Int = store.data.first()[Keys.SEED_VERSION] ?: 0

    override suspend fun setSeedVersion(version: Int) {
        store.edit { it[Keys.SEED_VERSION] = version }
    }

    private object Keys {
        val UNIT = stringPreferencesKey("weight_unit")
        val INCREMENT_KG = doublePreferencesKey("weight_increment_kg")
        val INCREMENT_LB = doublePreferencesKey("weight_increment_lb")
        val DEFAULT_SETS = intPreferencesKey("default_set_count")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_OPEN_NEXT = booleanPreferencesKey("open_next_automatically")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val SEED_VERSION = intPreferencesKey("seed_version")
    }
}

const val SETTINGS_FILE = "liftly.preferences_pb"

fun createDataStore(absolutePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { absolutePath.toPath() })

/** Binds a `DataStore<Preferences>` for the current platform. */
expect val platformDataStoreModule: Module

val dataStoreModule = module {
    includes(platformDataStoreModule)
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}
