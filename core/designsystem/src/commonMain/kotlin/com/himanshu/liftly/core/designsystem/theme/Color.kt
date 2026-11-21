package com.himanshu.liftly.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "Calibrated plates": competition plate colors on a graphite / cool-grey ground.
// Plate red is the action color, plate blue the secondary, plate green "done", plate yellow PRs.
private val PlateRed = Color(0xFFD42A2F)
private val Graphite = Color(0xFF16181C)

val LiftlyLightColors = lightColorScheme(
    primary = PlateRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410004),
    secondary = Color(0xFF1F5AD6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF001A43),
    tertiary = Color(0xFF15824C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9F2DA),
    onTertiaryContainer = Color(0xFF002111),
    background = Color(0xFFEEF0F3),
    onBackground = Color(0xFF111317),
    surface = Color(0xFFEEF0F3),
    onSurface = Color(0xFF111317),
    surfaceVariant = Color(0xFFE1E4E9),
    onSurfaceVariant = Color(0xFF5A606B),
    outline = Color(0xFF8B909A),
    outlineVariant = Color(0xFFD6D9DF),
    inverseSurface = Graphite,
    inverseOnSurface = Color(0xFFF1F2F4),
    inversePrimary = Color(0xFFFF8A8C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF7F8FA),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceBright = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceTint = Color.Transparent,
)

val LiftlyDarkColors = darkColorScheme(
    primary = Color(0xFFE5383D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C1013),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFF8AB0FF),
    onSecondary = Color(0xFF002A6B),
    secondaryContainer = Color(0xFF173E8C),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF5ED493),
    onTertiary = Color(0xFF003920),
    tertiaryContainer = Color(0xFF0F5A35),
    onTertiaryContainer = Color(0xFFC9F2DA),
    background = Color(0xFF111214),
    onBackground = Color(0xFFF1F2F4),
    surface = Color(0xFF111214),
    onSurface = Color(0xFFF1F2F4),
    surfaceVariant = Color(0xFF26292E),
    onSurfaceVariant = Color(0xFF9399A3),
    outline = Color(0xFF5C616A),
    outlineVariant = Color(0xFF2B2E34),
    inverseSurface = Color(0xFFE9EBEE),
    inverseOnSurface = Graphite,
    inversePrimary = PlateRed,
    surfaceContainerLowest = Color(0xFF0C0D0F),
    surfaceContainerLow = Color(0xFF1B1D21),
    surfaceContainer = Color(0xFF17191C),
    surfaceContainerHigh = Color(0xFF26292E),
    surfaceBright = Color(0xFF3A3D44),
    surfaceContainerHighest = Color(0xFF1B1D21),
    surfaceTint = Color.Transparent,
)

/** Semantic colors not covered by Material roles. */
object LiftlyColors {
    val Success = Color(0xFF1FA863)
    val Pr = Color(0xFFF2C12E)

    /** Text on [Pr] (a light yellow in both themes). */
    val OnPr = Color(0xFF1A1400)
}

/**
 * Competition (calibrated) plate colors, IPF convention: 25 red, 20 blue, 15 yellow, 10 green,
 * 5 white, smaller change plates dark. Used for plate stacks and as Liftly's data palette.
 */
object PlateColors {
    val Red = Color(0xFFE0262C)
    val Blue = Color(0xFF2463E8)
    val Yellow = Color(0xFFF2C12E)
    val Green = Color(0xFF1FA863)
    val White = Color(0xFFE9EBEE)
    val Change = Color(0xFF4A4E57)

    /** The four big plates in order, for categorical series (sets per muscle, week plates). */
    val Series = listOf(Red, Blue, Yellow, Green)

    /** Color of a plate weighing [kg] (pound plates map to the nearest kg class). */
    fun forPlate(kg: Double): Color = when {
        kg >= RED_MIN -> Red
        kg >= BLUE_MIN -> Blue
        kg >= YELLOW_MIN -> Yellow
        kg >= GREEN_MIN -> Green
        kg >= WHITE_MIN -> White
        else -> Change
    }

    /** Readable text on a plate color: dark on yellow/white, white on the rest. */
    fun contentOn(plate: Color): Color = if (plate == Yellow || plate == White) Color(0xFF111317) else Color.White

    private const val RED_MIN = 22.5
    private const val BLUE_MIN = 17.5
    private const val YELLOW_MIN = 12.5
    private const val GREEN_MIN = 8.0
    private const val WHITE_MIN = 4.0
}
