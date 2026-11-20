package com.himanshu.liftly.feature.workout.di

import com.himanshu.liftly.feature.workout.active.ActiveWorkoutViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val workoutModule = module {
    viewModelOf(::ActiveWorkoutViewModel)
}
