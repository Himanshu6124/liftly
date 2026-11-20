package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** One day of the Home heatmap. */
data class TrainingDay(val date: LocalDate, val workouts: Int, val workingSets: Int)

/**
 * Home's consistency card: a day grid of the last [WEEKS] weeks (Monday-first columns, oldest
 * first, ending today), this week's workout count against the goal, and the weekly streak.
 */
data class TrainingCalendar(
    val days: List<TrainingDay>,
    val today: LocalDate,
    val thisWeekCount: Int,
    val goal: Int,
    /** Consecutive weeks (ending last week, plus this week once its goal is met) with ≥ [goal] workouts. */
    val streakWeeks: Int,
) {
    val goalMet: Boolean get() = thisWeekCount >= goal

    companion object {
        const val WEEKS = 16

        fun from(
            history: List<WorkoutHistoryItem>,
            goal: Int,
            now: Instant,
            zone: TimeZone = TimeZone.currentSystemDefault(),
        ): TrainingCalendar {
            val today = now.toLocalDateTime(zone).date
            val byDay = history.groupBy { it.workout.startedAt.toLocalDateTime(zone).date }
            val thisWeek = WeekGrouping.weekStart(now, zone)
            val first = thisWeek.minus(DatePeriod(days = 7 * (WEEKS - 1)))
            val days = generateSequence(first) { it.plus(DatePeriod(days = 1)) }
                .takeWhile { it <= today }
                .map { d -> byDay[d].orEmpty().let { TrainingDay(d, it.size, it.sumOf { w -> w.workingSets }) } }
                .toList()

            val perWeek = history.groupingBy { WeekGrouping.weekStart(it.workout.startedAt, zone) }.eachCount()
            val thisWeekCount = perWeek[thisWeek] ?: 0
            var streak = 0
            var week = thisWeek.minus(DatePeriod(days = 7))
            while ((perWeek[week] ?: 0) >= goal) {
                streak++
                week = week.minus(DatePeriod(days = 7))
            }
            if (thisWeekCount >= goal) streak++

            return TrainingCalendar(days, today, thisWeekCount, goal, streak)
        }
    }
}
