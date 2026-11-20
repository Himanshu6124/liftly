package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.ExerciseSession
import com.himanshu.liftly.core.domain.model.SetType
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** What an exercise chart can plot, one value per session. Weights are in kg. */
enum class ProgressMetric { EST_1RM, BEST_WEIGHT, VOLUME, MAX_REPS, TOTAL_REPS, LONGEST_SET, DISTANCE }

enum class ProgressRange(val days: Int?) { MONTH(30), QUARTER(91), YEAR(365), ALL(null) }

data class ProgressPoint(val at: Instant, val value: Double)

object ExerciseProgress {

    /** The session's value for [metric] from working sets (warm-ups excluded), or null when it has none. */
    fun value(session: ExerciseSession, metric: ProgressMetric): Double? {
        val working = session.sets.filter { it.setType != SetType.WARMUP }
        val v = when (metric) {
            ProgressMetric.EST_1RM -> working.mapNotNull { s ->
                val w = s.weightKg ?: return@mapNotNull null
                s.reps?.let { Strength.estimatedOneRepMax(w, it) }
            }.maxOrNull()

            ProgressMetric.BEST_WEIGHT -> working.mapNotNull { it.weightKg }.filter { it > 0 }.maxOrNull()

            ProgressMetric.VOLUME -> working.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

            ProgressMetric.MAX_REPS -> working.mapNotNull { it.reps }.maxOrNull()?.toDouble()

            ProgressMetric.TOTAL_REPS -> working.mapNotNull { it.reps }.sum().toDouble()

            ProgressMetric.LONGEST_SET -> working.mapNotNull { it.durationSec }.maxOrNull()?.toDouble()

            ProgressMetric.DISTANCE -> working.mapNotNull { it.distanceM }.sum()
        }
        return v?.takeIf { it > 0.0 }
    }

    /** Metrics that have at least one value in [sessions], in display order. */
    fun available(sessions: List<ExerciseSession>): List<ProgressMetric> =
        ProgressMetric.entries.filter { m -> sessions.any { value(it, m) != null } }

    /** Oldest first, only sessions inside [range] (counted back from [now]) that have a value. */
    fun series(sessions: List<ExerciseSession>, metric: ProgressMetric, range: ProgressRange, now: Instant): List<ProgressPoint> {
        val from = range.days?.let { now - it.days }
        return sessions
            .filter { from == null || it.performedAt >= from }
            .sortedBy { it.performedAt }
            .mapNotNull { s -> value(s, metric)?.let { ProgressPoint(s.performedAt, it) } }
    }
}
