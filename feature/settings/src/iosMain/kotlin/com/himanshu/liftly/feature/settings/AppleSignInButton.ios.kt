package com.himanshu.liftly.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitView
import com.himanshu.liftly.core.platform.AppleSignInBridgeHolder

@Composable
internal actual fun AppleSignInButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bridge = AppleSignInBridgeHolder.delegate ?: return FallbackAppleButton(enabled, onClick, modifier)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    // White button on dark backgrounds, black on light (Apple's guidelines). Recreated when it flips.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    key(dark) {
        UIKitView(
            factory = { bridge.makeButton(dark) { if (currentEnabled) currentOnClick() } },
            modifier = modifier.fillMaxWidth().height(BUTTON_HEIGHT),
            update = { it.alpha = if (enabled) 1.0 else 0.5 },
        )
    }
}
