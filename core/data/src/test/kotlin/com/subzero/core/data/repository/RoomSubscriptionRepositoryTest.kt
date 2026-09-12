package com.subzero.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.data.database.SubzeroDatabase
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.jpy
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** Exercises the real Room implementation (in-memory SQLite) through the repository contract. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoomSubscriptionRepositoryTest {

    private lateinit var database: SubzeroDatabase
    private lateinit var repository: RoomSubscriptionRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SubzeroDatabase::class.java)
            .setQueryCoroutineContext(dispatcher)
            .build()
        repository = RoomSubscriptionRepository(database.subscriptionDao(), dispatcher)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `round trips every field of a subscription`() = runTest(dispatcher) {
        val sub = subscription(
            id = "round-trip",
            name = "Adobe Creative Cloud",
            price = jpy(6480),
            billingCycle = BillingCycle.of(2, CycleUnit.MONTH),
            anchorDate = date("2024-02-29"),
            nextBillingDate = date("2026-10-29"),
            status = SubscriptionStatus.PAUSED,
            notes = "Shared with the team",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-02-01T00:00:00Z"),
            statusChangedAt = Instant.parse("2026-02-01T00:00:00Z"),
        )

        repository.addSubscription(sub, priceChange(subscriptionId = "round-trip", price = jpy(6480)))

        assertThat(repository.getSubscription(sub.id)).isEqualTo(sub)
        assertThat(repository.getSubscriptions()).containsExactly(sub)
    }

    @Test
    fun `standard cycles are restored as their singleton objects`() = runTest(dispatcher) {
        repository.addSubscription(subscription(id = "q", billingCycle = BillingCycle.Quarterly), priceChange(subscriptionId = "q"))
        assertThat(repository.getSubscription(subscription(id = "q").id)?.billingCycle).isSameInstanceAs(BillingCycle.Quarterly)
    }

    @Test
    fun `add stores the initial price change atomically`() = runTest(dispatcher) {
        val sub = subscription()
        repository.addSubscription(sub, priceChange(price = usd(1549), effectiveFrom = date("2026-09-16")))

        val history = repository.getPriceHistory(sub.id)
        assertThat(history).hasSize(1)
        assertThat(history.single().price).isEqualTo(usd(1549))
    }

    @Test
    fun `update appends a price change and keeps history ordered by date`() = runTest(dispatcher) {
        val sub = subscription(price = usd(1399))
        repository.addSubscription(sub, priceChange(price = usd(1399), effectiveFrom = date("2026-01-16")))

        repository.updateSubscription(sub.copy(price = usd(1549)), priceChange(price = usd(1549), effectiveFrom = date("2026-09-12")))
        repository.updateSubscription(sub.copy(name = "Netflix Premium"))

        assertThat(repository.getSubscription(sub.id)?.name).isEqualTo("Netflix Premium")
        assertThat(repository.getPriceHistory(sub.id).map { it.price }).containsExactly(usd(1399), usd(1549)).inOrder()
    }

    @Test
    fun `observe emits on every change`() = runTest(dispatcher) {
        repository.observeSubscriptions().test {
            assertThat(awaitItem()).isEmpty()
            val sub = subscription()
            repository.addSubscription(sub, priceChange())
            assertThat(awaitItem()).containsExactly(sub)
            repository.deleteSubscription(sub.id)
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `subscriptions are ordered by next billing date then name`() = runTest(dispatcher) {
        val later = subscription(id = "b", name = "Zeta", nextBillingDate = date("2026-09-20"))
        val sooner = subscription(id = "a", name = "alpha", nextBillingDate = date("2026-09-16"))
        val sameDay = subscription(id = "c", name = "Beta", nextBillingDate = date("2026-09-16"))
        listOf(later, sooner, sameDay).forEach { repository.addSubscription(it, priceChange(subscriptionId = it.id.value)) }

        assertThat(repository.getSubscriptions().map { it.name }).containsExactly("alpha", "Beta", "Zeta").inOrder()
    }

    @Test
    fun `delete cascades to price history and payments`() = runTest(dispatcher) {
        val sub = subscription()
        repository.addSubscription(sub, priceChange())
        repository.applyRollover(listOf(paymentRecord(paidOn = date("2026-08-16"))), emptyList())

        repository.deleteSubscription(sub.id)

        assertThat(repository.getSubscription(sub.id)).isNull()
        assertThat(repository.getPriceHistory(sub.id)).isEmpty()
        assertThat(repository.getPaymentRecords(sub.id)).isEmpty()
    }

    @Test
    fun `rollover ignores duplicate payment dates and updates subscriptions in one step`() = runTest(dispatcher) {
        val sub = subscription(nextBillingDate = date("2026-09-12"))
        repository.addSubscription(sub, priceChange())
        repository.applyRollover(listOf(paymentRecord(paidOn = date("2026-09-12"), amount = usd(1399))), emptyList())

        repository.applyRollover(
            records = listOf(
                paymentRecord(paidOn = date("2026-09-12"), amount = usd(1549), id = "dup"),
                paymentRecord(paidOn = date("2026-08-12"), id = "earlier"),
            ),
            subscriptions = listOf(sub.copy(nextBillingDate = date("2026-10-12"))),
        )

        val records = repository.getPaymentRecords(sub.id)
        assertThat(records.map { it.paidOn }).containsExactly(date("2026-08-12"), date("2026-09-12")).inOrder()
        assertThat(records.last().amount).isEqualTo(usd(1399))
        assertThat(repository.getSubscription(sub.id)?.nextBillingDate).isEqualTo(date("2026-10-12"))
    }

    @Test
    fun `payment records between dates span subscriptions`() = runTest(dispatcher) {
        val a = subscription(id = "a")
        val b = subscription(id = "b")
        repository.addSubscription(a, priceChange(subscriptionId = "a"))
        repository.addSubscription(b, priceChange(subscriptionId = "b"))
        repository.applyRollover(
            listOf(
                paymentRecord(subscriptionId = "a", paidOn = date("2026-08-01")),
                paymentRecord(subscriptionId = "b", paidOn = date("2026-08-15")),
                paymentRecord(subscriptionId = "a", paidOn = date("2026-09-01")),
            ),
            emptyList(),
        )

        val august = repository.getPaymentRecordsBetween(date("2026-08-01"), date("2026-08-31"))
        assertThat(august.map { it.subscriptionId.value }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `adding a subscription with a price change for another id is rejected`() = runTest(dispatcher) {
        val result = runCatching { repository.addSubscription(subscription(id = "x"), priceChange(subscriptionId = "y")) }
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.getSubscriptions()).isEmpty()
    }
}
