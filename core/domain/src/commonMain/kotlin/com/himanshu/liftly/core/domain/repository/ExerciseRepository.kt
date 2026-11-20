package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /** All non-archived exercises, A–Z. */
    fun observeLibrary(): Flow<List<Exercise>>

    fun observeExercise(id: EntityId): Flow<Exercise?>

    suspend fun setFavorite(id: EntityId, favorite: Boolean)

    /** Blank notes are stored as null. */
    suspend fun setUserNote(id: EntityId, note: String?)

    /** Creates a custom exercise and returns its id. */
    suspend fun createCustom(exercise: NewCustomExercise): EntityId
}
