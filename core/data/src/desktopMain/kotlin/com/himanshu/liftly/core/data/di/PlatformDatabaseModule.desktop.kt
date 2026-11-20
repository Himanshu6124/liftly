package com.himanshu.liftly.core.data.di

import androidx.room.Room
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.configureLiftly
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformDatabaseModule: Module = module {
    single<AppDatabase> {
        Room.databaseBuilder<AppDatabase>(name = File(appDataDir(), AppDatabase.NAME).absolutePath)
            .configureLiftly()
    }
}

/** ~/Library/Application Support/Liftly, %APPDATA%\Liftly or ~/.local/share/liftly. */
fun appDataDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    val dir = when {
        "mac" in os -> File(home, "Library/Application Support/Liftly")
        "win" in os -> File(System.getenv("APPDATA") ?: home, "Liftly")
        else -> File(home, ".local/share/liftly")
    }
    return dir.apply { mkdirs() }
}
