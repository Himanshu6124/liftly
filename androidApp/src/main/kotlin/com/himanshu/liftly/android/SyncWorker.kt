package com.himanshu.liftly.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.himanshu.liftly.core.domain.sync.SyncController
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Syncs about every 6 hours while the app isn't open, only with a network connection, so another
 * device's workouts are already here when the app opens. Does nothing while signed out.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val synced = GlobalContext.get().get<SyncController>().syncNow()
        return if (synced) Result.success() else Result.retry()
    }

    companion object {
        private const val NAME = "liftly-sync"
        private const val INTERVAL_HOURS = 6L

        /** Idempotent: keeps an already scheduled job (and its timing). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
