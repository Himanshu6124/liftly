package com.himanshu.liftly.di

import com.himanshu.liftly.AppViewModel
import com.himanshu.liftly.core.data.di.dataModule
import com.himanshu.liftly.core.datastore.dataStoreModule
import com.himanshu.liftly.core.platform.platformServicesModule
import com.himanshu.liftly.feature.home.di.homeModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val appModule = module {
    viewModelOf(::AppViewModel)
}

val liftlyModules = listOf(
    dataModule,
    dataStoreModule,
    platformServicesModule,
    appModule,
    homeModule,
)

/** Called once by every platform entry point before the first composition. */
fun initKoin(platformConfig: KoinAppDeclaration = {}): KoinApplication = startKoin {
    platformConfig()
    modules(liftlyModules)
}
