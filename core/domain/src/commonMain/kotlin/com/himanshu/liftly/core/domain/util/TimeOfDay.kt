package com.himanshu.liftly.core.domain.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Workouts the user didn't name are stored with an empty name and labelled at display time
 * ("Morning workout"), so the label follows the UI language instead of being frozen in the DB.
 */
enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

fun timeOfDay(at: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): TimeOfDay =
    when (at.toLocalDateTime(zone).hour) {
        in 5..11 -> TimeOfDay.MORNING
        in 12..16 -> TimeOfDay.AFTERNOON
        in 17..21 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
