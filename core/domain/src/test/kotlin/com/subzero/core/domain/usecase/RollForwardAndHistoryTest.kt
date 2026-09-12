package com.subzero.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.PaymentSource
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class RollForwardAndHistoryTest {

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()

    // --- rollover ---

    @Test
    fun `rollover records each missed charge and moves the next date past today`() = runTest {
        // Weekly, last projected charge was Aug 29; the app has not run since.
        val weekly = subscription(id = "w", billingCycle = BillingCycle.Weekly, anchorDate = date("2026-08-01"), nextBillingDate = date("2026-08-29"))
        repository.seed(weekly)

        val result = RollForwardBillingDatesUseCase(repository, clock)()

        assertThat(result).isEqualTo(RollForwardBillingDatesUseCase.Result(recordedPayments = 3, updatedSubscriptions = 1))
        assertThat(repository.paymentsSnapshot.map { it.paidOn }).containsExactly(
            date("2026-08-29"), date("2026-09-05"), date("2026-09-12"),
        )
        assertThat(repository.paymentsSnapshot.all { it.source == PaymentSource.RECORDED && it.amount == weekly.price }).isTrue()
        assertThat(repository.subscriptionsSnapshot.single().nextBillingDate).isEqualTo(date("2026-09-19"))
    }

    @Test
    fun `rollover on the charge day records today and projects the next occurrence`() = runTest {
        val monthly = subscription(anchorDate = date("2026-08-12"), nextBillingDate = date("2026-09-12"))
        repository.seed(monthly)

        RollForwardBillingDatesUseCase(repository, clock)()

        assertThat(repository.paymentsSnapshot.single().paidOn).isEqualTo(today)
        assertThat(repository.subscriptionsSnapshot.single().nextBillingDate).isEqualTo(date("2026-10-12"))
    }

    @Test
    fun `rollover is idempotent`() = runTest {
        repository.seed(subscription(anchorDate = date("2026-08-12"), nextBillingDate = date("2026-09-12")))
        val useCase = RollForwardBillingDatesUseCase(repository, clock)

        useCase()
        val second = useCase()

        assertThat(second).isEqualTo(RollForwardBillingDatesUseCase.Result(0, 0))
        assertThat(repository.paymentsSnapshot).hasSize(1)
    }

    @Test
    fun `rollover never duplicates a record for the same subscription and date`() = runTest {
        repository.seed(subscription(anchorDate = date("2026-08-12"), nextBillingDate = date("2026-09-12")))
        repository.seedPayments(paymentRecord(paidOn = today, amount = usd(1399)))

        RollForwardBillingDatesUseCase(repository, clock)()

        assertThat(repository.paymentsSnapshot).hasSize(1)
        assertThat(repository.paymentsSnapshot.single().amount).isEqualTo(usd(1399))
    }

    @Test
    fun `rollover ignores future paused and canceled subscriptions`() = runTest {
        repository.seed(
            subscription(id = "future", nextBillingDate = date("2026-09-13")),
            subscription(id = "paused", nextBillingDate = date("2026-09-01"), status = SubscriptionStatus.PAUSED),
            subscription(id = "canceled", nextBillingDate = date("2026-09-01"), status = SubscriptionStatus.CANCELED),
        )

        val result = RollForwardBillingDatesUseCase(repository, clock)()

        assertThat(result).isEqualTo(RollForwardBillingDatesUseCase.Result(0, 0))
        assertThat(repository.paymentsSnapshot).isEmpty()
    }

    @Test
    fun `rollover handles month end anchors correctly`() = runTest {
        // Anchored Jan 31, last run before Feb; today is Sep 12 -> Feb 28, Mar 31, ..., Aug 31 recorded.
        repository.seed(subscription(anchorDate = date("2026-01-31"), nextBillingDate = date("2026-02-28")))

        RollForwardBillingDatesUseCase(repository, clock)()

        assertThat(repository.paymentsSnapshot.map { it.paidOn }).containsExactly(
            date("2026-02-28"), date("2026-03-31"), date("2026-04-30"), date("2026-05-31"),
            date("2026-06-30"), date("2026-07-31"), date("2026-08-31"),
        ).inOrder()
        assertThat(repository.subscriptionsSnapshot.single().nextBillingDate).isEqualTo(date("2026-09-30"))
    }

    // --- spending history ---

    @Test
    fun `history combines recorded payments with labelled estimates before the app knew`() {
        // Subscribed since Jan 16 2026, added to SUBZERO on Jul 1 2026, price rose in April.
        val sub = subscription(
            anchorDate = date("2026-01-16"),
            nextBillingDate = date("2026-09-16"),
            createdAt = Instant.parse("2026-07-01T09:00:00Z"),
        )
        val prices = listOf(
            priceChange(price = usd(1399), effectiveFrom = date("2026-01-16")),
            priceChange(price = usd(1549), effectiveFrom = date("2026-04-10")),
        )
        val records = listOf(
            paymentRecord(paidOn = date("2026-07-16"), amount = usd(1549)),
            paymentRecord(paidOn = date("2026-08-16"), amount = usd(1549)),
        )

        val history = CalculateSpendingHistoryUseCase(clock)(sub, prices, records, months = 12)

        assertThat(history.months).hasSize(12)
        assertThat(history.months.first().month).isEqualTo(YearMonth.of(2025, 10))
        assertThat(history.months.last().month).isEqualTo(YearMonth.of(2026, 9))

        val byMonth = history.months.associateBy { it.month }
        assertThat(byMonth.getValue(YearMonth.of(2025, 12)).total).isEqualTo(usd(0))
        assertThat(byMonth.getValue(YearMonth.of(2026, 1)).total).isEqualTo(usd(1399))
        assertThat(byMonth.getValue(YearMonth.of(2026, 1)).estimated).isEqualTo(usd(1399))
        assertThat(byMonth.getValue(YearMonth.of(2026, 3)).total).isEqualTo(usd(1399))
        assertThat(byMonth.getValue(YearMonth.of(2026, 4)).total).isEqualTo(usd(1549))
        assertThat(byMonth.getValue(YearMonth.of(2026, 6)).estimated).isEqualTo(usd(1549))
        assertThat(byMonth.getValue(YearMonth.of(2026, 7)).total).isEqualTo(usd(1549))
        assertThat(byMonth.getValue(YearMonth.of(2026, 7)).estimated).isEqualTo(usd(0))
        assertThat(byMonth.getValue(YearMonth.of(2026, 9)).total).isEqualTo(usd(0))

        assertThat(history.total).isEqualTo(usd(1399 * 3 + 1549 * 5))
        assertThat(history.hasEstimates).isTrue()
    }

    @Test
    fun `history has no estimates when the anchor is after installation`() {
        val sub = subscription(anchorDate = date("2026-09-16"), createdAt = Instant.parse("2026-09-01T00:00:00Z"))
        val history = CalculateSpendingHistoryUseCase(clock)(sub, listOf(priceChange()), emptyList())
        assertThat(history.total).isEqualTo(usd(0))
        assertThat(history.hasEstimates).isFalse()
    }

    @Test
    fun `history never estimates beyond today`() {
        val sub = subscription(anchorDate = date("2026-01-12"), createdAt = Instant.parse("2026-12-01T00:00:00Z"))
        val history = CalculateSpendingHistoryUseCase(clock)(sub, listOf(priceChange(effectiveFrom = date("2026-01-12"))), emptyList())
        // Jan..Sep 12 inclusive = 9 charges (Sep 12 is today).
        assertThat(history.total).isEqualTo(usd(1549 * 9))
    }

    @Test
    fun `history ignores records in another currency`() {
        val sub = subscription(anchorDate = date("2026-09-16"), createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val records = listOf(paymentRecord(paidOn = date("2026-08-01"), amount = com.subzero.core.domain.testing.eur(500)))
        val history = CalculateSpendingHistoryUseCase(clock)(sub, emptyList(), records)
        assertThat(history.total).isEqualTo(usd(0))
    }
}
