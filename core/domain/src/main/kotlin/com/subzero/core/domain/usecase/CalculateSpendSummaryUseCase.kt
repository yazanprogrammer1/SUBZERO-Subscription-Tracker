package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import javax.inject.Inject

/**
 * Normalized recurring spend in the home currency.
 *
 * Only ACTIVE subscriptions count: paused and canceled ones are not charging.
 * Subscriptions priced in another currency are never silently converted; they are excluded
 * from the totals and reported in [excludedForeignCurrency] so the UI can say so.
 */
data class SpendSummary(
    val monthly: Money,
    val yearly: Money,
    val includedCount: Int,
    val excludedForeignCurrency: Int,
) {
    companion object {
        fun empty(currency: CurrencyCode) = SpendSummary(
            monthly = Money.zero(currency),
            yearly = Money.zero(currency),
            includedCount = 0,
            excludedForeignCurrency = 0,
        )
    }
}

class CalculateSpendSummaryUseCase @Inject constructor() {

    operator fun invoke(subscriptions: List<Subscription>, homeCurrency: CurrencyCode): SpendSummary {
        var monthly = Money.zero(homeCurrency)
        var yearly = Money.zero(homeCurrency)
        var included = 0
        var excluded = 0
        subscriptions.asSequence()
            .filter { it.isActive }
            .forEach { subscription ->
                if (subscription.price.currency != homeCurrency) {
                    excluded++
                    return@forEach
                }
                monthly += BillingSchedule.monthlyEquivalent(subscription.price, subscription.billingCycle)
                yearly += BillingSchedule.yearlyEquivalent(subscription.price, subscription.billingCycle)
                included++
            }
        return SpendSummary(
            monthly = monthly,
            yearly = yearly,
            includedCount = included,
            excludedForeignCurrency = excluded,
        )
    }
}
