package com.himanshu.liftly.core.designsystem.component

import androidx.compose.runtime.Composable
import com.himanshu.liftly.core.designsystem.resources.Res
import com.himanshu.liftly.core.designsystem.resources.workout_name_afternoon
import com.himanshu.liftly.core.designsystem.resources.workout_name_evening
import com.himanshu.liftly.core.designsystem.resources.workout_name_morning
import com.himanshu.liftly.core.designsystem.resources.workout_name_night
import com.himanshu.liftly.core.domain.util.TimeOfDay
import com.himanshu.liftly.core.domain.util.timeOfDay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** The stored name, or "Morning workout" etc. when the user never named it. */
@Composable
fun workoutDisplayName(name: String, startedAt: Instant): String = name.ifBlank { defaultWorkoutName(startedAt) }

@Composable
fun defaultWorkoutName(startedAt: Instant): String = stringResource(defaultNameRes(startedAt))

/** [workoutDisplayName] outside composition (notifications, widgets). */
suspend fun workoutDisplayNameText(name: String, startedAt: Instant): String =
    name.ifBlank { getString(defaultNameRes(startedAt)) }

private fun defaultNameRes(startedAt: Instant) = when (timeOfDay(startedAt)) {
    TimeOfDay.MORNING -> Res.string.workout_name_morning
    TimeOfDay.AFTERNOON -> Res.string.workout_name_afternoon
    TimeOfDay.EVENING -> Res.string.workout_name_evening
    TimeOfDay.NIGHT -> Res.string.workout_name_night
}
