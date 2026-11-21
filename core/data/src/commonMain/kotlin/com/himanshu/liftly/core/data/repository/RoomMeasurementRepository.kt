package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.data.db.entity.BodyMeasurementEntity
import com.himanshu.liftly.core.data.db.entity.SyncMeta
import com.himanshu.liftly.core.data.db.entity.touch
import com.himanshu.liftly.core.data.db.writeTransaction
import com.himanshu.liftly.core.data.sync.SyncSql
import com.himanshu.liftly.core.domain.model.BodyMeasurement
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.MeasurementType
import com.himanshu.liftly.core.domain.repository.MeasurementRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.util.IdGenerator
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMeasurementRepository(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val sync: SyncController,
    private val sql: SyncSql,
) : MeasurementRepository {
    private val dao = db.bodyMeasurementDao()

    override fun observe(type: MeasurementType): Flow<List<BodyMeasurement>> = dao.observe(type).map { rows -> rows.map { it.toDomain() } }

    override fun observeUsedTypes(): Flow<Set<MeasurementType>> = dao.observeUsedTypes().map { it.toSet() }

    override suspend fun add(type: MeasurementType, value: Double): EntityId = write {
        val now = now()
        val id = ids.newId()
        dao.upsertAll(listOf(BodyMeasurementEntity(id, type, value, measuredAt = now, sync = SyncMeta.dirty(now))))
        id
    }

    override suspend fun delete(id: EntityId): BodyMeasurement? = write {
        val row = dao.getById(id) ?: return@write null
        sql.delete("body_measurements", listOf(id), now())
        row.toDomain()
    }

    override suspend fun restore(measurement: BodyMeasurement) = write {
        val now = now()
        // Revive the tombstone (keeps its server version so it syncs as an update), or re-insert.
        val old = dao.getByIdsIncludingDeleted(listOf(measurement.id)).firstOrNull()
        val meta = old?.sync?.copy(deletedAt = null)?.touch(now) ?: SyncMeta.dirty(now)
        dao.upsertAll(
            listOf(
                BodyMeasurementEntity(
                    measurement.id,
                    measurement.type,
                    measurement.value,
                    measurement.measuredAt.toEpochMilliseconds(),
                    meta,
                ),
            ),
        )
    }

    private fun now() = time.now().toEpochMilliseconds()

    private suspend fun <T> write(block: suspend () -> T): T = db.writeTransaction(block).also { sync.requestSync() }
}

private fun BodyMeasurementEntity.toDomain() = BodyMeasurement(id, type, value, measuredAt.toInstant())
