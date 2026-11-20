package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Icon button that fires once on tap and keeps firing while held (after a short delay). */
@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            delay(REPEAT_START_MS)
            while (true) {
                currentOnClick()
                delay(REPEAT_INTERVAL_MS)
            }
        }
    }
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.size(52.dp).semantics { this.contentDescription = contentDescription },
        content = content,
    )
}

/** `[−]  value  [+]` with long-press repeat. Big targets for one-handed use. */
@Composable
fun NumberStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RepeatingIconButton(onDecrement, decrementDescription, enabled = canDecrement) {
            Text("−", style = MaterialTheme.typography.headlineSmall)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 40.dp))
        RepeatingIconButton(onIncrement, incrementDescription, enabled = canIncrement) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private const val REPEAT_START_MS = 400L
private const val REPEAT_INTERVAL_MS = 90L
