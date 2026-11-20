package com.himanshu.liftly.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.PersonalRecordEntity
import com.himanshu.liftly.core.data.db.entity.PersonalRecordWithWorkoutExercise
import com.himanshu.liftly.core.data.db.entity.RoutineEntity
import com.himanshu.liftly.core.data.db.entity.RoutineExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SetWithDate
import com.himanshu.liftly.core.data.db.entity.SyncCursorEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutExerciseWithExercise
import com.himanshu.liftly.core.data.db.entity.WorkoutSetEntity
import com.himanshu.liftly.core.data.db.entity.WorkoutWithStats
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

    /** Includes archived exercises: routines and history may still reference them. */
    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL")
    fun observeAllIncludingArchived(): Flow<List<ExerciseEntity>>

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

    @Query("SELECT * FROM routines WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): RoutineEntity?

    @Query("SELECT * FROM routines WHERE deletedAt IS NULL ORDER BY orderIndex")
    suspend fun getAll(): List<RoutineEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM routines WHERE deletedAt IS NULL")
    suspend fun maxOrderIndex(): Int
}

@Dao
interface RoutineExerciseDao : SyncDao<RoutineExerciseEntity> {
    @Query("SELECT * FROM routine_exercises WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<RoutineExerciseEntity>

    @Query(
        """
        SELECT * FROM routine_exercises
        WHERE routineId = :routineId AND exerciseId = :exerciseId AND deletedAt IS NULL
        ORDER BY orderIndex LIMIT 1
        """,
    )
    suspend fun getTarget(routineId: String, exerciseId: String): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercises WHERE deletedAt IS NULL ORDER BY orderIndex")
    fun observeAll(): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY orderIndex")
    suspend fun getForRoutine(routineId: String): List<RoutineExerciseEntity>
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

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getInProgress(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeLastFinished(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<WorkoutEntity?>

    /** Finished workouts with list stats computed in SQL (working sets exclude warm-ups). */
    @Query(
        """
        SELECT w.*,
          (SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id AND we.deletedAt IS NULL) AS exerciseCount,
          (SELECT COUNT(*) FROM workout_exercises we
             WHERE we.workoutId = w.id AND we.deletedAt IS NULL AND we.status = 'LOGGED') AS loggedCount,
          (SELECT COUNT(*) FROM workout_sets s JOIN workout_exercises we ON we.id = s.workoutExerciseId
             WHERE we.workoutId = w.id AND s.deletedAt IS NULL AND we.deletedAt IS NULL
               AND we.status = 'LOGGED' AND s.setType != 'WARMUP') AS workingSets,
          (SELECT COALESCE(SUM(COALESCE(s.weightKg, 0) * COALESCE(s.reps, 0)), 0)
             FROM workout_sets s JOIN workout_exercises we ON we.id = s.workoutExerciseId
             WHERE we.workoutId = w.id AND s.deletedAt IS NULL AND we.deletedAt IS NULL
               AND we.status = 'LOGGED' AND s.setType != 'WARMUP') AS volumeKg,
          (SELECT COUNT(*) FROM personal_records pr
             JOIN workout_sets s ON s.id = pr.workoutSetId
             JOIN workout_exercises we ON we.id = s.workoutExerciseId
             WHERE we.workoutId = w.id AND pr.deletedAt IS NULL AND s.deletedAt IS NULL) AS prCount
        FROM workouts w
        WHERE w.endedAt IS NOT NULL AND w.deletedAt IS NULL
        ORDER BY w.startedAt DESC
        """,
    )
    fun observeHistory(): Flow<List<WorkoutWithStats>>
}

@Dao
interface WorkoutExerciseDao : SyncDao<WorkoutExerciseEntity> {
    @Query("SELECT * FROM workout_exercises WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<WorkoutExerciseEntity>

    @Query("SELECT id FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun idsForWorkout(workoutId: String): List<String>

    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND deletedAt IS NULL ORDER BY orderIndex")
    fun observeWithExercise(workoutId: String): Flow<List<WorkoutExerciseWithExercise>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND deletedAt IS NULL ORDER BY orderIndex")
    suspend fun getForWorkout(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): WorkoutExerciseEntity?

    @Query(
        """
        SELECT we.* FROM workout_exercises we
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND we.workoutId != :excludeWorkoutId AND we.status = 'LOGGED'
          AND we.deletedAt IS NULL AND w.deletedAt IS NULL
        ORDER BY we.loggedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLastLogged(exerciseId: String, excludeWorkoutId: String): WorkoutExerciseEntity?

    @Query(
        """
        SELECT we.* FROM workout_exercises we
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND we.id != :excludeId AND we.status = 'LOGGED'
          AND we.deletedAt IS NULL AND w.deletedAt IS NULL
        """,
    )
    suspend fun getLoggedForExercise(exerciseId: String, excludeId: String): List<WorkoutExerciseEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM workout_exercises WHERE workoutId = :workoutId AND deletedAt IS NULL")
    suspend fun maxOrderIndex(workoutId: String): Int

    /** Most recently logged distinct exercises (in finished or current workouts). */
    @Query(
        """
        SELECT we.exerciseId FROM workout_exercises we
        JOIN exercises e ON e.id = we.exerciseId
        WHERE we.status = 'LOGGED' AND we.deletedAt IS NULL AND e.deletedAt IS NULL AND e.isArchived = 0
        GROUP BY we.exerciseId
        ORDER BY MAX(we.loggedAt) DESC
        LIMIT :limit
        """,
    )
    fun observeRecentExerciseIds(limit: Int): Flow<List<String>>
}

@Dao
interface WorkoutSetDao : SyncDao<WorkoutSetEntity> {
    @Query("SELECT * FROM workout_sets WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<WorkoutSetEntity>

    @Query("SELECT id FROM workout_sets WHERE workoutExerciseId IN (:workoutExerciseIds)")
    suspend fun idsForWorkoutExercises(workoutExerciseIds: List<String>): List<String>

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId AND deletedAt IS NULL ORDER BY orderIndex")
    suspend fun getForWorkoutExercise(workoutExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId IN (:ids) AND deletedAt IS NULL ORDER BY orderIndex")
    suspend fun getForWorkoutExercises(ids: List<String>): List<WorkoutSetEntity>

    /** Every logged set of an exercise with its workout's start time (for records and charts). */
    @Query(
        """
        SELECT s.*, w.startedAt AS performedAt, w.id AS workoutId, w.name AS workoutName FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND we.status = 'LOGGED'
          AND s.deletedAt IS NULL AND we.deletedAt IS NULL AND w.deletedAt IS NULL
        ORDER BY w.startedAt
        """,
    )
    fun observeLoggedForExercise(exerciseId: String): Flow<List<SetWithDate>>

    @Query(
        """
        SELECT s.* FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.workoutId = :workoutId AND s.deletedAt IS NULL AND we.deletedAt IS NULL
        ORDER BY s.orderIndex
        """,
    )
    fun observeForWorkout(workoutId: String): Flow<List<WorkoutSetEntity>>
}

@Dao
interface PersonalRecordDao : SyncDao<PersonalRecordEntity> {
    @Query("SELECT * FROM personal_records WHERE syncStatus = 'PENDING'")
    override suspend fun pendingSync(): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_records WHERE id IN (:ids)")
    override suspend fun getByIdsIncludingDeleted(ids: List<String>): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_records WHERE workoutSetId IN (:setIds) AND deletedAt IS NULL")
    suspend fun getForSets(setIds: List<String>): List<PersonalRecordEntity>

    @Query(
        """
        SELECT pr.*, s.workoutExerciseId AS workoutExerciseId FROM personal_records pr
        JOIN workout_sets s ON s.id = pr.workoutSetId
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.workoutId = :workoutId AND pr.deletedAt IS NULL AND s.deletedAt IS NULL AND we.deletedAt IS NULL
        """,
    )
    fun observeForWorkout(workoutId: String): Flow<List<PersonalRecordWithWorkoutExercise>>

    /** Records whose set still exists, for one exercise. */
    @Query(
        """
        SELECT pr.* FROM personal_records pr
        JOIN workout_sets s ON s.id = pr.workoutSetId
        WHERE pr.exerciseId = :exerciseId AND pr.deletedAt IS NULL AND s.deletedAt IS NULL
        """,
    )
    fun observeForExercise(exerciseId: String): Flow<List<PersonalRecordEntity>>
}

@Dao
interface SyncCursorDao {
    @Query("SELECT cursor FROM sync_cursors WHERE tableName = :table")
    suspend fun get(table: String): String?

    @Upsert
    suspend fun put(entity: SyncCursorEntity)
}
