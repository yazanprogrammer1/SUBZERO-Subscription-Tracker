package com.subzero.core.domain.insight

import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.UpcomingPayment
import java.math.BigDecimal

/**
 * A deterministic, data-backed observation about the user's subscriptions.
 *
 * Insights carry typed data only; wording lives in the UI so it can be localized and phrased
 * carefully (never "you should cancel X"). [priority] orders them: higher shows first.
 */
sealed interface Insight {
    val id: String
    val priority: Int

    /** What the user pays per year across active home-currency subscriptions. */
    data class YearlyTotal(
        val yearly: Money,
        val monthly: Money,
        val activeCount: Int,
    ) : Insight {
        override val id = "yearly-total"
        override val priority = PRIORITY_LOW
    }

    /** The single most expensive subscription and its share of the yearly total. */
    data class LargestSubscription(
        val subscription: Subscription,
        val yearlyEquivalent: Money,
        val shareOfTotalPercent: Int,
    ) : Insight {
        override val id = "largest-${subscription.id.value}"
        override val priority = PRIORITY_MEDIUM
    }

    /** Several subscriptions in one category, some of which the user says they rarely use. */
    data class CategoryConcentration(
        val category: Category,
        val subscriptions: List<Subscription>,
        val monthlyTotal: Money,
        val rarelyUsedCount: Int,
    ) : Insight {
        override val id = "category-${category.name}"
        override val priority = if (rarelyUsedCount > 0) PRIORITY_HIGH else PRIORITY_MEDIUM
    }

    /** A subscription the user declared as rarely used, with what it costs per year. */
    data class LowUsage(
        val subscription: Subscription,
        val yearlyEquivalent: Money,
    ) : Insight {
        override val id = "low-usage-${subscription.id.value}"
        override val priority = PRIORITY_HIGH
    }

    /** What could be saved per month by dropping the rarely used subscriptions. */
    data class PotentialSavings(
        val monthly: Money,
        val yearly: Money,
        val subscriptions: List<Subscription>,
    ) : Insight {
        override val id = "potential-savings"
        override val priority = PRIORITY_TOP
    }

    /** A price change recorded for a subscription. */
    data class PriceIncrease(
        val subscription: Subscription,
        val from: Money,
        val to: Money,
        val changePercent: BigDecimal,
    ) : Insight {
        override val id = "price-${subscription.id.value}-${to.amountMinor}"

        /** Time-sensitive news, so it outranks the standing low-usage observations. */
        override val priority = PRIORITY_HIGH + PRICE_INCREASE_BOOST
    }

    /** The largest single charge coming up in the next window. */
    data class UpcomingLargeCharge(
        val payment: UpcomingPayment,
    ) : Insight {
        override val id = "upcoming-large-${payment.subscription.id.value}-${payment.date}"
        override val priority = PRIORITY_MEDIUM
    }

    companion object {
        const val PRIORITY_TOP = 40
        const val PRIORITY_HIGH = 30
        const val PRIORITY_MEDIUM = 20
        const val PRIORITY_LOW = 10
        const val PRICE_INCREASE_BOOST = 5
    }
}
