package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.ExerciseSession
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.util.ExerciseProgress
import com.himanshu.liftly.core.domain.util.ProgressMetric
import com.himanshu.liftly.core.domain.util.ProgressRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ExerciseProgressTest {
    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)

    private fun set(w: Double?, r: Int?, type: SetType = SetType.NORMAL, dur: Int? = null) =
        WorkoutSet("s", "we", 0, w, r, dur, null, null, type)

    private fun session(daysAgo: Int, vararg sets: WorkoutSet) =
        ExerciseSession("w$daysAgo", "Push", now - daysAgo.days, sets.toList())

    @Test
    fun metricsUseWorkingSetsOnly() {
        val s = session(0, set(100.0, 5), set(110.0, 3), set(140.0, 1, SetType.WARMUP))
        assertEquals(121.0, ExerciseProgress.value(s, ProgressMetric.EST_1RM)!!, 0.01) // best set: 110 × (1 + 3/30)
        assertEquals(110.0, ExerciseProgress.value(s, ProgressMetric.BEST_WEIGHT))
        assertEquals(830.0, ExerciseProgress.value(s, ProgressMetric.VOLUME))
        assertEquals(5.0, ExerciseProgress.value(s, ProgressMetric.MAX_REPS))
        assertEquals(8.0, ExerciseProgress.value(s, ProgressMetric.TOTAL_REPS))
    }

    @Test
    fun est1rmIgnoresHighRepSets() {
        assertNull(ExerciseProgress.value(session(0, set(50.0, 15)), ProgressMetric.EST_1RM))
    }

    @Test
    fun availableMetricsFollowTheData() {
        val bodyweight = listOf(session(0, set(null, 12), set(null, 10)))
        assertEquals(listOf(ProgressMetric.MAX_REPS, ProgressMetric.TOTAL_REPS), ExerciseProgress.available(bodyweight))
        val plank = listOf(session(0, set(null, null, dur = 60)))
        assertEquals(listOf(ProgressMetric.LONGEST_SET), ExerciseProgress.available(plank))
    }

    @Test
    fun seriesIsOldestFirstAndRespectsRange() {
        val sessions = listOf(session(5, set(80.0, 5)), session(200, set(60.0, 5)), session(40, set(70.0, 5)))
        val month = ExerciseProgress.series(sessions, ProgressMetric.BEST_WEIGHT, ProgressRange.MONTH, now)
        assertEquals(listOf(80.0), month.map { it.value })
        val all = ExerciseProgress.series(sessions, ProgressMetric.BEST_WEIGHT, ProgressRange.ALL, now)
        assertEquals(listOf(60.0, 70.0, 80.0), all.map { it.value })
    }
}
