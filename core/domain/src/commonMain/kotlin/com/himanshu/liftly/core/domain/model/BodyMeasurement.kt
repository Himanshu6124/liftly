package com.himanshu.liftly.core.domain.model

import kotlin.time.Instant

/** One body measurement. [value] is in kg (weight), cm (lengths) or % (body fat). */
data class BodyMeasurement(
    val id: EntityId,
    val type: MeasurementType,
    val value: Double,
    val measuredAt: Instant,
)
