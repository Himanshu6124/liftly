package com.himanshu.liftly.core.data.di

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.repository.RoomExerciseRepository
import com.himanshu.liftly.core.data.repository.RoomWorkoutRepository
import com.himanshu.liftly.core.data.seed.RoomExerciseSeeder
import com.himanshu.liftly.core.data.sync.LocalOnlySyncController
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.data.sync.SyncTables
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.repository.ExerciseSeeder
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/** Provides the platform-specific `AppDatabase` singleton. A `SeedSource` must be bound by the app. */
expect val platformDatabaseModule: Module

val dataModule = module {
    includes(platformDatabaseModule)
    single { TimeProvider.System }
    single { IdGenerator.Uuid }
    single { AppScope() }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().routineDao() }
    single { get<AppDatabase>().routineExerciseDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().workoutExerciseDao() }
    single { get<AppDatabase>().workoutSetDao() }
    single { get<AppDatabase>().personalRecordDao() }
    single { SyncSql(get()) }
    single<ExerciseRepository> { RoomExerciseRepository(get(), get(), get(), get()) }
    single<WorkoutRepository> { RoomWorkoutRepository(get(), get(), get(), get(), get()) }
    factory { StartWorkoutUseCase(get()) }
    factory { LogExerciseSetsUseCase(get()) }
    factory { GetLogSheetPrefillUseCase() }
    factory { ParseRepInputUseCase() }
    single<ExerciseSeeder> { RoomExerciseSeeder(get(), get(), get()) }
    single { SyncTables(get()) }

    // Offline-only for now. To enable a backend: bind a RemoteSyncDataSource implementation and
    // replace this with `BackgroundSyncController(SyncEngine(get(), get(), get(), get()), get(), appScope)`.
    single<SyncController> { LocalOnlySyncController() }
}
