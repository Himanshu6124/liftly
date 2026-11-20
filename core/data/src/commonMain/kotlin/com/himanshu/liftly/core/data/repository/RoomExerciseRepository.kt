package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.data.db.writeTransaction
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomExerciseRepository(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val sync: SyncController,
    private val ids: IdGenerator,
) : ExerciseRepository {
    private val dao = db.exerciseDao()

    override fun observeLibrary(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeExercise(id: EntityId): Flow<Exercise?> =
        dao.observeById(id).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun setFavorite(id: EntityId, favorite: Boolean) =
        edit(id) { it.copy(isFavorite = favorite) }

    override suspend fun setUserNote(id: EntityId, note: String?) =
        edit(id) { it.copy(userNote = note?.trim()?.takeIf(String::isNotEmpty)) }

    override suspend fun setRestSeconds(id: EntityId, seconds: Int) = edit(id) { it.copy(restSeconds = seconds) }

    override suspend fun createCustom(exercise: NewCustomExercise): EntityId {
        val id = ids.newId()
        dao.upsertAll(
            listOf(
                ExerciseEntity(
                    id = id,
                    seedKey = null,
                    name = exercise.name.trim(),
                    aliases = emptyList(),
                    primaryMuscle = exercise.primaryMuscle,
                    secondaryMuscles = emptyList(),
                    equipment = exercise.equipment,
                    trackingType = exercise.trackingType,
                    mechanic = null,
                    difficulty = null,
                    imagePaths = emptyList(),
                    videoUrl = null,
                    instructions = emptyList(),
                    formCues = emptyList(),
                    commonMistakes = emptyList(),
                    userNote = null,
                    isFavorite = false,
                    isCustom = true,
                    isArchived = false,
                    sync = SyncMeta.dirty(time.now().toEpochMilliseconds()),
                ),
            ),
        )
        sync.requestSync()
        return id
    }

    /** Every local write marks the row PENDING so it is pushed once a backend exists. */
    private suspend fun edit(id: EntityId, change: (ExerciseEntity) -> ExerciseEntity) {
        val changed = db.writeTransaction {
            val current = dao.getById(id) ?: return@writeTransaction false
            val updated = change(current)
            if (updated == current) return@writeTransaction false
            dao.upsertAll(listOf(updated.copy(sync = current.sync.touch(time.now().toEpochMilliseconds()))))
            true
        }
        if (changed) sync.requestSync()
    }
}
