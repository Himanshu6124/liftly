package com.himanshu.liftly.core.domain.util

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Barbell loading, all in the display unit (kg or lb). Plates are per side and assumed available
 * in pairs, as many as needed. Uses exact search (not greedy), so with 15 and 10 plates a 20 per
 * side load is 10 + 10, which greedy (15 first) would miss.
 */
object PlateCalculator {
    /** Plates for one side, heaviest first; [total] is what the bar actually weighs loaded. */
    data class Loadout(val perSide: List<Double>, val total: Double, val exact: Boolean)

    /** Closest load ≤ [target] (just the bar when [target] is below it). */
    fun loadout(target: Double, bar: Double, plates: List<Double>): Loadout {
        if (target <= bar) return Loadout(emptyList(), bar, exact = abs(target - bar) < TOLERANCE)
        val solver = Solver(plates)
        val side = solver.platesFor(solver.bestAtMost(solver.floorUnits((target - bar) / 2)))
        val total = bar + 2 * side.sum()
        return Loadout(side, total, exact = abs(total - target) < TOLERANCE)
    }

    /** The loadable total nearest to [target] (ties go lower); never below the empty bar. */
    fun nearestLoadable(target: Double, bar: Double, plates: List<Double>): Double {
        if (target <= bar) return bar
        val solver = Solver(plates)
        val perSide = (target - bar) / 2
        val below = solver.fromUnits(solver.bestAtMost(solver.floorUnits(perSide)))
        val above = solver.bestAtLeast(solver.ceilUnits(perSide))?.let(solver::fromUnits)
        val side = if (above != null && above - perSide < perSide - below - TOLERANCE) above else below
        return bar + 2 * side
    }

    private const val TOLERANCE = 1e-6

    /** Works in integer hundredths divided by the plates' common step, so 1.25 and 2.5 are exact. */
    private class Solver(plates: List<Double>) {
        private val hundredths = plates.map { (it * SCALE).roundToInt() }.filter { it > 0 }.distinct().sortedDescending()
        private val step = hundredths.fold(0) { acc, p -> gcd(acc, p) }.coerceAtLeast(1)
        private val units = hundredths.map { it / step }

        fun floorUnits(value: Double): Int = floor(value * SCALE / step + EPSILON).toInt().coerceAtLeast(0)
        fun ceilUnits(value: Double): Int = ceil(value * SCALE / step - EPSILON).toInt().coerceAtLeast(0)
        fun fromUnits(value: Int): Double = value * step / SCALE.toDouble()

        /** fewest[i] = fewest plates summing exactly to i (-1 = impossible), up to [max]. */
        private fun fewest(max: Int): IntArray {
            val best = IntArray(max + 1) { -1 }
            best[0] = 0
            for (i in 1..max) {
                units.filter { it <= i && best[i - it] >= 0 }.forEach { p ->
                    val count = best[i - p] + 1
                    if (best[i] < 0 || count < best[i]) best[i] = count
                }
            }
            return best
        }

        fun bestAtMost(target: Int): Int {
            if (units.isEmpty()) return 0
            val best = fewest(target)
            return (target downTo 0).first { best[it] >= 0 }
        }

        fun bestAtLeast(target: Int): Int? {
            val max = target + (units.maxOrNull() ?: return null)
            val best = fewest(max)
            return (target..max).firstOrNull { best[it] >= 0 }
        }

        fun platesFor(total: Int): List<Double> {
            val best = fewest(total)
            val out = mutableListOf<Double>()
            var left = total
            while (left > 0) {
                // Heaviest plate that keeps an optimal path: gives the usual "heaviest first" loading.
                val p = units.first { it <= left && best[left - it] == best[left] - 1 }
                out += fromUnits(p)
                left -= p
            }
            return out
        }

        private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        companion object {
            const val SCALE = 100
            const val EPSILON = 1e-6
        }
    }
}

/** Ramp-up sets before the working weight. */
object WarmupGenerator {
    /** (fraction of working weight, reps). */
    val STEPS = listOf(0.4 to 10, 0.6 to 5, 0.8 to 3)

    /**
     * Warm-up weights in the display unit for [working]. With a barbell they snap to loads the plates can
     * make; otherwise to [increment]. Duplicates (e.g. light weights that all round to the bar) are dropped.
     */
    fun generate(working: Double, increment: Double, barbell: Barbell? = null): List<Pair<Double, Int>> =
        STEPS.map { (fraction, reps) -> weight(working * fraction, increment, barbell) to reps }
            .filter { it.first > 0 && it.first < working }
            .distinctBy { it.first }

    /** [raw] snapped to a loadable barbell weight, or to [increment]. */
    fun weight(raw: Double, increment: Double, barbell: Barbell?): Double =
        barbell?.let { PlateCalculator.nearestLoadable(raw, it.bar, it.plates) } ?: Units.roundToIncrement(raw, increment)

    data class Barbell(val bar: Double, val plates: List<Double>)
}
