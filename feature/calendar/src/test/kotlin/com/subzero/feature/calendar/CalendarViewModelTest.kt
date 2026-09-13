package com.subzero.feature.calendar

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.eur
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()
    private val preferences = FakeUserPreferencesRepository()

    private fun viewModel() = CalendarViewModel(repository, preferences, GetUpcomingPaymentsUseCase(clock), clock)

    @Test
    fun `starts on the current month with today selected`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-09-16")))
        viewModel().uiState.test {
            val state = awaitItem()
            assertThat(state.month).isEqualTo(YearMonth.of(2026, 9))
            assertThat(state.selectedDay).isEqualTo(today)
            assertThat(state.hasSubscriptions).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `month total sums home currency charges and counts foreign ones`() = runTest {
        repository.seed(
            subscription(id = "n", price = usd(1549), anchorDate = date("2026-09-16")),
            subscription(id = "w", price = usd(500), billingCycle = BillingCycle.Weekly, anchorDate = date("2026-09-01")),
            subscription(id = "e", price = eur(999), anchorDate = date("2026-09-20")),
            subscription(id = "p", price = usd(9999), anchorDate = date("2026-09-21"), status = SubscriptionStatus.PAUSED),
        )
        viewModel().uiState.test {
            val state = awaitItem()
            // Weekly on Sep 1, 8, 15, 22, 29 = 5 charges.
            assertThat(state.monthTotal).isEqualTo(usd(1549 + 500 * 5))
            assertThat(state.foreignCurrencyCount).isEqualTo(1)
            assertThat(state.paymentsByDay.keys).containsAtLeast(date("2026-09-16"), date("2026-09-20"), date("2026-09-29"))
            assertThat(state.paymentsByDay).doesNotContainKey(date("2026-09-21"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `month navigation clears selection except when returning to the current month`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-09-16")))
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.nextMonth()
            val october = expectMostRecentItem()
            assertThat(october.month).isEqualTo(YearMonth.of(2026, 10))
            assertThat(october.selectedDay).isNull()
            assertThat(october.paymentsByDay.keys).containsExactly(date("2026-10-16"))

            vm.previousMonth()
            val september = expectMostRecentItem()
            assertThat(september.selectedDay).isEqualTo(today)

            vm.previousMonth()
            vm.goToToday()
            assertThat(expectMostRecentItem().month).isEqualTo(YearMonth.of(2026, 9))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a day toggles it`() = runTest {
        repository.seed(subscription(id = "n", anchorDate = date("2026-09-16")))
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.selectDay(date("2026-09-16"))
            val selected = expectMostRecentItem()
            assertThat(selected.selectedDay).isEqualTo(date("2026-09-16"))
            assertThat(selected.selectedPayments).hasSize(1)

            vm.selectDay(date("2026-09-16"))
            assertThat(expectMostRecentItem().selectedDay).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `grid starts on the locale first day of week and covers six weeks`() {
        val us = monthCells(YearMonth.of(2026, 9), WeekFields.of(Locale.US))
        assertThat(us).hasSize(42)
        assertThat(us.first()).isEqualTo(date("2026-08-30")) // Sunday
        assertThat(us.last()).isEqualTo(date("2026-10-10"))

        val de = monthCells(YearMonth.of(2026, 9), WeekFields.of(Locale.GERMANY))
        assertThat(de.first()).isEqualTo(date("2026-08-31")) // Monday

        val feb = monthCells(YearMonth.of(2027, 2), WeekFields.of(Locale.GERMANY))
        assertThat(feb.first()).isEqualTo(date("2027-02-01"))
    }
}
