package com.himanshu.liftly.core.data.di

import androidx.room.Room
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.configureLiftly
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformDatabaseModule: Module = module {
    single<AppDatabase> {
        Room.databaseBuilder<AppDatabase>(name = "${documentDirectory()}/${AppDatabase.NAME}")
            .configureLiftly()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory unavailable" }
}
