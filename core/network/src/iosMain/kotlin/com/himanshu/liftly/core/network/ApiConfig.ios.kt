package com.himanshu.liftly.core.network

import kotlin.experimental.ExperimentalNativeApi

/** The iOS simulator shares the host network. */
actual val devServerUrl: String = "http://localhost:8080"

@OptIn(ExperimentalNativeApi::class)
internal actual fun isDebugBuild(): Boolean = Platform.isDebugBinary
