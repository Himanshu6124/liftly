package com.himanshu.liftly.feature.workout

import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.platform.Haptics
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        settings.value = transform(settings.value)
    }
    override suspend fun seedVersion() = 1
    override suspend fun setSeedVersion(version: Int) = Unit
}

class CountingHaptics : Haptics {
    var light = 0
    override fun light() {
        light++
    }
    override fun success() = Unit
}

fun sets(vararg pairs: Pair<Double?, Int>) = pairs.mapIndexed { i, (w, r) ->
    WorkoutSet("old$i", "oldwe", i, w, r, null, null, null, com.himanshu.liftly.core.domain.model.SetType.NORMAL)
}
