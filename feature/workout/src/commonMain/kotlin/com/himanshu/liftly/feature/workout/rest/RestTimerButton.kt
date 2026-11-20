package com.himanshu.liftly.feature.workout.rest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutIntent
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.rest_add
import com.himanshu.liftly.feature.workout.resources.rest_button
import com.himanshu.liftly.feature.workout.resources.rest_menu_description
import com.himanshu.liftly.feature.workout.resources.rest_preset_remembered
import com.himanshu.liftly.feature.workout.resources.rest_running_description
import com.himanshu.liftly.feature.workout.resources.rest_stop
import com.himanshu.liftly.feature.workout.resources.rest_subtract
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Floating rest timer. Idle: "Rest ▾" opens the presets (the one last used for the current
 * exercise is marked). Running: shows the countdown; tap for +30 s / −30 s / Stop.
 */
@Composable
internal fun RestTimerButton(
    endsAt: Instant?,
    suggestion: Int,
    remembered: Boolean,
    onIntent: (ActiveWorkoutIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        if (endsAt == null) {
            val description = stringResource(Res.string.rest_menu_description)
            ExtendedFloatingActionButton(
                onClick = { menuOpen = true },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
                text = { Text(stringResource(Res.string.rest_button)) },
                icon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                RestTimer.PRESETS.forEach { seconds ->
                    val label = SetFormatter.clock(seconds)
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (seconds == suggestion &&
                                    remembered
                                ) {
                                    stringResource(Res.string.rest_preset_remembered, label)
                                } else {
                                    label
                                },
                            )
                        },
                        leadingIcon = if (seconds == suggestion) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            menuOpen = false
                            onIntent(ActiveWorkoutIntent.RestStart(seconds))
                        },
                    )
                }
            }
        } else {
            val left = remainingText(endsAt)
            val description = stringResource(Res.string.rest_running_description, left)
            ExtendedFloatingActionButton(
                onClick = { menuOpen = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
                text = { Text(left, style = MaterialTheme.typography.titleMedium) },
                icon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(
                    Res.string.rest_add to ActiveWorkoutIntent.RestAdjust(RestTimer.ADJUST_SECONDS),
                    Res.string.rest_subtract to ActiveWorkoutIntent.RestAdjust(-RestTimer.ADJUST_SECONDS),
                    Res.string.rest_stop to ActiveWorkoutIntent.RestStop,
                ).forEach { (label, intent) ->
                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                        menuOpen = false
                        onIntent(intent)
                    })
                }
            }
        }
    }
}

/** "1:23", ticking once a second; only this read recomposes. Derived from [endsAt], never a counter. */
@Composable
private fun remainingText(endsAt: Instant): String {
    val seconds by produceState(secondsLeft(endsAt), endsAt) {
        while (true) {
            value = secondsLeft(endsAt)
            if (value == 0) break
            delay(MILLIS_PER_SECOND - (Clock.System.now().toEpochMilliseconds() % MILLIS_PER_SECOND))
        }
    }
    return SetFormatter.clock(seconds)
}

/** Rounded up, so it reads 0:01 until the very end. */
private fun secondsLeft(endsAt: Instant): Int {
    val ms = (endsAt - Clock.System.now()).inWholeMilliseconds
    return if (ms <= 0) 0 else ((ms + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
}

private const val MILLIS_PER_SECOND = 1_000L
