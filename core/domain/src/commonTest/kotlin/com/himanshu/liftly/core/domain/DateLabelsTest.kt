package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.util.DateLabels
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelsTest {
    private val today = LocalDate(2026, 11, 21)

    @Test
    fun thisYearHasNoYear() = assertEquals("Sat, 21 Nov", DateLabels.short(LocalDate(2026, 11, 21), today))

    @Test
    fun singleDigitDayIsNotPadded() = assertEquals("Mon, 2 Mar", DateLabels.short(LocalDate(2026, 3, 2), today))

    @Test
    fun otherYearShowsYear() = assertEquals("Fri, 21 Nov 2025", DateLabels.short(LocalDate(2025, 11, 21), today))
}
