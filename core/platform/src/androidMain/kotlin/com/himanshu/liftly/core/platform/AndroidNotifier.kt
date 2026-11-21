package com.himanshu.liftly.core.platform

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlin.time.Instant

/**
 * Local notifications, posted on time by [ExactRestTimer] while the process is alive, with an
 * AlarmManager alarm + [NotificationReceiver] as the fallback when it isn't. The alarm is exact when
 * the system allows it (Android 12+ may not; then it can be a little late).
 * Notification permission (Android 13+) is requested through [NotificationPermission] the first time
 * something is scheduled, not at launch.
 */
internal class AndroidNotifier(private val context: Context) : Notifier {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val exactTimer = ExactRestTimer(context)

    override fun schedule(id: String, at: Instant, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            NotificationPermission.request()
        }
        val pending = pendingIntent(id, title, body)
        val millis = at.toEpochMilliseconds()
        exactTimer.schedule(id, millis, intent = notifyIntent(id, title, body), fire = { intent ->
            NotificationReceiver.post(context, intent)
            alarms.cancel(pending) // already alerted; the fallback must not fire again
        })
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    override fun cancel(id: String) {
        exactTimer.cancel(id)
        alarms.cancel(pendingIntent(id, "", ""))
        context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
    }

    private fun notifyIntent(id: String, title: String, body: String) = Intent(context, NotificationReceiver::class.java)
        .setAction(ACTION_NOTIFY)
        .putExtra(EXTRA_ID, id)
        .putExtra(EXTRA_TITLE, title)
        .putExtra(EXTRA_BODY, body)

    private fun pendingIntent(id: String, title: String, body: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        notifyIntent(id, title, body),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        internal const val ACTION_NOTIFY = "com.himanshu.liftly.NOTIFY"
        internal const val EXTRA_ID = "id"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_BODY = "body"
        internal const val CHANNEL_ID = "rest_timer"
    }
}

/** Posts the notification when the fallback alarm fires. Declared in the app manifest. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AndroidNotifier.ACTION_NOTIFY) post(context, intent)
    }

    internal companion object {
        fun post(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(AndroidNotifier.CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_HIGH),
            )
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
            }
            val id = intent.getStringExtra(AndroidNotifier.EXTRA_ID).orEmpty()
            val notification = Notification.Builder(context, AndroidNotifier.CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(intent.getStringExtra(AndroidNotifier.EXTRA_TITLE))
                .setContentText(intent.getStringExtra(AndroidNotifier.EXTRA_BODY))
                .setAutoCancel(true)
                .setContentIntent(open)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOnlyAlertOnce(true)
                .build()
            manager.notify(id.hashCode(), notification)
        }
    }
}

/** The activity registers how to ask for the notification permission; asked at most once per launch. */
object NotificationPermission {
    private var requester: (() -> Unit)? = null
    private var asked = false

    fun register(request: () -> Unit) {
        requester = request
    }

    fun unregister() {
        requester = null
    }

    internal fun request() {
        if (asked) return
        asked = true
        requester?.invoke()
    }
}
