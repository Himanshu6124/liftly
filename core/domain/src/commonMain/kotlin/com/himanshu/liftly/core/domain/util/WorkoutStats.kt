package com.himanshu.liftly.core.domain.util

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.PersonalRecord
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

/** Numbers for the Workout Summary screen. Volume and set counts exclude warm-ups. */
data class WorkoutStats(
    val duration: Duration?,
    val volumeKg: Double,
    val workingSets: Int,
    val logged: Int,
    val skipped: Int,
    val records: List<Pair<String, PersonalRecord>>,
    /** 1.0 = primary muscle of a logged exercise, 0.4 = secondary only. */
    val muscles: Map<Muscle, Float>,
) {
    companion object {
        fun of(w: ActiveWorkout): WorkoutStats {
            val logged = w.exercises.filter { it.workoutExercise.status == ExerciseStatus.LOGGED }
            val working = logged.flatMap { it.sets }.filter { it.setType != SetType.WARMUP }
            val muscles = mutableMapOf<Muscle, Float>()
            logged.forEach { e ->
                e.exercise.secondaryMuscles.forEach { m -> muscles[m] = maxOf(muscles[m] ?: 0f, SECONDARY) }
                muscles[e.exercise.primaryMuscle] = 1f
            }
            return WorkoutStats(
                duration = w.workout.endedAt?.let { it - w.workout.startedAt },
                volumeKg = Strength.volume(working.sortedBy { it.orderIndex }.let { sets -> sets.toDraftSets() }),
                workingSets = working.size,
                logged = logged.size,
                skipped = w.exercises.count { it.workoutExercise.status == ExerciseStatus.SKIPPED },
                records = logged.flatMap { e -> e.personalRecords.map { e.exercise.name to it } },
                muscles = muscles,
            )
        }

        private const val SECONDARY = 0.4f
    }
}

/** History sections: Monday-based weeks, newest first. */
data class WeekGroup(val weekStart: LocalDate, val relative: RelativeWeek, val items: List<WorkoutHistoryItem>)

enum class RelativeWeek { THIS_WEEK, LAST_WEEK, EARLIER }

object WeekGrouping {
    private const val DAYS_PER_WEEK = 7

    fun group(items: List<WorkoutHistoryItem>, now: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): List<WeekGroup> {
        val thisWeek = weekStart(now, zone)
        val lastWeek = thisWeek.minus(DatePeriod(days = 7))
        return items
            .sortedByDescending { it.workout.startedAt }
            .groupBy { weekStart(it.workout.startedAt, zone) }
            .map { (start, list) ->
                val relative = when (start) {
                    thisWeek -> RelativeWeek.THIS_WEEK
                    lastWeek -> RelativeWeek.LAST_WEEK
                    else -> RelativeWeek.EARLIER
                }
                WeekGroup(start, relative, list)
            }
            .sortedByDescending { it.weekStart }
    }

    /** Monday 00:00 of the week [offset] weeks from the current one (0 = this week, -1 = last) and the next Monday. */
    fun weekBounds(now: Instant, offset: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Triple<LocalDate, Instant, Instant> {
        val start = weekStart(now, zone).plus(DatePeriod(days = DAYS_PER_WEEK * offset))
        val end = start.plus(DatePeriod(days = DAYS_PER_WEEK))
        return Triple(start, start.atStartOfDayIn(zone), end.atStartOfDayIn(zone))
    }

    fun weekStart(at: Instant, zone: TimeZone): LocalDate {
        val date = at.toLocalDateTime(zone).date
        val daysFromMonday = date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        return date.minus(DatePeriod(days = daysFromMonday))
    }
}
