package com.himanshu.liftly.core.data.di

import androidx.room.Room
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.configureLiftly
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<AppDatabase> {
        val context = androidContext().applicationContext
        Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath(AppDatabase.NAME).absolutePath)
            .configureLiftly()
    }
}
