package com.himanshu.liftly.feature.workout.di

import com.himanshu.liftly.feature.workout.active.ActiveWorkoutViewModel
import com.himanshu.liftly.feature.workout.ongoing.OngoingWorkoutPublisher
import com.himanshu.liftly.feature.workout.rest.NotifierRestAlarm
import com.himanshu.liftly.feature.workout.rest.RestAlarm
import com.himanshu.liftly.feature.workout.summary.WorkoutSummaryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val workoutModule = module {
    single<RestAlarm> { NotifierRestAlarm(get(), get()) }
    single { OngoingWorkoutPublisher(get(), get(), get(), get()) }
    viewModelOf(::ActiveWorkoutViewModel)
    viewModel { params -> WorkoutSummaryViewModel(params.get(), get(), get(), get(), get()) }
}
