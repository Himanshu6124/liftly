package com.himanshu.liftly.feature.exercises.di

import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailViewModel
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val exercisesModule = module {
    viewModel { ExerciseLibraryViewModel(get(), get()) }
    viewModel { params -> ExerciseDetailViewModel(params.get(), get(), get(), get()) }
}
