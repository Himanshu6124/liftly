package com.himanshu.liftly.feature.home.di

import com.himanshu.liftly.feature.home.HomeViewModel
import com.himanshu.liftly.feature.home.widget.HomeWidgetPublisher
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    single { HomeWidgetPublisher(get(), get(), get(), get(), get(), get()) }
}
