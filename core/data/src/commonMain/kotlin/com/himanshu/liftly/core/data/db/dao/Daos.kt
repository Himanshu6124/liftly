package com.himanshu.liftly.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncCursorEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao : SyncDao<ExerciseEntity> {
    @Query("SELECT * FROM exercises WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT COUNT(*) FROM exercises WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE seedKey IN (:seedKeys)")
    suspend fun getBySeedKeys(seedKeys: List<String>): List<ExerciseEntity>
}

@Dao
interface RoutineDao : SyncDao<RoutineEntity> {
    @Query("SELECT * FROM routines WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE deletedAt IS NULL ORDER BY orderIndex")
    fun observeAll(): Flow<List<RoutineEntity>>
}

@Dao
interface RoutineExerciseDao : SyncDao<RoutineExerciseEntity> {
    @Query("SELECT * FROM routine_exercises WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<RoutineExerciseEntity>
}

@Dao
interface WorkoutDao : SyncDao<WorkoutEntity> {
    @Query("SELECT * FROM workouts WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL AND deletedAt IS NULL LIMIT 1")
    fun observeInProgress(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): WorkoutEntity?
}

@Dao
interface WorkoutExerciseDao : SyncDao<WorkoutExerciseEntity> {
    @Query("SELECT * FROM workout_exercises WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<WorkoutExerciseEntity>

    @Query("SELECT id FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun idsForWorkout(workoutId: String): List<String>
}

@Dao
interface WorkoutSetDao : SyncDao<WorkoutSetEntity> {
    @Query("SELECT * FROM workout_sets WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<WorkoutSetEntity>

    @Query("SELECT id FROM workout_sets WHERE workoutExerciseId IN (:workoutExerciseIds)")
    suspend fun idsForWorkoutExercises(workoutExerciseIds: List<String>): List<String>
}

@Dao
interface PersonalRecordDao : SyncDao<PersonalRecordEntity> {
    @Query("SELECT * FROM personal_records WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_records WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<PersonalRecordEntity>
}

@Dao
interface SyncCursorDao {
    @Query("SELECT cursor FROM sync_cursors WHERE tableName = :table")
    suspend fun get(table: String): String?

    @Upsert
    suspend fun put(entity: SyncCursorEntity)
}
