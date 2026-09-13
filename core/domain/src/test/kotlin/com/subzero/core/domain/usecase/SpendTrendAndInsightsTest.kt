package com.subzero.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.eur
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class SpendTrendAndInsightsTest {

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val trend = CalculateSpendTrendUseCase(clock, CalculateSpendingHistoryUseCase(clock))
    private val insights = GenerateInsightsUseCase(GetUpcomingPaymentsUseCase(clock), clock)

    private val installed = Instant.parse("2026-08-01T00:00:00Z")

    // Netflix: monthly on the 16th since January, price rose in April, known to the app since Aug 1.
    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-01-16"), nextBillingDate = date("2026-09-16"), createdAt = installed)
    private val netflixPrices = listOf(
        priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2026-01-16")),
        priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-08-20")),
    )
    private val netflixRecords = listOf(paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-08-16")))

    // Spotify: monthly on the 5th, already charged this month (recorded), rarely used.
    private val spotify = subscription(id = "s", name = "Spotify", price = usd(1199), anchorDate = date("2026-06-05"), nextBillingDate = date("2026-10-05"), usage = DeclaredUsage.RARELY, createdAt = installed)
    private val spotifyRecords = listOf(
        paymentRecord(subscriptionId = "s", amount = usd(1199), paidOn = date("2026-08-05")),
        paymentRecord(subscriptionId = "s", amount = usd(1199), paidOn = date("2026-09-05")),
    )

    @Test
    fun `trend sums recorded estimated and projected amounts per month`() {
        val result = trend(
            subscriptions = listOf(netflix, spotify),
            priceChanges = netflixPrices + priceChange(subscriptionId = "s", price = usd(1199), effectiveFrom = date("2026-06-05")),
            records = netflixRecords + spotifyRecords,
            homeCurrency = CurrencyCode.USD,
            months = 3,
        )

        assertThat(result.months.map { it.month }).containsExactly(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9)).inOrder()
        // July: both estimated (before Aug 1): Netflix 13.99 + Spotify 11.99.
        val july = result.months[0]
        assertThat(july.total).isEqualTo(usd(1399 + 1199))
        assertThat(july.estimated).isEqualTo(usd(1399 + 1199))
        // August: recorded 15.49 + 11.99.
        assertThat(result.months[1].total).isEqualTo(usd(1549 + 1199))
        assertThat(result.months[1].estimated).isEqualTo(usd(0))
        // September: Spotify recorded 11.99 + Netflix projected on the 16th 15.49.
        val september = result.current
        assertThat(september.total).isEqualTo(usd(1199 + 1549))
        assertThat(september.projected).isEqualTo(usd(1549))
        assertThat(result.changeVersusPreviousPercent).isEqualTo(BigDecimal("0.0"))
    }

    @Test
    fun `trend change percent compares current with previous month`() {
        val yearly = subscription(id = "y", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly, anchorDate = date("2026-09-20"), nextBillingDate = date("2026-09-20"), createdAt = installed)
        val result = trend(listOf(netflix, yearly), netflixPrices, netflixRecords, CurrencyCode.USD, months = 2)
        // August 15.49 -> September 15.49 + 599.88
        assertThat(result.previous?.total).isEqualTo(usd(1549))
        assertThat(result.current.total).isEqualTo(usd(1549 + 59988))
        assertThat(result.changeVersusPreviousPercent).isEqualTo(BigDecimal("3872.7"))
    }

    @Test
    fun `trend ignores foreign currency and inactive projections`() {
        val paused = subscription(id = "p", price = usd(9999), status = SubscriptionStatus.PAUSED, anchorDate = date("2026-09-20"), createdAt = installed)
        val foreign = subscription(id = "f", price = eur(500), anchorDate = date("2026-09-20"), createdAt = installed)
        val result = trend(listOf(paused, foreign), emptyList(), emptyList(), CurrencyCode.USD, months = 2)
        assertThat(result.current.total).isEqualTo(usd(0))
        assertThat(result.changeVersusPreviousPercent).isNull()
    }

    @Test
    fun `potential savings sums rarely used active home currency subscriptions`() {
        val rarelyForeign = subscription(id = "rf", price = eur(1000), usage = DeclaredUsage.RARELY)
        val rarelyPaused = subscription(id = "rp", price = usd(1000), usage = DeclaredUsage.RARELY, status = SubscriptionStatus.PAUSED)
        val savings = CalculatePotentialSavingsUseCase()(listOf(netflix, spotify, rarelyForeign, rarelyPaused), CurrencyCode.USD)
        assertThat(savings.monthly).isEqualTo(usd(1199))
        assertThat(savings.subscriptions).containsExactly(spotify)
    }

    @Test
    fun `insights are generated ordered by priority`() {
        val chatgpt = subscription(id = "c", name = "ChatGPT", price = usd(2000), category = Category.AI, anchorDate = date("2026-09-20"), nextBillingDate = date("2026-09-20"))
        val claude = subscription(id = "cl", name = "Claude", price = usd(2000), category = Category.AI, anchorDate = date("2026-09-21"), nextBillingDate = date("2026-09-21"))
        val gemini = subscription(id = "g", name = "Gemini", price = usd(1999), category = Category.AI, anchorDate = date("2026-09-22"), nextBillingDate = date("2026-09-22"), usage = DeclaredUsage.RARELY)
        val adobe = subscription(id = "a", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly, category = Category.SOFTWARE, anchorDate = date("2026-09-25"), nextBillingDate = date("2026-09-25"))

        val result = insights(listOf(netflix, spotify, chatgpt, claude, gemini, adobe), netflixPrices, CurrencyCode.USD)

        assertThat(result.first()).isInstanceOf(Insight.PotentialSavings::class.java)
        val savings = result.first() as Insight.PotentialSavings
        assertThat(savings.subscriptions.map { it.name }).containsExactly("Spotify", "Gemini")
        assertThat(savings.monthly).isEqualTo(usd(1199 + 1999))

        val kinds = result.map { it::class.simpleName }
        assertThat(kinds).containsAtLeast("PriceIncrease", "LowUsage", "CategoryConcentration", "UpcomingLargeCharge", "LargestSubscription", "YearlyTotal")
        assertThat(result.map { it.priority }).isInOrder(reverseOrder<Int>())

        val price = result.filterIsInstance<Insight.PriceIncrease>().single()
        assertThat(price.subscription.name).isEqualTo("Netflix")
        assertThat(price.changePercent).isEqualTo(BigDecimal("10.7"))

        val category = result.filterIsInstance<Insight.CategoryConcentration>().single()
        assertThat(category.category).isEqualTo(Category.AI)
        assertThat(category.subscriptions).hasSize(3)
        assertThat(category.rarelyUsedCount).isEqualTo(1)

        val largest = result.filterIsInstance<Insight.LargestSubscription>().single()
        assertThat(largest.subscription.name).isEqualTo("Adobe")

        val large = result.filterIsInstance<Insight.UpcomingLargeCharge>().single()
        assertThat(large.payment.subscription.name).isEqualTo("Adobe")
    }

    @Test
    fun `no insights without active home currency subscriptions`() {
        val foreign = subscription(id = "f", price = eur(500))
        assertThat(insights(listOf(foreign), emptyList(), CurrencyCode.USD)).isEmpty()
        assertThat(insights(emptyList(), emptyList(), CurrencyCode.USD)).isEmpty()
    }

    @Test
    fun `old or decreasing price changes are not reported`() {
        val old = listOf(
            priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2025-01-16")),
            priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2025-06-01")),
        )
        assertThat(insights(listOf(netflix), old, CurrencyCode.USD).filterIsInstance<Insight.PriceIncrease>()).isEmpty()

        val decrease = listOf(
            priceChange(subscriptionId = "n", price = usd(1799), effectiveFrom = date("2026-01-16")),
            priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-09-01")),
        )
        assertThat(insights(listOf(netflix), decrease, CurrencyCode.USD).filterIsInstance<Insight.PriceIncrease>()).isEmpty()
    }
}
