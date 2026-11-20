package com.himanshu.liftly.core.data.di

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.sync.LocalOnlySyncController
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.sync.SyncTables
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/** Provides the platform-specific `AppDatabase` singleton. */
expect val platformDatabaseModule: Module

val dataModule = module {
    includes(platformDatabaseModule)
    single { TimeProvider.System }
    single { IdGenerator.Uuid }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().routineDao() }
    single { get<AppDatabase>().routineExerciseDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().workoutExerciseDao() }
    single { get<AppDatabase>().workoutSetDao() }
    single { get<AppDatabase>().personalRecordDao() }
    single { SyncSql(get()) }
    single { SyncTables(get()) }

    // Offline-only for now. To enable a backend: bind a RemoteSyncDataSource implementation and
    // replace this with `BackgroundSyncController(SyncEngine(get(), get(), get(), get()), get(), appScope)`.
    single<SyncController> { LocalOnlySyncController() }
}
