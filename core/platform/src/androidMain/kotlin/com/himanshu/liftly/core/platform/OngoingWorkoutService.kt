package com.himanshu.liftly.core.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import co.touchlab.kermit.Logger

/**
 * Keeps the ongoing "workout in progress" notification (elapsed chronometer, or a countdown while
 * resting). Being a foreground service also keeps the process alive, so [ExactRestTimer] can post
 * "rest is over" on time. Type `specialUse`: none of the predefined types (health needs sensors)
 * covers a user-started workout timer.
 */
class OngoingWorkoutService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workout = AndroidOngoingWorkoutIndicator.current
        if (workout == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = OngoingWorkoutNotification.build(this, workout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(OngoingWorkoutNotification.ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(OngoingWorkoutNotification.ID, notification)
        }
        return START_NOT_STICKY
    }
}

internal class AndroidOngoingWorkoutIndicator(private val context: Context) : OngoingWorkoutIndicator {
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    override fun show(workout: OngoingWorkout) {
        val wasShowing = current != null
        current = workout
        if (wasShowing) {
            manager.notify(OngoingWorkoutNotification.ID, OngoingWorkoutNotification.build(context, workout))
            return
        }
        // Only reached from the app in the foreground (starting or resuming a workout), where
        // starting a foreground service is allowed; anything else is logged, not fatal.
        runCatching { context.startForegroundService(Intent(context, OngoingWorkoutService::class.java)) }
            .onFailure { Logger.withTag("OngoingWorkout").w(it) { "Could not start the ongoing notification" } }
    }

    override fun end() {
        if (current == null) return
        current = null
        context.stopService(Intent(context, OngoingWorkoutService::class.java))
        manager.cancel(OngoingWorkoutNotification.ID)
    }

    companion object {
        @Volatile
        internal var current: OngoingWorkout? = null
    }
}

internal object OngoingWorkoutNotification {
    const val ID = 0x11F7
    private const val CHANNEL_ID = "ongoing_workout"

    fun build(context: Context, workout: OngoingWorkout): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout in progress", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(context, ID, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val resting = workout.restEndsAtMillis
        val progress = "${workout.loggedCount} of ${workout.totalCount} logged"
        val text = workout.nextExercise?.let { "$progress · Next: $it" } ?: progress
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(workout.title)
            .setContentText(text)
            .setSubText(if (resting != null) "Resting" else null)
            .setWhen(resting ?: workout.startedAtMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(resting != null)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_WORKOUT)
            .setContentIntent(open)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .build()
    }
}
