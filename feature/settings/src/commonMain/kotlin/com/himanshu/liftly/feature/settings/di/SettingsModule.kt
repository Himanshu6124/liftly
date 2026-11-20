package com.himanshu.liftly.feature.settings.di

import com.himanshu.liftly.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
