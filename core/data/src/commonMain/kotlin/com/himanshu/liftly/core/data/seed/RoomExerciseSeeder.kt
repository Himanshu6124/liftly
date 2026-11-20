package com.himanshu.liftly.core.data.seed

import co.touchlab.kermit.Logger
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.SyncStatus
import com.himanshu.liftly.core.data.db.writeTransaction
import com.himanshu.liftly.core.domain.repository.ExerciseSeeder
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Inserts the bundled library on first launch and upgrades it when the seed version grows.
 *
 * - Rows are matched by `seedKey` and get the deterministic id `seed:<seedKey>`, identical on every device.
 * - Upgrades replace only library content (name, instructions, images...). User data on those rows
 *   (favorite, note, archived) and custom exercises are never touched.
 * - Seeded rows start as SYNCED with `updatedAt = 0`: they are not pushed to a backend unless the
 *   user changes them, and any real edit (local or remote) wins over seed content.
 */
class RoomExerciseSeeder(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val source: SeedSource,
) : ExerciseSeeder {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val log = Logger.withTag("ExerciseSeeder")

    override suspend fun seedIfNeeded() = mutex.withLock {
        val dao = db.exerciseDao()
        val installed = settings.seedVersion()
        if (installed > 0 && dao.count() > 0 && installed >= SEED_VERSION) return@withLock

        val file = json.decodeFromString(SeedFile.serializer(), source.readExercisesJson())
        db.writeTransaction {
            val existing = dao.getBySeedKeys(file.exercises.map { it.seedKey }).associateBy { it.seedKey }
            dao.upsertAll(file.exercises.map { seed -> seed.toEntity(existing[seed.seedKey]) })
        }
        settings.setSeedVersion(maxOf(file.version, SEED_VERSION))
        log.i { "Seeded ${file.exercises.size} exercises (v${file.version})" }
    }

    private fun SeedExercise.toEntity(current: ExerciseEntity?) = ExerciseEntity(
        id = current?.id ?: idFor(seedKey),
        seedKey = seedKey,
        name = name,
        aliases = aliases,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        trackingType = trackingType,
        mechanic = mechanic,
        difficulty = difficulty,
        imagePaths = imagePaths,
        videoUrl = videoUrl,
        instructions = instructions,
        formCues = formCues,
        commonMistakes = commonMistakes,
        userNote = current?.userNote,
        isFavorite = current?.isFavorite ?: false,
        isCustom = false,
        isArchived = current?.isArchived ?: false,
        sync = current?.sync ?: SyncMeta(updatedAt = 0, syncStatus = SyncStatus.SYNCED),
    )

    companion object {
        /** Bump together with the `version` in exercises.json when the seed changes. */
        const val SEED_VERSION = 1

        fun idFor(seedKey: String) = "seed:$seedKey"
    }
}
