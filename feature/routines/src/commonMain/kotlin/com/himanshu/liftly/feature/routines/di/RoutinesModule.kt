package com.himanshu.liftly.feature.routines.di

import com.himanshu.liftly.feature.routines.editor.RoutineEditorViewModel
import com.himanshu.liftly.feature.routines.list.RoutinesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val routinesModule = module {
    viewModelOf(::RoutinesViewModel)
    viewModel { params -> RoutineEditorViewModel(params.getOrNull(), get(), get(), get(), get()) }
}
