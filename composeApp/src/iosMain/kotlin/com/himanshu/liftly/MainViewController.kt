package com.himanshu.liftly

import androidx.compose.ui.window.ComposeUIViewController
import com.himanshu.liftly.di.initKoin

private var koinStarted = false

@Suppress("FunctionName", "unused") // Called from Swift.
fun MainViewController() = ComposeUIViewController(
    configure = {
        if (!koinStarted) {
            initKoin()
            koinStarted = true
        }
    },
) { App() }
