package com.himanshu.liftly

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.himanshu.liftly.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Liftly",
            state = rememberWindowState(width = 1100.dp, height = 780.dp),
        ) {
            App()
        }
    }
}
