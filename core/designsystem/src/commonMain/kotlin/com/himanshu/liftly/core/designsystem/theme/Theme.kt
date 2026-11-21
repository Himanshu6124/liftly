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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** System faces, heavier and tighter at the top; tabular figures everywhere so kg and reps line up. */
private val LiftlyTypography: Typography
    @Composable get() {
        val base = Typography()
        fun TextStyle.tuned(weight: FontWeight, tracking: Double = 0.0) =
            copy(fontWeight = weight, letterSpacing = tracking.sp, fontFeatureSettings = "tnum")
        return Typography(
            displayLarge = base.displayLarge.tuned(FontWeight.ExtraBold, -1.0),
            displayMedium = base.displayMedium.tuned(FontWeight.ExtraBold, -1.0),
            displaySmall = base.displaySmall.tuned(FontWeight.ExtraBold, -0.75),
            headlineLarge = base.headlineLarge.tuned(FontWeight.ExtraBold, -0.75),
            headlineMedium = base.headlineMedium.tuned(FontWeight.ExtraBold, -0.6),
            headlineSmall = base.headlineSmall.tuned(FontWeight.Bold, -0.4),
            titleLarge = base.titleLarge.tuned(FontWeight.Bold, -0.3),
            titleMedium = base.titleMedium.tuned(FontWeight.SemiBold, -0.1),
            titleSmall = base.titleSmall.tuned(FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.tuned(FontWeight.Normal, 0.0),
            bodyMedium = base.bodyMedium.tuned(FontWeight.Normal, 0.0),
            bodySmall = base.bodySmall.tuned(FontWeight.Normal, 0.1),
            labelLarge = base.labelLarge.tuned(FontWeight.SemiBold, 0.1),
            labelMedium = base.labelMedium.tuned(FontWeight.SemiBold, 0.3),
            labelSmall = base.labelSmall.tuned(FontWeight.SemiBold, 0.3),
        )
    }

private val LiftlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
