package com.himanshu.liftly.android

import android.app.Application
import com.himanshu.liftly.di.initKoin
import org.koin.android.ext.koin.androidContext

class LiftlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@LiftlyApplication) }
    }
}
