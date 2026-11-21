package com.himanshu.liftly.feature.home.widget

import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.usecase.NextRoutine
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.TrainingCalendar
import com.himanshu.liftly.core.domain.util.WeekGrouping
import com.himanshu.liftly.core.platform.HomeWidgetBridge
import com.himanshu.liftly.core.platform.WidgetSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

/** Keeps the home-screen widgets (weekly goal, streak, start next routine) in step with the data. Started once at launch. */
class HomeWidgetPublisher(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val settings: SettingsRepository,
    private val bridge: HomeWidgetBridge,
    private val appScope: AppScope,
    private val time: TimeProvider,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = appScope.launch { snapshots().collect(bridge::update) }
    }

    internal fun snapshots(): Flow<WidgetSnapshot> = combine(
        workouts.observeHistory(),
        settings.settings.map { it.weeklyGoal }.distinctUntilChanged(),
        routines.observeRoutines(),
        workouts.observeActiveWorkout().map { it != null }.distinctUntilChanged(),
    ) { history, goal, routineList, inProgress -> snapshot(history, goal, routineList, inProgress, time.now()) }
        .distinctUntilChanged()

    internal companion object {
        fun snapshot(
            history: List<WorkoutHistoryItem>,
            goal: Int,
            routines: List<RoutineWithExercises>,
            inProgress: Boolean,
            now: Instant,
            zone: TimeZone = TimeZone.currentSystemDefault(),
        ): WidgetSnapshot {
            val calendar = TrainingCalendar.from(history, goal, now, zone)
            return WidgetSnapshot(
                thisWeekCount = calendar.thisWeekCount,
                weeklyGoal = goal,
                streakWeeks = calendar.streakWeeks,
                weekStartMillis = WeekGrouping.weekStart(now, zone).atStartOfDayIn(zone).toEpochMilliseconds(),
                nextRoutine = NextRoutine.pick(routines)?.routine?.name,
                workoutInProgress = inProgress,
            )
        }
    }
}
