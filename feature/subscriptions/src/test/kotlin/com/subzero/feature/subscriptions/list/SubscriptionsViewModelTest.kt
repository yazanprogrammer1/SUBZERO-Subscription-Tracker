package com.subzero.feature.subscriptions.list

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.FilterSubscriptionsUseCase
import com.subzero.core.domain.usecase.SearchSubscriptionsUseCase
import com.subzero.core.domain.usecase.SortSubscriptionsUseCase
import com.subzero.core.domain.usecase.SubscriptionFilter
import com.subzero.core.domain.usecase.SubscriptionSort
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SubscriptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = fixedClock(date("2026-09-12"))
    private val repository = FakeSubscriptionRepository()

    private fun viewModel() = SubscriptionsViewModel(
        repository = repository,
        search = SearchSubscriptionsUseCase(),
        filterUseCase = FilterSubscriptionsUseCase(clock),
        sortUseCase = SortSubscriptionsUseCase(),
        clock = clock,
    )

    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"))
    private val adobe = subscription(
        id = "a",
        name = "Adobe",
        price = usd(59988),
        billingCycle = BillingCycle.Yearly,
        anchorDate = date("2026-10-01"),
        nextBillingDate = date("2026-10-01"),
        category = Category.SOFTWARE,
    )

    @Test
    fun `empty repository yields the empty state`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isEmpty).isTrue()
            assertThat(state.hasNoMatches).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items follow next payment order by default`() = runTest {
        repository.seed(adobe, netflix)
        viewModel().uiState.test {
            assertThat(awaitItem().items.map { it.name }).containsExactly("Netflix", "Adobe").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search narrows and reports no matches instead of empty`() = runTest {
        repository.seed(adobe, netflix)
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.setSearchActive(true)
            vm.setQuery("zzz")
            val state = expectMostRecentItem()
            assertThat(state.items).isEmpty()
            assertThat(state.hasNoMatches).isTrue()
            assertThat(state.isEmpty).isFalse()

            vm.setSearchActive(false)
            assertThat(expectMostRecentItem().items).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter and sort apply together`() = runTest {
        repository.seed(adobe, netflix)
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.setFilter(SubscriptionFilter.Cycle(BillingCycle.Yearly))
            assertThat(expectMostRecentItem().items.map { it.name }).containsExactly("Adobe")

            vm.setFilter(SubscriptionFilter.All)
            vm.setSort(SubscriptionSort.HIGHEST_COST)
            assertThat(expectMostRecentItem().items.map { it.name }).containsExactly("Adobe", "Netflix").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `list reacts to repository changes`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertThat(awaitItem().isEmpty).isTrue()
            repository.seed(netflix)
            assertThat(awaitItem().items).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
