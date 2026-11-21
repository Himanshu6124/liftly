package com.himanshu.liftly.android

import android.app.Application
import com.himanshu.liftly.android.widget.AndroidHomeWidgetBridge
import com.himanshu.liftly.core.platform.AndroidSignIn
import com.himanshu.liftly.core.platform.HomeWidgetBridge
import com.himanshu.liftly.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class LiftlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSignIn.serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
        initKoin {
            androidContext(this@LiftlyApplication)
            // The Glance widget lives in this module, so its bridge is bound here.
            modules(module { single<HomeWidgetBridge> { AndroidHomeWidgetBridge(this@LiftlyApplication) } })
        }
    }
}
