package com.himanshu.liftly.core.datastore

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataStoreModule: Module = module {
    single { createDataStore(androidContext().filesDir.resolve(SETTINGS_FILE).absolutePath) }
}
