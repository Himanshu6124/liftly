package com.himanshu.liftly.core.network

import com.himanshu.liftly.core.data.sync.AccountSyncController
import com.himanshu.liftly.core.data.sync.RemoteSyncDataSource
import com.himanshu.liftly.core.data.sync.SyncEngine
import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.util.AppScope
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

/** Server access: session, sync transport, and the [SyncController] the repositories call after writes. */
val networkModule = module {
    single { ApiConfig(devServerUrl) }
    single { liftlyHttpClient(get()) }
    single { AuthApi(get()) }
    single { AuthSession(get(), get(), get(), now = { Clock.System.now().toEpochMilliseconds() }) } bind AuthRepository::class
    single<RemoteSyncDataSource> { HttpSyncDataSource(get(), get()) }
    single { SyncEngine(get(), get(), get(), get()) }
    single<SyncController> { AccountSyncController(get(), get(), get(), get(), get(), scope = get<AppScope>()) }
}
