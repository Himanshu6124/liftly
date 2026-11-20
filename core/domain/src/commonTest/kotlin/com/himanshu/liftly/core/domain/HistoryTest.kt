package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ActiveWorkoutExercise
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.PersonalRecord
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutExercise
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.util.RelativeWeek
import com.himanshu.liftly.core.domain.util.WeekGrouping
import com.himanshu.liftly.core.domain.util.WorkoutStats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class HistoryTest {
    private val zone = TimeZone.UTC
    private fun at(y: Int, m: Int, d: Int, h: Int = 10) = LocalDateTime(y, m, d, h, 0).toInstant(zone)
    private fun item(id: String, start: Instant) = WorkoutHistoryItem(Workout(id, "", null, start, start, null), 1, 1, 3, 100.0, 0)

    @Test
    fun weekBoundsAreMondayToMondayWithOffset() {
        val now = at(2026, 11, 21) // Saturday
        val (start, from, to) = WeekGrouping.weekBounds(now, 0, zone)
        assertEquals(LocalDate(2026, 11, 16), start)
        assertEquals(LocalDateTime(2026, 11, 16, 0, 0).toInstant(zone), from)
        assertEquals(LocalDateTime(2026, 11, 23, 0, 0).toInstant(zone), to)
        assertEquals(LocalDate(2026, 11, 2), WeekGrouping.weekBounds(now, -2, zone).first)
    }

    @Test
    fun groupsByMondayWeeksNewestFirst() {
        val now = at(2026, 11, 21) // Saturday
        val groups = WeekGrouping.group(
            listOf(
                item("mon", at(2026, 11, 16)),
                item("sun-before", at(2026, 11, 15)),
                item("old", at(2026, 10, 1)),
                item("sat", at(2026, 11, 21)),
            ),
            now,
            zone,
        )
        assertEquals(listOf(RelativeWeek.THIS_WEEK, RelativeWeek.LAST_WEEK, RelativeWeek.EARLIER), groups.map { it.relative })
        assertEquals(listOf("sat", "mon"), groups[0].items.map { it.workout.id })
        assertEquals(LocalDate(2026, 11, 16), groups[0].weekStart)
        assertEquals(LocalDate(2026, 11, 9), groups[1].weekStart)
    }

    @Test
    fun summaryStatsExcludeWarmupsAndSkipped() {
        val start = Instant.fromEpochMilliseconds(0)
        fun we(id: String, status: ExerciseStatus) = WorkoutExercise(id, "w", id, 0, null, status, null, InputMode.UNIFORM, null)
        fun set(w: Double, r: Int, type: SetType = SetType.NORMAL) = WorkoutSet("s$w$r$type", "x", 0, w, r, null, null, null, type)
        val bench = exercise("Bench", id = "bench", primary = Muscle.CHEST).copy(secondaryMuscles = listOf(Muscle.TRICEPS))
        val pr = PersonalRecord("p", "bench", PrType.MAX_WEIGHT, 100.0, "s", start)
        val w = ActiveWorkout(
            Workout("w", "", null, start, start + 50.minutes, null),
            listOf(
                ActiveWorkoutExercise(
                    we("bench", ExerciseStatus.LOGGED),
                    bench,
                    listOf(set(40.0, 10, SetType.WARMUP), set(100.0, 5), set(100.0, 5)),
                    listOf(pr),
                ),
                ActiveWorkoutExercise(we("row", ExerciseStatus.SKIPPED), exercise("Row", id = "row", primary = Muscle.LATS), emptyList()),
            ),
        )
        val stats = WorkoutStats.of(w)
        assertEquals(50.minutes, stats.duration)
        assertEquals(1000.0, stats.volumeKg)
        assertEquals(2, stats.workingSets)
        assertEquals(1, stats.logged)
        assertEquals(1, stats.skipped)
        assertEquals(listOf("Bench" to pr), stats.records)
        assertEquals(mapOf(Muscle.CHEST to 1f, Muscle.TRICEPS to 0.4f), stats.muscles)
    }
}
