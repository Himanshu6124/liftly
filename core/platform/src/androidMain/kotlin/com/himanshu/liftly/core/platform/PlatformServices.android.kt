package com.himanshu.liftly.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private class AndroidHaptics(context: Context) : Haptics {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun light() = vibrate(VibrationEffect.EFFECT_TICK, fallbackMs = 10)
    override fun success() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK, fallbackMs = 30)

    private fun vibrate(effect: Int, fallbackMs: Long) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            v.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

private class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private class AndroidShareService(private val context: Context) : ShareService {
    override fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun shareFile(path: String, mime: String) {
        // File sharing needs a FileProvider; added together with backup/export (Phase 3).
        shareText(path)
    }
}

actual val platformServicesModule: Module = module {
    single<Haptics> { AndroidHaptics(androidContext()) }
    single<Notifier> { AndroidNotifier(androidContext()) }
    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }
    single<ShareService> { AndroidShareService(androidContext()) }
    single<OngoingWorkoutIndicator> { AndroidOngoingWorkoutIndicator(androidContext()) }
    single<NativeSignIn> { AndroidNativeSignIn(androidContext()) }
    single<SecureStore> { AndroidSecureStore(androidContext()) }
}
