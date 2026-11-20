package com.himanshu.liftly.feature.workout.active

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.elapsed_description
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Ticks once a second. Only this composable recomposes; the exercise list doesn't.
 * Time is derived from [startedAt] (stored in Room), never from a running counter, so it stays
 * correct after the app is backgrounded or killed.
 */
@Composable
internal fun ElapsedTime(startedAt: Instant, modifier: Modifier = Modifier) {
    val elapsed by produceState(Clock.System.now() - startedAt, startedAt) {
        while (true) {
            value = Clock.System.now() - startedAt
            delay(1_000 - (value.inWholeMilliseconds % 1_000))
        }
    }
    val text = formatElapsed(elapsed)
    val description = stringResource(Res.string.elapsed_description, text)
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { contentDescription = description },
    )
}

fun formatElapsed(d: Duration): String {
    val total = d.inWholeSeconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun Long.two() = toString().padStart(2, '0')
    return if (h > 0) "$h:${m.two()}:${s.two()}" else "${m.two()}:${s.two()}"
}
