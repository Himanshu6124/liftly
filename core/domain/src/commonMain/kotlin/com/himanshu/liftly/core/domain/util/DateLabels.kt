package com.himanshu.liftly.core.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Human dates for lists: "Sat, 21 Nov" this year, "Sat, 21 Nov 2025" otherwise (instead of ISO 2025-11-21). */
object DateLabels {
    private val thisYear = LocalDate.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
        chars(", ")
        day(padding = Padding.NONE)
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
    }
    private val otherYear = LocalDate.Format {
        date(thisYear)
        char(' ')
        year()
    }

    fun short(date: LocalDate, today: LocalDate): String = date.format(if (date.year == today.year) thisYear else otherYear)

    fun short(instant: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String =
        short(instant.toLocalDateTime(zone).date, Clock.System.now().toLocalDateTime(zone).date)
}
