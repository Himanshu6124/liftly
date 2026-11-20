package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.model.WorkoutSet
import kotlin.math.abs
import kotlin.math.roundToLong

/** One-line set summaries for cards and hints. Warm-ups are left out. */
object SetFormatter {

    /** Logged card: `4 × 80 kg · 8, 8, 7, 6` or `80/80/75 kg · 8, 8, 10`; reps-only: `3 sets · 12, 10, 8`. */
    fun summary(sets: List<DraftSet>, unit: WeightUnit): String? {
        val working = sets.filter { it.setType != SetType.WARMUP }
        if (working.isEmpty()) return null
        cardioSummary(working)?.let { return it }
        val reps = working.joinToString(", ") { it.reps?.toString() ?: "–" }
        val weights = working.map { it.weightKg }
        return when {
            weights.all { it == null } -> "${working.size} sets · $reps"
            weights.distinct().size == 1 -> "${working.size} × ${weight(weights.first(), unit)} · $reps"
            else -> "${weights.joinToString("/") { number(it, unit) }} ${unit.symbol} · $reps"
        }
    }

    /** Time sets: `3 × 1:00` or `1:00, 0:45`; distance: `5000 m · 25:00`. null for weight/reps sets. */
    private fun cardioSummary(working: List<DraftSet>): String? {
        if (working.isEmpty()) return null
        if (working.any { it.distanceM != null }) {
            val s = working.first()
            val metres = s.distanceM?.let { "${number(it, WeightUnit.KG)} m" }
            return listOfNotNull(metres, s.durationSec?.let(::clock)).joinToString(" · ")
        }
        if (working.all { it.reps == null && it.durationSec != null }) {
            val times = working.mapNotNull { it.durationSec }
            return if (times.distinct().size == 1) "${times.size} × ${clock(times.first())}" else times.joinToString(", ") { clock(it) }
        }
        return null
    }

    fun clock(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = (seconds % 60).toString().padStart(2, '0')
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
    }

    /** Pending-card hint: `77.5 kg × 8, 8, 8, 7`. */
    fun hint(sets: List<DraftSet>, unit: WeightUnit): String? {
        val working = sets.filter { it.setType != SetType.WARMUP }
        cardioSummary(working)?.let { return it }
        if (working.isEmpty() || working.all { it.reps == null }) return null
        val reps = working.joinToString(", ") { it.reps?.toString() ?: "–" }
        val weights = working.map { it.weightKg }
        return when {
            weights.all { it == null } -> reps
            weights.distinct().size == 1 -> "${weight(weights.first(), unit)} × $reps"
            else -> "${weights.joinToString("/") { number(it, unit) }} ${unit.symbol} × $reps"
        }
    }

    fun weight(kg: Double?, unit: WeightUnit): String = "${number(kg, unit)} ${unit.symbol}"

    /** 80.0 → "80", 77.5 → "77.5"; converted to [unit] and rounded to 0.1. */
    fun number(kg: Double?, unit: WeightUnit): String {
        if (kg == null) return "–"
        val value = (Units.kgToDisplay(kg, unit) * 10).roundToLong() / 10.0
        return if (abs(value - value.roundToLong()) < 1e-9) value.roundToLong().toString() else value.toString()
    }

    val WeightUnit.symbol: String get() = if (this == WeightUnit.KG) "kg" else "lb"
}

fun List<WorkoutSet>.toDraftSets() = sortedBy { it.orderIndex }.map {
    DraftSet(it.weightKg, it.reps, it.durationSec, it.distanceM, it.rpe, it.setType)
}
