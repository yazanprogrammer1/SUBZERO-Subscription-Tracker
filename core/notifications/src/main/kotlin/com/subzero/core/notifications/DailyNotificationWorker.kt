package com.subzero.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.CalculatePotentialSavingsUseCase
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.domain.usecase.RollForwardBillingDatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/**
 * The once-a-day job. Order matters: the rollover first so dates are current, then reminders.
 * Every send is gated by the user's preference for that kind and recorded in the ledger, so a
 * second run on the same day (app launch after the periodic run) sends nothing new.
 */
@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val planner: NotificationPlanner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching { planner.run() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        const val UNIQUE_NAME = "subzero.daily"
    }
}

/**
 * The decisions, kept out of the worker so they can be unit-tested without WorkManager.
 */
class NotificationPlanner @Inject constructor(
    private val repository: SubscriptionRepository,
    private val preferences: UserPreferencesRepository,
    private val rollForward: RollForwardBillingDatesUseCase,
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val calculatePotentialSavings: CalculatePotentialSavingsUseCase,
    private val ledger: NotificationLedger,
    private val notifier: Notifier,
    private val clock: Clock,
) {
    suspend fun run() {
        val today = LocalDate.now(clock)
        rollForward(today)
        val prefs = preferences.preferences.first()
        val notifications = prefs.notifications
        if (!notifier.areNotificationsEnabled) return
        val subscriptions = repository.getSubscriptions()

        if (notifications.upcomingChargeEnabled) {
            val target = today.plusDays(notifications.upcomingChargeDaysBefore.toLong())
            val due = getUpcomingPayments(subscriptions, from = target, to = target)
                .filterNot { ledger.wasSent(NotificationKind.UPCOMING, "${it.subscription.id.value}:${it.date}") }
            if (due.isNotEmpty()) {
                notifier.postUpcomingCharges(due, today)
                due.forEach { ledger.markSent(NotificationKind.UPCOMING, "${it.subscription.id.value}:${it.date}", today) }
            }
        }

        // Windows of a few days so a device that was off on the day still gets the note once.
        if (notifications.monthlySummaryEnabled && today.dayOfMonth in SUMMARY_DAYS) {
            val lastMonth = YearMonth.from(today).minusMonths(1)
            if (!ledger.wasSent(NotificationKind.MONTHLY_SUMMARY, lastMonth.toString())) {
                val records = repository.getPaymentRecordsBetween(lastMonth.atDay(1), lastMonth.atEndOfMonth())
                    .filter { it.amount.currency == prefs.homeCurrency }
                if (records.isNotEmpty()) {
                    val total = records.fold(Money.zero(prefs.homeCurrency)) { acc, r -> acc + r.amount }
                    notifier.postMonthlySummary(lastMonth, total, records.map { it.subscriptionId }.distinct().size)
                    ledger.markSent(NotificationKind.MONTHLY_SUMMARY, lastMonth.toString(), today)
                }
            }
        }

        if (notifications.savingsInsightsEnabled && today.dayOfMonth in SAVINGS_DAYS) {
            val key = YearMonth.from(today).toString()
            if (!ledger.wasSent(NotificationKind.SAVINGS, key)) {
                val savings = calculatePotentialSavings(subscriptions, prefs.homeCurrency)
                if (!savings.isEmpty) {
                    notifier.postSavings(savings.monthly, savings.subscriptions.size)
                    ledger.markSent(NotificationKind.SAVINGS, key, today)
                }
            }
        }

        ledger.prune(keepAfter = today.minusDays(LEDGER_KEEP_DAYS))
    }

    private companion object {
        val SUMMARY_DAYS = 1..3
        val SAVINGS_DAYS = 15..17
        const val LEDGER_KEEP_DAYS = 45L
    }
}
