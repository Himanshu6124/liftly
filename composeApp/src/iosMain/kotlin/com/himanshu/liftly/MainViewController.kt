package com.himanshu.liftly

import androidx.compose.ui.window.ComposeUIViewController

@Suppress("FunctionName", "unused") // Called from Swift.
fun MainViewController() = ComposeUIViewController(configure = { IosStartup.ensureStarted() }) { App() }
