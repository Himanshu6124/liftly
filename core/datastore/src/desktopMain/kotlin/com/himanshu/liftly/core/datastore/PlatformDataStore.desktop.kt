package com.himanshu.liftly.core.datastore

import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformDataStoreModule: Module = module {
    single {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = when {
            "mac" in os -> File(home, "Library/Application Support/Liftly")
            "win" in os -> File(System.getenv("APPDATA") ?: home, "Liftly")
            else -> File(home, ".local/share/liftly")
        }.apply { mkdirs() }
        createDataStore(File(dir, SETTINGS_FILE).absolutePath)
    }
}
