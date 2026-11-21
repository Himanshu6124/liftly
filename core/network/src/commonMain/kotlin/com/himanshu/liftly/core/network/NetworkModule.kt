package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.domain.auth.AuthRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

val networkModule = module {
    single { ApiConfig(devServerUrl) }
    single { liftlyHttpClient(get()) }
    single { AuthApi(get()) }
    single { AuthSession(get(), get(), get(), now = { Clock.System.now().toEpochMilliseconds() }) } bind AuthRepository::class
}
