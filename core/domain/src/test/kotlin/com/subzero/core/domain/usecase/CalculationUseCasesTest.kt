package com.subzero.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.eur
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import org.junit.Test

class CalculationUseCasesTest {

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)

    private val netflix = subscription(id = "netflix", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"))
    private val chatgpt = subscription(id = "chatgpt", name = "ChatGPT", price = usd(2000), anchorDate = date("2026-09-20"), category = Category.AI)
    private val googleOne = subscription(id = "google", name = "Google One", price = usd(299), anchorDate = date("2026-09-22"), category = Category.CLOUD)
    private val spotify = subscription(id = "spotify", name = "Spotify", price = usd(1199), anchorDate = date("2026-09-24"))
    private val adobeYearly = subscription(id = "adobe", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly, anchorDate = date("2026-10-01"), category = Category.SOFTWARE)
    private val paused = subscription(id = "paused", name = "Paused Thing", price = usd(9999), status = SubscriptionStatus.PAUSED)
    private val canceled = subscription(id = "canceled", name = "Old Thing", price = usd(9999), status = SubscriptionStatus.CANCELED)
    private val euroSub = subscription(id = "euro", name = "Deezer", price = eur(1099), anchorDate = date("2026-09-18"))

    // --- spend summary ---

    @Test
    fun `spend summary sums monthly and yearly equivalents of active subscriptions`() {
        val summary = CalculateSpendSummaryUseCase()(listOf(netflix, chatgpt, googleOne, spotify), CurrencyCode.USD)
        assertThat(summary.monthly).isEqualTo(usd(1549 + 2000 + 299 + 1199))
        assertThat(summary.yearly).isEqualTo(usd((1549 + 2000 + 299 + 1199) * 12))
        assertThat(summary.includedCount).isEqualTo(4)
        assertThat(summary.excludedForeignCurrency).isEqualTo(0)
    }

    @Test
    fun `spend summary normalizes yearly subscriptions to monthly`() {
        val summary = CalculateSpendSummaryUseCase()(listOf(adobeYearly), CurrencyCode.USD)
        assertThat(summary.monthly).isEqualTo(usd(4999))
        assertThat(summary.yearly).isEqualTo(usd(59988))
    }

    @Test
    fun `spend summary ignores paused and canceled subscriptions`() {
        val summary = CalculateSpendSummaryUseCase()(listOf(netflix, paused, canceled), CurrencyCode.USD)
        assertThat(summary.monthly).isEqualTo(usd(1549))
        assertThat(summary.includedCount).isEqualTo(1)
    }

    @Test
    fun `spend summary excludes and counts foreign currency subscriptions`() {
        val summary = CalculateSpendSummaryUseCase()(listOf(netflix, euroSub), CurrencyCode.USD)
        assertThat(summary.monthly).isEqualTo(usd(1549))
        assertThat(summary.includedCount).isEqualTo(1)
        assertThat(summary.excludedForeignCurrency).isEqualTo(1)
    }

    @Test
    fun `spend summary of an empty list is zero`() {
        val summary = CalculateSpendSummaryUseCase()(emptyList(), CurrencyCode.USD)
        assertThat(summary).isEqualTo(SpendSummary.empty(CurrencyCode.USD))
    }

    // --- upcoming payments ---

    @Test
    fun `upcoming payments are ordered by date within the window`() {
        val upcoming = GetUpcomingPaymentsUseCase(clock)(listOf(spotify, netflix, chatgpt, googleOne, adobeYearly))
        assertThat(upcoming.map { it.subscription.name }).containsExactly(
            "Netflix", "ChatGPT", "Google One", "Spotify", "Adobe",
        ).inOrder()
        assertThat(upcoming.first().date).isEqualTo(date("2026-09-16"))
    }

    @Test
    fun `upcoming payments lists a weekly subscription once per week`() {
        val weekly = subscription(id = "w", name = "Weekly", billingCycle = BillingCycle.Weekly, anchorDate = date("2026-09-13"))
        val upcoming = GetUpcomingPaymentsUseCase(clock)(listOf(weekly), from = today, to = today.plusDays(21))
        // Window ends Oct 3, so Oct 4 is excluded.
        assertThat(upcoming.map { it.date }).containsExactly(
            date("2026-09-13"), date("2026-09-20"), date("2026-09-27"),
        ).inOrder()
    }

    @Test
    fun `upcoming payments skip inactive subscriptions`() {
        val upcoming = GetUpcomingPaymentsUseCase(clock)(listOf(paused, canceled))
        assertThat(upcoming).isEmpty()
    }

    @Test
    fun `next payment picks the soonest active charge`() {
        val next = GetUpcomingPaymentsUseCase(clock).next(listOf(spotify, chatgpt, netflix, paused))
        assertThat(next?.subscription?.name).isEqualTo("Netflix")
        assertThat(next?.date).isEqualTo(date("2026-09-16"))
        assertThat(GetUpcomingPaymentsUseCase(clock).next(emptyList())).isNull()
    }

    @Test
    fun `next payment on the charge day is today`() {
        val chargesToday = subscription(id = "t", anchorDate = date("2026-08-12"))
        val next = GetUpcomingPaymentsUseCase(clock).next(listOf(chargesToday))
        assertThat(next?.date).isEqualTo(today)
    }

    // --- search ---

    @Test
    fun `search matches name case insensitively`() {
        val results = SearchSubscriptionsUseCase()(listOf(netflix, chatgpt, spotify), "NET")
        assertThat(results).containsExactly(netflix)
    }

    @Test
    fun `search matches category`() {
        val claude = subscription(id = "claude", name = "Claude", category = Category.AI)
        val results = SearchSubscriptionsUseCase()(listOf(netflix, chatgpt, claude), "ai")
        assertThat(results).containsExactly(chatgpt, claude)
    }

    @Test
    fun `search matches amounts by exact value or integer part`() {
        val all = listOf(netflix, chatgpt, googleOne, spotify)
        assertThat(SearchSubscriptionsUseCase()(all, "15.49")).containsExactly(netflix)
        assertThat(SearchSubscriptionsUseCase()(all, "15,49")).containsExactly(netflix)
        assertThat(SearchSubscriptionsUseCase()(all, "15")).containsExactly(netflix)
        assertThat(SearchSubscriptionsUseCase()(all, "20")).containsExactly(chatgpt)
        assertThat(SearchSubscriptionsUseCase()(all, "2")).containsExactly(googleOne)
        assertThat(SearchSubscriptionsUseCase()(all, "3")).isEmpty()
    }

    @Test
    fun `blank search returns everything`() {
        val all = listOf(netflix, chatgpt)
        assertThat(SearchSubscriptionsUseCase()(all, "   ")).isEqualTo(all)
    }

    // --- filter ---

    @Test
    fun `filters by cycle category status and upcoming window`() {
        val all = listOf(netflix, chatgpt, googleOne, spotify, adobeYearly, paused, canceled)
        val filter = FilterSubscriptionsUseCase(clock)
        assertThat(filter(all, SubscriptionFilter.Cycle(BillingCycle.Yearly))).containsExactly(adobeYearly)
        assertThat(filter(all, SubscriptionFilter.InCategory(Category.AI))).containsExactly(chatgpt)
        assertThat(filter(all, SubscriptionFilter.Status(SubscriptionStatus.PAUSED))).containsExactly(paused)
        assertThat(filter(all, SubscriptionFilter.Upcoming(days = 7))).containsExactly(netflix)
        assertThat(filter(all, SubscriptionFilter.Upcoming(days = 10))).containsExactly(netflix, chatgpt, googleOne)
        assertThat(filter(all, SubscriptionFilter.All)).isEqualTo(all)
    }

    @Test
    fun `high cost filter keeps subscriptions above the active average per currency`() {
        // Monthly equivalents: 15.49, 20.00, 2.99, 11.99, 49.99 -> average 20.09
        val all = listOf(netflix, chatgpt, googleOne, spotify, adobeYearly, paused, euroSub)
        val result = FilterSubscriptionsUseCase(clock)(all, SubscriptionFilter.HighCost)
        assertThat(result).containsExactly(adobeYearly)
        assertThat(FilterSubscriptionsUseCase(clock)(emptyList(), SubscriptionFilter.HighCost)).isEmpty()
    }

    // --- sort ---

    @Test
    fun `sorts by cost next payment and name`() {
        val all = listOf(spotify, adobeYearly, netflix, chatgpt, googleOne)
        val sort = SortSubscriptionsUseCase()
        assertThat(sort(all, SubscriptionSort.HIGHEST_COST).map { it.name })
            .containsExactly("Adobe", "ChatGPT", "Netflix", "Spotify", "Google One").inOrder()
        assertThat(sort(all, SubscriptionSort.LOWEST_COST).map { it.name })
            .containsExactly("Google One", "Spotify", "Netflix", "ChatGPT", "Adobe").inOrder()
        assertThat(sort(all, SubscriptionSort.NEXT_PAYMENT).map { it.name })
            .containsExactly("Netflix", "ChatGPT", "Google One", "Spotify", "Adobe").inOrder()
        assertThat(sort(all, SubscriptionSort.ALPHABETICAL).map { it.name })
            .containsExactly("Adobe", "ChatGPT", "Google One", "Netflix", "Spotify").inOrder()
    }

    @Test
    fun `next payment sort places inactive subscriptions last`() {
        val sorted = SortSubscriptionsUseCase()(listOf(canceled, netflix, paused), SubscriptionSort.NEXT_PAYMENT)
        assertThat(sorted.first()).isEqualTo(netflix)
    }
}
