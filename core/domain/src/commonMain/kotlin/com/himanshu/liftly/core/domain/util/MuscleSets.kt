package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.ExerciseSetCount
import com.himanshu.liftly.core.domain.model.Muscle

/**
 * Weekly volume per muscle in "sets": a working set counts 1 for the exercise's primary muscle and
 * ½ for each secondary one (the usual hypertrophy-volume convention).
 */
object MuscleSets {
    const val SECONDARY_WEIGHT = 0.5

    fun perMuscle(counts: List<ExerciseSetCount>): Map<Muscle, Double> {
        val totals = mutableMapOf<Muscle, Double>()
        counts.forEach { c ->
            totals[c.primary] = (totals[c.primary] ?: 0.0) + c.workingSets
            c.secondary.filter { it != c.primary }.distinct().forEach { m ->
                totals[m] = (totals[m] ?: 0.0) + c.workingSets * SECONDARY_WEIGHT
            }
        }
        return totals
    }
}
