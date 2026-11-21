package com.himanshu.liftly.core.network

/** Where the server is. Until the production server exists (Milestone 25), the local dev server. */
data class ApiConfig(val baseUrl: String)

/** The dev server (`./gradlew :server:runDev`) as seen from a simulator or emulator. */
expect val devServerUrl: String
