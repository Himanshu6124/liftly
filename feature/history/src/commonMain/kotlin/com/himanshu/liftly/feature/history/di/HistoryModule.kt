package com.himanshu.liftly.feature.history.di

import com.himanshu.liftly.feature.history.HistoryUndo
import com.himanshu.liftly.feature.history.detail.WorkoutDetailViewModel
import com.himanshu.liftly.feature.history.list.HistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val historyModule = module {
    single { HistoryUndo() }
    viewModelOf(::HistoryViewModel)
    viewModel { params -> WorkoutDetailViewModel(params.get(), get(), get(), get()) }
}
