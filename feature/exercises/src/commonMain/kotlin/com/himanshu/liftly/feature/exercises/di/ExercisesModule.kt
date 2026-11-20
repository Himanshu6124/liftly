package com.himanshu.liftly.feature.exercises.di

import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailViewModel
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryViewModel
import com.himanshu.liftly.feature.exercises.picker.ExercisePickerViewModel
import com.himanshu.liftly.feature.exercises.preview.FormPreviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val exercisesModule = module {
    viewModel { ExerciseLibraryViewModel(get(), get()) }
    viewModel { params -> ExerciseDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> ExercisePickerViewModel(params.get(), get(), get()) }
    viewModel { params -> FormPreviewViewModel(params.get(), get()) }
}
