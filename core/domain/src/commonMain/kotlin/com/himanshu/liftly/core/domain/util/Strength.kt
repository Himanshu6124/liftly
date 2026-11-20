package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.SetType

object Strength {
    /** Epley: w × (1 + reps / 30). Only meaningful for 1–12 reps; null otherwise. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double? = when {
        reps !in 1..MAX_REPS_FOR_1RM || weightKg <= 0.0 -> null
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    fun estimatedOneRepMax(set: DraftSet): Double? {
        if (set.setType == SetType.WARMUP) return null
        val w = set.weightKg ?: return null
        val r = set.reps ?: return null
        return estimatedOneRepMax(w, r)
    }

    /** Σ weight × reps over working sets (warm-ups excluded). */
    fun volume(sets: List<DraftSet>): Double = sets
        .filter { it.setType != SetType.WARMUP }
        .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    const val MAX_REPS_FOR_1RM = 12
}
