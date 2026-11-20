package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.WeightUnit
import kotlin.math.roundToLong

object Units {
    const val KG_PER_LB = 0.45359237

    fun kgToDisplay(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> kg
        WeightUnit.LB -> kg / KG_PER_LB
    }

    fun displayToKg(value: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> value
        WeightUnit.LB -> value * KG_PER_LB
    }

    /** Rounds [value] to the nearest multiple of [increment] (e.g. 2.5 kg plates). */
    fun roundToIncrement(value: Double, increment: Double): Double {
        if (increment <= 0.0) return value
        return (value / increment).roundToLong() * increment
    }
}
