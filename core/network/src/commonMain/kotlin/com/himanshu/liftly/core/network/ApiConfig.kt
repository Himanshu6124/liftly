package com.himanshu.liftly.core.network

/** Where the server is. */
data class ApiConfig(val baseUrl: String)

/** Production server (Oracle VM behind nginx, see deploy/README.md). */
const val PRODUCTION_SERVER_URL = "https://liftly-api.duckdns.org"

/** The dev server (`./gradlew :server:runDev`) as seen from a simulator or emulator. */
expect val devServerUrl: String

/** Debug app builds (simulator, emulator, `assembleDebug`) use the dev server. */
internal expect fun isDebugBuild(): Boolean

fun defaultServerUrl(): String = if (isDebugBuild()) devServerUrl else PRODUCTION_SERVER_URL
