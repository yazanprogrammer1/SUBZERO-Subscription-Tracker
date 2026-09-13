package com.subzero.core.domain.insight

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.UpcomingPayment
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Everything a rule may look at. Built once per evaluation by [GenerateInsightsUseCase]. */
data class InsightContext(
    val subscriptions: List<Subscription>,
    val priceChanges: List<PriceChange>,
    val upcoming: List<UpcomingPayment>,
    val homeCurrency: CurrencyCode,
    val today: LocalDate,
) {
    /** Active subscriptions priced in the home currency: the only ones whose amounts can be summed. */
    val active: List<Subscription> = subscriptions.filter { it.isActive && it.price.currency == homeCurrency }

    fun monthly(subscription: Subscription): Money =
        BillingSchedule.monthlyEquivalent(subscription.price, subscription.billingCycle)

    fun yearly(subscription: Subscription): Money =
        BillingSchedule.yearlyEquivalent(subscription.price, subscription.billingCycle)

    fun sumMonthly(items: List<Subscription>): Money = items.fold(Money.zero(homeCurrency)) { acc, s -> acc + monthly(s) }

    fun sumYearly(items: List<Subscription>): Money = items.fold(Money.zero(homeCurrency)) { acc, s -> acc + yearly(s) }
}

/** One deterministic rule. Rules are pure and independent; the use case runs them all. */
fun interface InsightRule {
    fun evaluate(context: InsightContext): List<Insight>
}

object YearlyTotalRule : InsightRule {
    override fun evaluate(context: InsightContext): List<Insight> {
        if (context.active.isEmpty()) return emptyList()
        return listOf(
            Insight.YearlyTotal(
                yearly = context.sumYearly(context.active),
                monthly = context.sumMonthly(context.active),
                activeCount = context.active.size,
            ),
        )
    }
}

object LargestSubscriptionRule : InsightRule {
    private const val MIN_SUBSCRIPTIONS = 2

    override fun evaluate(context: InsightContext): List<Insight> {
        if (context.active.size < MIN_SUBSCRIPTIONS) return emptyList()
        val total = context.sumYearly(context.active)
        if (total.isZero) return emptyList()
        val largest = context.active.maxByOrNull { context.yearly(it).amountMinor } ?: return emptyList()
        val yearly = context.yearly(largest)
        val share = BigDecimal.valueOf(yearly.amountMinor)
            .multiply(BigDecimal(100))
            .divide(BigDecimal.valueOf(total.amountMinor), 0, RoundingMode.HALF_EVEN)
            .toInt()
        return listOf(Insight.LargestSubscription(largest, yearly, share))
    }
}

object CategoryConcentrationRule : InsightRule {
    private const val MIN_IN_CATEGORY = 3

    override fun evaluate(context: InsightContext): List<Insight> =
        context.active
            .groupBy { it.category }
            .filter { (_, items) -> items.size >= MIN_IN_CATEGORY }
            .map { (category, items) ->
                Insight.CategoryConcentration(
                    category = category,
                    subscriptions = items,
                    monthlyTotal = context.sumMonthly(items),
                    rarelyUsedCount = items.count { it.usage == DeclaredUsage.RARELY },
                )
            }
}

object LowUsageRule : InsightRule {
    override fun evaluate(context: InsightContext): List<Insight> =
        context.active
            .filter { it.usage == DeclaredUsage.RARELY }
            .map { Insight.LowUsage(it, context.yearly(it)) }
}

object PotentialSavingsRule : InsightRule {
    override fun evaluate(context: InsightContext): List<Insight> {
        val rarely = context.active.filter { it.usage == DeclaredUsage.RARELY }
        if (rarely.isEmpty()) return emptyList()
        return listOf(
            Insight.PotentialSavings(
                monthly = context.sumMonthly(rarely),
                yearly = context.sumYearly(rarely),
                subscriptions = rarely,
            ),
        )
    }
}

object PriceIncreaseRule : InsightRule {
    /** Only increases within this many days are still news. */
    private const val RECENT_DAYS = 90L

    override fun evaluate(context: InsightContext): List<Insight> {
        val byId = context.subscriptions.associateBy { it.id }
        return context.priceChanges
            .groupBy { it.subscriptionId }
            .mapNotNull { (id, changes) ->
                val subscription = byId[id] ?: return@mapNotNull null
                if (!subscription.isActive) return@mapNotNull null
                val sorted = changes.sortedBy { it.effectiveFrom }
                if (sorted.size < 2) return@mapNotNull null
                val latest = sorted.last()
                val previous = sorted[sorted.size - 2]
                if (latest.price.currency != previous.price.currency) return@mapNotNull null
                if (latest.price.amountMinor <= previous.price.amountMinor) return@mapNotNull null
                if (latest.effectiveFrom.isBefore(context.today.minusDays(RECENT_DAYS))) return@mapNotNull null
                if (previous.price.isZero) return@mapNotNull null
                val percent = BigDecimal.valueOf(latest.price.amountMinor - previous.price.amountMinor)
                    .multiply(BigDecimal(100))
                    .divide(BigDecimal.valueOf(previous.price.amountMinor), 1, RoundingMode.HALF_EVEN)
                Insight.PriceIncrease(subscription, previous.price, latest.price, percent)
            }
    }
}

object UpcomingLargeChargeRule : InsightRule {
    /** A charge is "large" when it is at least this multiple of the average monthly equivalent. */
    private val LARGE_FACTOR = BigDecimal("2.0")

    override fun evaluate(context: InsightContext): List<Insight> {
        if (context.active.isEmpty()) return emptyList()
        val averageMonthly = context.sumMonthly(context.active).amountMinor / context.active.size
        if (averageMonthly == 0L) return emptyList()
        val threshold = BigDecimal.valueOf(averageMonthly).multiply(LARGE_FACTOR).toLong()
        val largest = context.upcoming
            .filter { it.amount.currency == context.homeCurrency && it.amount.amountMinor >= threshold }
            .maxByOrNull { it.amount.amountMinor } ?: return emptyList()
        return listOf(Insight.UpcomingLargeCharge(largest))
    }
}

/** The default rule set, in no particular order; priority sorts the output. */
val defaultInsightRules: List<InsightRule> = listOf(
    PotentialSavingsRule,
    PriceIncreaseRule,
    LowUsageRule,
    CategoryConcentrationRule,
    UpcomingLargeChargeRule,
    LargestSubscriptionRule,
    YearlyTotalRule,
)
