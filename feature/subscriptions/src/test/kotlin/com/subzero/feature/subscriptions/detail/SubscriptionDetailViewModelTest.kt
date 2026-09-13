package com.subzero.feature.subscriptions.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CalculateSpendingHistoryUseCase
import com.subzero.core.domain.usecase.DeleteSubscriptionUseCase
import com.subzero.core.domain.usecase.SetDeclaredUsageUseCase
import com.subzero.core.domain.usecase.SetSubscriptionStatusUseCase
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SubscriptionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()

    private fun viewModel(id: String = "n") = SubscriptionDetailViewModel(
        key = SubscriptionDetailKey(id),
        repository = repository,
        calculateSpendingHistory = CalculateSpendingHistoryUseCase(clock),
        setStatus = SetSubscriptionStatusUseCase(repository, clock),
        setDeclaredUsage = SetDeclaredUsageUseCase(repository, clock),
        deleteSubscription = DeleteSubscriptionUseCase(repository),
        clock = clock,
    )

    private fun seedNetflix() {
        repository.seed(
            subscription(
                id = "n",
                name = "Netflix",
                price = usd(1549),
                billingCycle = BillingCycle.Monthly,
                anchorDate = date("2026-01-16"),
                nextBillingDate = date("2026-09-16"),
                createdAt = Instant.parse("2026-07-01T09:00:00Z"),
            ),
        )
        repository.seedPrices(
            priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2026-01-16")),
            priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-04-10")),
        )
        repository.seedPayments(
            paymentRecord(subscriptionId = "n", paidOn = date("2026-07-16")),
            paymentRecord(subscriptionId = "n", paidOn = date("2026-08-16")),
        )
    }

    @Test
    fun `loaded state carries equivalents history and price history`() = runTest {
        seedNetflix()
        viewModel().uiState.test {
            val state = awaitItem() as SubscriptionDetailUiState.Loaded
            assertThat(state.monthlyEquivalent).isEqualTo(usd(1549))
            assertThat(state.yearlyEquivalent).isEqualTo(usd(18588))
            assertThat(state.priceHistory).hasSize(2)
            assertThat(state.history.total).isEqualTo(usd(1399 * 3 + 1549 * 5))
            assertThat(state.history.hasEstimates).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown id is gone`() = runTest {
        viewModel("ghost").uiState.test {
            assertThat(awaitItem()).isEqualTo(SubscriptionDetailUiState.Gone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause resume and cancel flow through to the repository`() = runTest {
        seedNetflix()
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.pause()
            assertThat((expectMostRecentItem() as SubscriptionDetailUiState.Loaded).subscription.status).isEqualTo(SubscriptionStatus.PAUSED)

            vm.resume()
            val resumed = expectMostRecentItem() as SubscriptionDetailUiState.Loaded
            assertThat(resumed.subscription.status).isEqualTo(SubscriptionStatus.ACTIVE)
            assertThat(resumed.subscription.nextBillingDate).isEqualTo(date("2026-09-16"))

            vm.markCanceled()
            assertThat((expectMostRecentItem() as SubscriptionDetailUiState.Loaded).isCanceled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `usage change is stored`() = runTest {
        seedNetflix()
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.setUsage(DeclaredUsage.RARELY)
            assertThat((expectMostRecentItem() as SubscriptionDetailUiState.Loaded).subscription.usage).isEqualTo(DeclaredUsage.RARELY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete makes the state gone`() = runTest {
        seedNetflix()
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.delete()
            assertThat(expectMostRecentItem()).isEqualTo(SubscriptionDetailUiState.Gone)
            assertThat(repository.subscriptionsSnapshot).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
