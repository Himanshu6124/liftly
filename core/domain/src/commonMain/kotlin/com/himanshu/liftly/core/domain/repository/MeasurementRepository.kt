package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.BodyMeasurement
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.MeasurementType
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    /** All entries of [type], oldest first. */
    fun observe(type: MeasurementType): Flow<List<BodyMeasurement>>

    /** Types that have at least one entry (to show them first). */
    fun observeUsedTypes(): Flow<Set<MeasurementType>>

    /** [value] in kg / cm / %; recorded at the current time. */
    suspend fun add(type: MeasurementType, value: Double): EntityId

    /** Deletes and returns the entry so Undo can put it back. */
    suspend fun delete(id: EntityId): BodyMeasurement?

    suspend fun restore(measurement: BodyMeasurement)
}
