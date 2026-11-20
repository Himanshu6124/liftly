package com.himanshu.liftly.core.datastore

import com.himanshu.liftly.core.domain.model.ThemeMode
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSettingsRepositoryTest {
    @Test
    fun defaultsThenPersistsUpdates() = runTest {
        val file = File.createTempFile("liftly", ".preferences_pb").apply { delete() }
        val repo = DataStoreSettingsRepository(createDataStore(file.absolutePath))

        assertEquals(UserSettings(), repo.settings.first())
        repo.update { it.copy(weightUnit = WeightUnit.LB, themeMode = ThemeMode.DARK, defaultSetCount = 4) }
        repo.setSeedVersion(3)

        val s = repo.settings.first()
        assertEquals(WeightUnit.LB, s.weightUnit)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(4, s.defaultSetCount)
        assertEquals(3, repo.seedVersion())
    }
}
