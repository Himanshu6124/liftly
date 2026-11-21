package com.himanshu.liftly.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Apple sign-in is iOS-only; [AuthRepository.providers] never offers it here. */
@Composable
internal actual fun AppleSignInButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier) =
    FallbackAppleButton(enabled, onClick, modifier)
