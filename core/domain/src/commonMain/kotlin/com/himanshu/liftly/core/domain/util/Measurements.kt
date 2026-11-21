package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.MeasureKind
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.SetFormatter.symbol

/**
 * Display units for body measurements: stored as kg / cm / %, shown in kg + cm or lb + in
 * following the weight-unit setting (one switch for "metric vs imperial").
 */
object Measurements {
    const val CM_PER_INCH = 2.54

    fun toDisplay(value: Double, kind: MeasureKind, unit: WeightUnit): Double = when (kind) {
        MeasureKind.WEIGHT -> Units.kgToDisplay(value, unit)
        MeasureKind.LENGTH -> if (unit == WeightUnit.KG) value else value / CM_PER_INCH
        MeasureKind.PERCENT -> value
    }

    fun fromDisplay(value: Double, kind: MeasureKind, unit: WeightUnit): Double = when (kind) {
        MeasureKind.WEIGHT -> Units.displayToKg(value, unit)
        MeasureKind.LENGTH -> if (unit == WeightUnit.KG) value else value * CM_PER_INCH
        MeasureKind.PERCENT -> value
    }

    fun symbol(kind: MeasureKind, unit: WeightUnit): String = when (kind) {
        MeasureKind.WEIGHT -> unit.symbol
        MeasureKind.LENGTH -> if (unit == WeightUnit.KG) "cm" else "in"
        MeasureKind.PERCENT -> "%"
    }

    /** Plausible input range in the display unit, to catch typos (e.g. 800 instead of 80.0). */
    fun isPlausible(value: Double, kind: MeasureKind): Boolean = when (kind) {
        MeasureKind.WEIGHT -> value in 1.0..MAX_WEIGHT
        MeasureKind.LENGTH -> value in 1.0..MAX_LENGTH
        MeasureKind.PERCENT -> value in 1.0..MAX_PERCENT
    }

    /** "80.4", "80" — one decimal at most. */
    fun format(value: Double): String {
        val rounded = kotlin.math.round(value * 10) / 10
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private const val MAX_WEIGHT = 700.0
    private const val MAX_LENGTH = 300.0
    private const val MAX_PERCENT = 75.0
}
