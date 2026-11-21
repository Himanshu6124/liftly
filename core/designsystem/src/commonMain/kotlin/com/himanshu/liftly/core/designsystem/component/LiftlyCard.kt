package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance

/**
 * Liftly's surface: a flat tonal card (no shadow) that springs down slightly while pressed.
 * [containerColor] defaults to the theme's card color.
 */
@Composable
fun LiftlyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHighest },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSurface },
    )
    if (onClick == null) {
        Card(modifier = modifier, colors = colors, content = content)
    } else {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) PRESSED_SCALE else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "cardPress",
        )
        Card(
            onClick = onClick,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            colors = colors,
            interactionSource = interaction,
            content = content,
        )
    }
}

private inline fun Color.takeOrElse(block: () -> Color): Color = if (this == Color.Unspecified) block() else this

private const val PRESSED_SCALE = 0.97f

/** The one high-emphasis card per screen ("Next up", "Resume"): graphite in light mode, raised graphite in dark. */
object HeroCardDefaults {
    val containerColor: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > HALF) {
            MaterialTheme.colorScheme.inverseSurface
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    val contentColor: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > HALF) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    private const val HALF = 0.5f
}
