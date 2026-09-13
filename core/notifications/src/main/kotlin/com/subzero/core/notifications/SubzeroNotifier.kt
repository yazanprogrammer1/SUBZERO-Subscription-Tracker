package com.subzero.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.subzero.core.domain.format.MoneyFormatter
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.UpcomingPayment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** How the app is opened from a notification. Read by the launcher activity. */
object NotificationDeepLink {
    const val EXTRA_SUBSCRIPTION_ID = "com.subzero.extra.SUBSCRIPTION_ID"

    fun subscriptionIdFrom(intent: Intent?): String? = intent?.getStringExtra(EXTRA_SUBSCRIPTION_ID)
}

/** What the planner needs from the platform notification layer. */
interface Notifier {
    val areNotificationsEnabled: Boolean
    fun postUpcomingCharges(payments: List<UpcomingPayment>, today: java.time.LocalDate)
    fun postMonthlySummary(month: YearMonth, total: Money, subscriptionCount: Int)
    fun postSavings(monthly: Money, subscriptionCount: Int)
}

/**
 * Builds and posts SUBZERO notifications on three channels the user controls separately.
 * Posting is a no-op when the permission is missing; the caller decides whether to ask for it.
 */
@Singleton
class SubzeroNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : Notifier {
    private val manager = NotificationManagerCompat.from(context)

    val hasPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override val areNotificationsEnabled: Boolean
        get() = hasPermission && manager.areNotificationsEnabled()

    fun ensureChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                channel(CHANNEL_UPCOMING, R.string.core_notifications_channel_upcoming, R.string.core_notifications_channel_upcoming_description, NotificationManagerCompat.IMPORTANCE_DEFAULT),
                channel(CHANNEL_SUMMARY, R.string.core_notifications_channel_summary, R.string.core_notifications_channel_summary_description, NotificationManagerCompat.IMPORTANCE_LOW),
                channel(CHANNEL_INSIGHTS, R.string.core_notifications_channel_insights, R.string.core_notifications_channel_insights_description, NotificationManagerCompat.IMPORTANCE_LOW),
            ),
        )
    }

    /** One notification per charge plus a group summary when there are several. */
    override fun postUpcomingCharges(payments: List<UpcomingPayment>, today: java.time.LocalDate) {
        if (payments.isEmpty() || !areNotificationsEnabled) return
        ensureChannels()
        val formatter = moneyFormatter()
        payments.forEach { payment ->
            val whenText = whenText(ChronoUnit.DAYS.between(today, payment.date).toInt())
            val notification = base(CHANNEL_UPCOMING)
                .setContentTitle(context.getString(R.string.core_notifications_upcoming_title, payment.subscription.name, formatter.format(payment.amount), whenText))
                .setContentText(context.getString(R.string.core_notifications_upcoming_body))
                .setContentIntent(openSubscription(payment.subscription.id.value))
                .setGroup(GROUP_UPCOMING)
                .build()
            notifySafely(upcomingId(payment), notification)
        }
        if (payments.size > 1) {
            val summary = base(CHANNEL_UPCOMING)
                .setContentTitle(context.resources.getQuantityString(R.plurals.core_notifications_upcoming_group_title, payments.size, payments.size))
                .setContentIntent(openApp())
                .setGroup(GROUP_UPCOMING)
                .setGroupSummary(true)
                .build()
            notifySafely(ID_UPCOMING_GROUP, summary)
        }
    }

    override fun postMonthlySummary(month: YearMonth, total: Money, subscriptionCount: Int) {
        if (!areNotificationsEnabled) return
        ensureChannels()
        val monthText = month.format(DateTimeFormatter.ofPattern("LLLL", context.resources.configuration.locales[0]))
        val notification = base(CHANNEL_SUMMARY)
            .setContentTitle(context.getString(R.string.core_notifications_summary_title, moneyFormatter().format(total), monthText))
            .setContentText(context.resources.getQuantityString(R.plurals.core_notifications_summary_body, subscriptionCount, subscriptionCount))
            .setContentIntent(openApp())
            .build()
        notifySafely(ID_SUMMARY, notification)
    }

    override fun postSavings(monthly: Money, subscriptionCount: Int) {
        if (!areNotificationsEnabled) return
        ensureChannels()
        val notification = base(CHANNEL_INSIGHTS)
            .setContentTitle(context.getString(R.string.core_notifications_savings_title, moneyFormatter().formatCompact(monthly)))
            .setContentText(context.resources.getQuantityString(R.plurals.core_notifications_savings_body, subscriptionCount, subscriptionCount))
            .setContentIntent(openApp())
            .build()
        notifySafely(ID_SAVINGS, notification)
    }

    fun cancelAll() = manager.cancelAll()

    private fun base(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.core_notifications_ic_notification)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    private fun notifySafely(id: Int, notification: android.app.Notification) {
        if (!hasPermission) return
        // Permission is checked just above; NotificationManagerCompat re-checks on API 33+.
        @Suppress("MissingPermission")
        manager.notify(id, notification)
    }

    private fun openSubscription(id: String): PendingIntent = pendingIntent(
        launchIntent().putExtra(NotificationDeepLink.EXTRA_SUBSCRIPTION_ID, id),
        requestCode = id.hashCode(),
    )

    private fun openApp(): PendingIntent = pendingIntent(launchIntent(), requestCode = 0)

    private fun launchIntent(): Intent =
        checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) { "No launcher activity" }
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }

    private fun pendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun channel(id: String, name: Int, description: Int, importance: Int) =
        NotificationChannelCompat.Builder(id, importance)
            .setName(context.getString(name))
            .setDescription(context.getString(description))
            .build()

    private fun whenText(daysAhead: Int): String = when (daysAhead) {
        0 -> context.getString(R.string.core_notifications_upcoming_today)
        1 -> context.getString(R.string.core_notifications_upcoming_tomorrow)
        else -> context.resources.getQuantityString(R.plurals.core_notifications_upcoming_in_days, daysAhead, daysAhead)
    }

    private fun moneyFormatter() = MoneyFormatter(context.resources.configuration.locales[0])

    private fun upcomingId(payment: UpcomingPayment): Int =
        ID_UPCOMING_BASE + ((payment.subscription.id.value + payment.date).hashCode() and 0xFFFF)

    companion object {
        const val CHANNEL_UPCOMING = "upcoming_charges"
        const val CHANNEL_SUMMARY = "monthly_summary"
        const val CHANNEL_INSIGHTS = "savings_insights"
        private const val GROUP_UPCOMING = "com.subzero.group.UPCOMING"
        private const val ID_UPCOMING_GROUP = 1
        private const val ID_SUMMARY = 2
        private const val ID_SAVINGS = 3
        private const val ID_UPCOMING_BASE = 1_000
    }
}
