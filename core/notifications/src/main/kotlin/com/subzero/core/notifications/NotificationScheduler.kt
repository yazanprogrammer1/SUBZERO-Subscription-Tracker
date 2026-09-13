package com.subzero.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the daily notification job scheduled. */
interface NotificationScheduler {
    fun ensureScheduled()
    fun cancel()
}

/**
 * Keeps one periodic daily job enqueued, first run aimed at [RUN_AT] local time. WorkManager
 * may shift runs by its flex window and OEM battery policies; the ledger makes late or
 * duplicate runs harmless.
 */
@Singleton
class WorkManagerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : NotificationScheduler {
    override fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(Duration.ofDays(1))
            .setInitialDelay(delayUntilNextRun())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyNotificationWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(DailyNotificationWorker.UNIQUE_NAME)
    }

    private fun delayUntilNextRun(): Duration {
        val now = ZonedDateTime.now(clock)
        var next = LocalDateTime.of(now.toLocalDate(), RUN_AT).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    private companion object {
        val RUN_AT: LocalTime = LocalTime.of(9, 0)
    }
}
