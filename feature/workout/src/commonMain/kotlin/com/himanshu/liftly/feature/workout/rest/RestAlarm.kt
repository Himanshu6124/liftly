package com.himanshu.liftly.feature.workout.rest

import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.platform.Notifier
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.rest_notification_body
import com.himanshu.liftly.feature.workout.resources.rest_notification_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

/** The "rest is over" alert outside the app (local notification); in the app a haptic plays instead. */
interface RestAlarm {
    fun schedule(endsAt: Instant)
    fun cancel()
}

class NotifierRestAlarm(private val notifier: Notifier, private val appScope: AppScope) : RestAlarm {
    override fun schedule(endsAt: Instant) {
        appScope.launch {
            notifier.schedule(ID, endsAt, getString(Res.string.rest_notification_title), getString(Res.string.rest_notification_body))
        }
    }

    override fun cancel() = notifier.cancel(ID)

    private companion object {
        const val ID = "rest-timer"
    }
}

object RestTimer {
    /** Presets offered in the menu, in seconds. */
    val PRESETS = listOf(60, 90, 120, 180)
    const val DEFAULT_SECONDS = 90
    const val ADJUST_SECONDS = 30
}
