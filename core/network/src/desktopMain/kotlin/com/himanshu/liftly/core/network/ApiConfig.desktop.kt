package com.himanshu.liftly.core.network

actual val devServerUrl: String = "http://localhost:8080"

/** Desktop talks to the dev server only when started with `-Dliftly.devServer=true`. */
internal actual fun isDebugBuild(): Boolean = System.getProperty("liftly.devServer") == "true"
