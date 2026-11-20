package com.himanshu.liftly.core.data.testing

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

fun inMemoryDb(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
    .setDriver(BundledSQLiteDriver())
    .build()

class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(UserSettings())
    var seed = 0
    override val settings: Flow<UserSettings> = state
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        state.value = transform(state.value)
    }
    override suspend fun seedVersion() = seed
    override suspend fun setSeedVersion(version: Int) {
        seed = version
    }
}

class RecordingSyncController : SyncController {
    var requests = 0
    override val state: Flow<SyncState> = flowOf(SyncState.LocalOnly)
    override fun requestSync() {
        requests++
    }
}
