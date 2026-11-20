package com.himanshu.liftly.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand: energetic coral primary, cool teal secondary, neutral graphite surfaces.
private val Coral40 = Color(0xFFC2410C)
private val Coral80 = Color(0xFFFFB59A)
private val Coral90 = Color(0xFFFFDBCE)
private val Coral20 = Color(0xFF5A1A00)
private val Coral30 = Color(0xFF7F2B00)
private val Teal40 = Color(0xFF00696E)
private val Teal80 = Color(0xFF6FD6DC)
private val Teal90 = Color(0xFFB4F0F3)
private val Teal20 = Color(0xFF003739)
private val Teal30 = Color(0xFF004F53)
private val Gold40 = Color(0xFF7A5900)
private val Gold80 = Color(0xFFF4BF48)

val LiftlyLightColors = lightColorScheme(
    primary = Coral40,
    onPrimary = Color.White,
    primaryContainer = Coral90,
    onPrimaryContainer = Coral20,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal20,
    tertiary = Gold40,
    background = Color(0xFFFCFAF8),
    surface = Color(0xFFFCFAF8),
    surfaceVariant = Color(0xFFF1E6E1),
    surfaceContainer = Color(0xFFF4EFEC),
    surfaceContainerHigh = Color(0xFFEEE8E5),
)

val LiftlyDarkColors = darkColorScheme(
    primary = Coral80,
    onPrimary = Coral20,
    primaryContainer = Coral30,
    onPrimaryContainer = Coral90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Gold80,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF2A2523),
    surfaceContainer = Color(0xFF1C1A19),
    surfaceContainerHigh = Color(0xFF262321),
)

/** Semantic colors not covered by Material roles. */
object LiftlyColors {
    val Success = Color(0xFF2E9E5B)
    val Pr = Color(0xFFE0A100)
}
