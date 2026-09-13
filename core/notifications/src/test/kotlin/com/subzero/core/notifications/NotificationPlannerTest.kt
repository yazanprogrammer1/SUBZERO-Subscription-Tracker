package com.subzero.core.notifications

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CalculatePotentialSavingsUseCase
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.domain.usecase.RollForwardBillingDatesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class NotificationPlannerTest {

    private class FakeNotifier(override var areNotificationsEnabled: Boolean = true) : Notifier {
        val upcoming = mutableListOf<List<UpcomingPayment>>()
        val summaries = mutableListOf<Triple<YearMonth, Money, Int>>()
        val savings = mutableListOf<Pair<Money, Int>>()
        override fun postUpcomingCharges(payments: List<UpcomingPayment>, today: LocalDate) { upcoming += payments }
        override fun postMonthlySummary(month: YearMonth, total: Money, subscriptionCount: Int) { summaries += Triple(month, total, subscriptionCount) }
        override fun postSavings(monthly: Money, subscriptionCount: Int) { savings += monthly to subscriptionCount }
    }

    private class FakeLedger : NotificationLedger {
        val sent = mutableMapOf<String, LocalDate>()
        override suspend fun wasSent(kind: NotificationKind, key: String) = "${kind.name}:$key" in sent
        override suspend fun markSent(kind: NotificationKind, key: String, on: LocalDate) { sent["${kind.name}:$key"] = on }
        override suspend fun prune(keepAfter: LocalDate) { sent.entries.removeIf { it.value.isBefore(keepAfter) } }
        override suspend fun clear() = sent.clear()
    }

    private val repository = FakeSubscriptionRepository()
    private val notifier = FakeNotifier()
    private val ledger = FakeLedger()

    private fun planner(today: LocalDate, prefs: NotificationPreferences): NotificationPlanner {
        val clock = fixedClock(today)
        return NotificationPlanner(
            repository = repository,
            preferences = FakeUserPreferencesRepository(UserPreferences(CurrencyCode.USD, ThemeMode.SYSTEM, prefs, true, null)),
            rollForward = RollForwardBillingDatesUseCase(repository, clock),
            getUpcomingPayments = GetUpcomingPaymentsUseCase(clock),
            calculatePotentialSavings = CalculatePotentialSavingsUseCase(),
            ledger = ledger,
            notifier = notifier,
            clock = clock,
        )
    }

    private val allOn = NotificationPreferences(upcomingChargeEnabled = true, upcomingChargeDaysBefore = 1, monthlySummaryEnabled = true, savingsInsightsEnabled = true)

    @Test
    fun `upcoming reminder is sent once for the charge days-before away`() = runTest {
        repository.seed(
            subscription(id = "n", name = "Netflix", anchorDate = date("2026-09-13"), nextBillingDate = date("2026-09-13")),
            subscription(id = "s", name = "Spotify", anchorDate = date("2026-09-20"), nextBillingDate = date("2026-09-20")),
        )
        val planner = planner(date("2026-09-12"), allOn)

        planner.run()
        planner.run()

        assertThat(notifier.upcoming).hasSize(1)
        assertThat(notifier.upcoming.single().map { it.subscription.name }).containsExactly("Netflix")
    }

    @Test
    fun `days before preference moves the reminder`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-09-15"), nextBillingDate = date("2026-09-15")))
        planner(date("2026-09-12"), allOn.copy(upcomingChargeDaysBefore = 3)).run()
        assertThat(notifier.upcoming).hasSize(1)
    }

    @Test
    fun `nothing is sent when notifications are disabled or not allowed`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-09-13"), nextBillingDate = date("2026-09-13")))
        planner(date("2026-09-12"), NotificationPreferences.Default).run()
        assertThat(notifier.upcoming).isEmpty()

        notifier.areNotificationsEnabled = false
        planner(date("2026-09-12"), allOn).run()
        assertThat(notifier.upcoming).isEmpty()
    }

    @Test
    fun `rollover runs before reminders`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-08-12"), nextBillingDate = date("2026-09-12")))
        planner(date("2026-09-12"), allOn).run()
        assertThat(repository.paymentsSnapshot).hasSize(1)
        assertThat(repository.subscriptionsSnapshot.single().nextBillingDate).isEqualTo(date("2026-10-12"))
    }

    @Test
    fun `monthly summary goes out once in the first days of the month from last month's records`() = runTest {
        repository.seed(subscription(id = "n"), subscription(id = "s"))
        repository.seedPayments(
            paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-08-16")),
            paymentRecord(subscriptionId = "s", amount = usd(1199), paidOn = date("2026-08-24")),
            paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-07-16")),
        )
        planner(date("2026-09-02"), allOn).run()
        planner(date("2026-09-03"), allOn).run()

        assertThat(notifier.summaries).containsExactly(Triple(YearMonth.of(2026, 8), usd(2748), 2))
    }

    @Test
    fun `savings note goes out mid month only when there is something to save`() = runTest {
        repository.seed(subscription(id = "n", usage = DeclaredUsage.DAILY))
        planner(date("2026-09-15"), allOn).run()
        assertThat(notifier.savings).isEmpty()

        repository.seed(subscription(id = "s", price = usd(1199), usage = DeclaredUsage.RARELY))
        planner(date("2026-09-15"), allOn).run()
        planner(date("2026-09-16"), allOn).run()
        assertThat(notifier.savings).containsExactly(usd(1199) to 1)
    }

    @Test
    fun `ledger entries older than the window are pruned`() = runTest {
        ledger.markSent(NotificationKind.UPCOMING, "old", date("2026-01-01"))
        planner(date("2026-09-12"), allOn).run()
        assertThat(ledger.sent).doesNotContainKey("UPCOMING:old")
    }
}
