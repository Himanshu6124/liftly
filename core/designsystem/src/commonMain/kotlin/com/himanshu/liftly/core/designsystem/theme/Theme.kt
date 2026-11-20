package com.himanshu.liftly.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.domain.model.ThemeMode

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    /** Minimum touch target. */
    val touch: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Whether [LiftlyTheme]'s dynamicColor has any effect here (Android 12+). */
expect val supportsDynamicColor: Boolean

/** Android 12+ wallpaper colors; null on platforms without dynamic color. */
@Composable
internal expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?

private val LiftlyTypography: Typography
    @Composable get() {
        val base = Typography()
        return base.copy(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }

private val LiftlyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun LiftlyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = (if (dynamicColor) platformDynamicColorScheme(dark) else null)
        ?: if (dark) LiftlyDarkColors else LiftlyLightColors

    MaterialTheme(colorScheme = colors, typography = LiftlyTypography, shapes = LiftlyShapes) {
        androidx.compose.runtime.CompositionLocalProvider(LocalSpacing provides Spacing(), content = content)
    }
}

object LiftlyTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
