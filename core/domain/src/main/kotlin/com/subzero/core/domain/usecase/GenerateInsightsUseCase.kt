package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.insight.InsightContext
import com.subzero.core.domain.insight.InsightRule
import com.subzero.core.domain.insight.defaultInsightRules
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Runs every [InsightRule] over the current data and returns the insights ordered by
 * priority (then by a stable id so the order does not jitter between refreshes).
 */
class GenerateInsightsUseCase @Inject constructor(
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val clock: Clock,
) {
    operator fun invoke(
        subscriptions: List<Subscription>,
        priceChanges: List<PriceChange>,
        homeCurrency: CurrencyCode,
        rules: List<InsightRule> = defaultInsightRules,
    ): List<Insight> {
        val today = LocalDate.now(clock)
        val context = InsightContext(
            subscriptions = subscriptions,
            priceChanges = priceChanges,
            upcoming = getUpcomingPayments(subscriptions, from = today),
            homeCurrency = homeCurrency,
            today = today,
        )
        return rules
            .flatMap { it.evaluate(context) }
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Insight> { it.priority }.thenBy { it.id })
    }
}

/** Monthly amount the user could stop paying by dropping rarely used subscriptions. */
data class PotentialSavings(
    val monthly: Money,
    val subscriptions: List<Subscription>,
) {
    val isEmpty: Boolean get() = subscriptions.isEmpty()
}

class CalculatePotentialSavingsUseCase @Inject constructor() {
    operator fun invoke(subscriptions: List<Subscription>, homeCurrency: CurrencyCode): PotentialSavings {
        val rarely = subscriptions.filter {
            it.isActive && it.price.currency == homeCurrency && it.usage == DeclaredUsage.RARELY
        }
        val monthly = rarely.fold(Money.zero(homeCurrency)) { acc, s ->
            acc + BillingSchedule.monthlyEquivalent(s.price, s.billingCycle)
        }
        return PotentialSavings(monthly = monthly, subscriptions = rarely)
    }
}
