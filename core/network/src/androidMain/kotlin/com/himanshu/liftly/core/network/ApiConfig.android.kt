package com.himanshu.liftly.core.network

import android.content.Context
import android.content.pm.ApplicationInfo
import org.koin.mp.KoinPlatform

/** The Android emulator reaches the host machine at 10.0.2.2. */
actual val devServerUrl: String = "http://10.0.2.2:8080"

internal actual fun isDebugBuild(): Boolean =
    KoinPlatform.getKoin().get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
