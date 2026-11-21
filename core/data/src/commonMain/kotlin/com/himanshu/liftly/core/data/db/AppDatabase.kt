package com.himanshu.liftly.core.data.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.dao.BodyMeasurementDao
import com.himanshu.liftly.core.data.db.dao.ExerciseDao
import com.himanshu.liftly.core.data.db.dao.PersonalRecordDao
import com.himanshu.liftly.core.data.db.dao.RoutineDao
import com.himanshu.liftly.core.data.db.dao.RoutineExerciseDao
import com.himanshu.liftly.core.data.db.dao.SyncCursorDao
import com.himanshu.liftly.core.data.db.dao.WorkoutDao
import com.himanshu.liftly.core.data.db.dao.WorkoutExerciseDao
import com.himanshu.liftly.core.data.db.dao.WorkoutSetDao
import com.himanshu.liftly.core.data.db.entity.BodyMeasurementEntity
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncCursorEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        SyncCursorEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: exercises.restSeconds, workouts.restEndsAt (rest timer).
        AutoMigration(from = 1, to = 2),
        // v3: body_measurements table.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao

    companion object {
        const val NAME = "liftly.db"
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Shared configuration applied to every platform's builder. */
fun RoomDatabase.Builder<AppDatabase>.configureLiftly(): AppDatabase = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
