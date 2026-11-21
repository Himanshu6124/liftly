package com.himanshu.liftly.core.platform

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/**
 * Fires the "rest is over" notification at the exact time while the app process is alive (the
 * ongoing-workout foreground service keeps it alive during a workout). A partial wake lock keeps
 * the CPU running until then, so the countdown isn't stretched by sleep. The AlarmManager alarm in
 * [AndroidNotifier] stays as the fallback for when the process is gone; it is cancelled once this
 * timer has posted, and the notification uses `setOnlyAlertOnce`, so there is never a double alert.
 */
internal class ExactRestTimer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val wakeLocks = mutableMapOf<String, PowerManager.WakeLock>()
    private val pending = mutableMapOf<String, Runnable>()

    fun schedule(id: String, atMillis: Long, fire: (Intent) -> Unit, intent: Intent) = handler.post {
        cancelOnMain(id)
        val delay = atMillis - System.currentTimeMillis()
        if (delay <= 0) return@post
        val lock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Liftly:rest-$id")
            .apply { acquire(delay + WAKE_LOCK_SLACK_MS) }
        val task = Runnable {
            pending.remove(id)
            fire(intent)
            wakeLocks.remove(id)?.takeIf { it.isHeld }?.release()
        }
        wakeLocks[id] = lock
        pending[id] = task
        handler.postDelayed(task, delay)
    }

    fun cancel(id: String) = handler.post { cancelOnMain(id) }

    private fun cancelOnMain(id: String) {
        pending.remove(id)?.let(handler::removeCallbacks)
        wakeLocks.remove(id)?.takeIf { it.isHeld }?.release()
    }

    private companion object {
        const val WAKE_LOCK_SLACK_MS = 10_000L
    }
}
