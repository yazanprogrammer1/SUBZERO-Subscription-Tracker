package com.subzero.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CalculatePotentialSavingsUseCase
import com.subzero.core.domain.usecase.CalculateSpendSummaryUseCase
import com.subzero.core.domain.usecase.CalculateSpendTrendUseCase
import com.subzero.core.domain.usecase.CalculateSpendingHistoryUseCase
import com.subzero.core.domain.usecase.GenerateInsightsUseCase
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()
    private val preferences = FakeUserPreferencesRepository()

    private fun viewModel(): HomeViewModel {
        val upcoming = GetUpcomingPaymentsUseCase(clock)
        return HomeViewModel(
            repository = repository,
            preferences = preferences,
            calculateSpendSummary = CalculateSpendSummaryUseCase(),
            getUpcomingPayments = upcoming,
            calculateSpendTrend = CalculateSpendTrendUseCase(clock, CalculateSpendingHistoryUseCase(clock)),
            calculatePotentialSavings = CalculatePotentialSavingsUseCase(),
            generateInsights = GenerateInsightsUseCase(upcoming, clock),
            clock = clock,
        )
    }

    @Test
    fun `no subscriptions yields the empty state with the display name`() = runTest {
        preferences.setDisplayName("Alex")
        viewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Empty("Alex"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dashboard aggregates summary next payment trend savings and insight`() = runTest {
        val installed = Instant.parse("2026-08-01T00:00:00Z")
        repository.seed(
            subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-01-16"), nextBillingDate = date("2026-09-16"), createdAt = installed),
            subscription(id = "s", name = "Spotify", price = usd(1199), anchorDate = date("2026-06-05"), nextBillingDate = date("2026-10-05"), usage = DeclaredUsage.RARELY, createdAt = installed),
        )
        repository.seedPrices(
            priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2026-01-16")),
            priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-08-20")),
            priceChange(subscriptionId = "s", price = usd(1199), effectiveFrom = date("2026-06-05")),
        )
        repository.seedPayments(
            paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-08-16")),
            paymentRecord(subscriptionId = "s", amount = usd(1199), paidOn = date("2026-09-05")),
        )

        viewModel().uiState.test {
            val state = awaitItem() as HomeUiState.Dashboard
            assertThat(state.summary.monthly).isEqualTo(usd(1549 + 1199))
            assertThat(state.summary.yearly).isEqualTo(usd((1549 + 1199) * 12))
            assertThat(state.nextPayment?.subscription?.name).isEqualTo("Netflix")
            assertThat(state.nextPayment?.date).isEqualTo(date("2026-09-16"))
            assertThat(state.trend.months).hasSize(6)
            assertThat(state.trend.current.total).isEqualTo(usd(1199 + 1549))
            assertThat(state.savings.monthly).isEqualTo(usd(1199))
            // The savings card already shows potential savings, so the insight slot shows the next one.
            assertThat(state.insight).isInstanceOf(Insight.PriceIncrease::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dashboard updates when a subscription is added`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertThat(awaitItem()).isInstanceOf(HomeUiState.Empty::class.java)
            repository.seed(subscription(id = "n", price = usd(1549), anchorDate = date("2026-09-16")))
            val state = expectMostRecentItem() as HomeUiState.Dashboard
            assertThat(state.summary.monthly).isEqualTo(usd(1549))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
