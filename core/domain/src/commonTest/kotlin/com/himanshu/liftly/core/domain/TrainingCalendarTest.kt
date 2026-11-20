package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import com.himanshu.liftly.core.domain.util.TrainingCalendar
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TrainingCalendarTest {
    private val zone = TimeZone.UTC
    private val now = at(2026, 11, 21) // Saturday

    private fun at(y: Int, m: Int, d: Int) = LocalDateTime(y, m, d, 18, 0).toInstant(zone)
    private fun item(start: Instant, sets: Int = 10) =
        WorkoutHistoryItem(Workout(start.toString(), "", null, start, start, null), 1, 1, sets, 0.0, 0)

    @Test
    fun gridStartsOnAMondayAndEndsToday() {
        val cal = TrainingCalendar.from(emptyList(), goal = 3, now = now, zone = zone)
        assertEquals(DayOfWeek.MONDAY, cal.days.first().date.dayOfWeek)
        assertEquals(LocalDate(2026, 11, 21), cal.days.last().date)
        assertEquals(15 * 7 + 6, cal.days.size) // 15 full weeks + Mon..Sat
        assertEquals(0, cal.streakWeeks)
    }

    @Test
    fun countsWorkoutsAndSetsPerDay() {
        val cal = TrainingCalendar.from(listOf(item(at(2026, 11, 18), 12), item(at(2026, 11, 18), 8)), 3, now, zone)
        val day = cal.days.single { it.date == LocalDate(2026, 11, 18) }
        assertEquals(2, day.workouts)
        assertEquals(20, day.workingSets)
        assertEquals(2, cal.thisWeekCount)
    }

    @Test
    fun streakCountsConsecutiveGoalWeeksAndThisWeekOnceMet() {
        val history = listOf(
            // this week (Nov 16–21): 2 so far
            item(at(2026, 11, 17)), item(at(2026, 11, 19)),
            // last week: 2 ✓, week before: 3 ✓, then a week with 1 ✗, then 2 ✓ (not counted)
            item(at(2026, 11, 10)), item(at(2026, 11, 12)),
            item(at(2026, 11, 2)), item(at(2026, 11, 4)), item(at(2026, 11, 6)),
            item(at(2026, 10, 28)),
            item(at(2026, 10, 20)), item(at(2026, 10, 22)),
        )
        val cal = TrainingCalendar.from(history, goal = 2, now = now, zone = zone)
        assertTrue(cal.goalMet)
        assertEquals(3, cal.streakWeeks) // this week + 2 previous
        val strict = TrainingCalendar.from(history, goal = 3, now = now, zone = zone)
        assertEquals(0, strict.streakWeeks, "last week missed a goal of 3; this week not met yet")
    }
}
