package com.subzero.feature.insights

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CalculateCategoryBreakdownUseCase
import com.subzero.core.domain.usecase.CalculateSpendSummaryUseCase
import com.subzero.core.domain.usecase.GenerateInsightsUseCase
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = fixedClock(date("2026-09-12"))
    private val repository = FakeSubscriptionRepository()
    private val preferences = FakeUserPreferencesRepository()

    private fun viewModel() = InsightsViewModel(
        repository = repository,
        preferences = preferences,
        generateInsights = GenerateInsightsUseCase(GetUpcomingPaymentsUseCase(clock), clock),
        calculateSpendSummary = CalculateSpendSummaryUseCase(),
        calculateCategoryBreakdown = CalculateCategoryBreakdownUseCase(),
        clock = clock,
    )

    @Test
    fun `no subscriptions state`() = runTest {
        viewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(InsightsUiState.NoSubscriptions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ready state ranks subscriptions groups by usage and lists insights`() = runTest {
        repository.seed(
            subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"), usage = DeclaredUsage.DAILY),
            subscription(id = "s", name = "Spotify", price = usd(1199), anchorDate = date("2026-09-24"), usage = DeclaredUsage.RARELY),
            subscription(id = "a", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly, category = Category.SOFTWARE, anchorDate = date("2026-10-01"), nextBillingDate = date("2026-10-01")),
        )
        viewModel().uiState.test {
            val state = awaitItem() as InsightsUiState.Ready
            assertThat(state.ranked.map { it.subscription.name }).containsExactly("Adobe", "Netflix", "Spotify").inOrder()
            assertThat(state.byUsage.keys).containsExactly(DeclaredUsage.RARELY, DeclaredUsage.DAILY, DeclaredUsage.UNKNOWN).inOrder()
            assertThat(state.unknownUsageCount).isEqualTo(1)
            assertThat(state.categories.first().category).isEqualTo(Category.SOFTWARE)
            assertThat(state.insights.first()).isInstanceOf(Insight.PotentialSavings::class.java)
            assertThat(state.summary.yearly).isEqualTo(usd((1549 + 1199) * 12 + 59988))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
