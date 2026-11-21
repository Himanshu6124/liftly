package com.himanshu.liftly.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.himanshu.liftly.core.platform.HomeWidgetBridge
import com.himanshu.liftly.core.platform.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stores the latest snapshot for [LiftlyWidget] (which runs in a fresh process at times) and redraws it. */
class AndroidHomeWidgetBridge(private val context: Context) : HomeWidgetBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun update(snapshot: WidgetSnapshot) {
        WidgetStore.write(context, snapshot)
        scope.launch { LiftlyWidget().updateAll(context) }
    }
}

internal object WidgetStore {
    private const val PREFS = "liftly_widget"
    private const val KEY = "snapshot"

    fun write(context: Context, snapshot: WidgetSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, snapshot.encode()).apply()
    }

    /** The stored snapshot, rolled over if a new week started since the app last wrote it. */
    fun read(context: Context, nowMillis: Long): WidgetSnapshot? {
        val stored = WidgetSnapshot.decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        return stored?.takeIf { nowMillis < it.weekStartMillis + WEEK_MILLIS } ?: stored?.rolledOver()
    }

    private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
}
