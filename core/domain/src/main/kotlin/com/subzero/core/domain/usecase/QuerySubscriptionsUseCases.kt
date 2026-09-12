package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Matches subscriptions by name, category or amount.
 * "net" → Netflix; "ai" → every AI-category subscription (category names match from the start,
 * so "ai" does not match Entertainment); "15.49" or "15" → matching prices.
 */
class SearchSubscriptionsUseCase @Inject constructor() {

    operator fun invoke(subscriptions: List<Subscription>, query: String): List<Subscription> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return subscriptions
        val amountQuery = q.replace(',', '.').toBigDecimalOrNull()
        return subscriptions.filter { subscription ->
            subscription.name.lowercase().contains(q) ||
                subscription.category.name.lowercase().startsWith(q) ||
                (amountQuery != null && subscription.price.matchesAmount(amountQuery))
        }
    }

    /** "15" matches 15.49 (integer part) and 15.00; "15.49" matches only 15.49. */
    private fun Money.matchesAmount(query: BigDecimal): Boolean {
        val major = toMajorUnits()
        return major.compareTo(query) == 0 ||
            (query.scale() <= 0 && major.setScale(0, java.math.RoundingMode.DOWN).compareTo(query) == 0)
    }
}

sealed interface SubscriptionFilter {
    data object All : SubscriptionFilter
    data class Cycle(val cycle: BillingCycle) : SubscriptionFilter
    data class InCategory(val category: Category) : SubscriptionFilter
    data class Status(val status: SubscriptionStatus) : SubscriptionFilter

    /** Charges within the next [days] days (inclusive of today). */
    data class Upcoming(val days: Long = 7) : SubscriptionFilter

    /** Monthly equivalent above the average of the active set, i.e. the expensive half. */
    data object HighCost : SubscriptionFilter
}

class FilterSubscriptionsUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(subscriptions: List<Subscription>, filter: SubscriptionFilter): List<Subscription> =
        when (filter) {
            SubscriptionFilter.All -> subscriptions
            is SubscriptionFilter.Cycle -> subscriptions.filter { it.billingCycle == filter.cycle }
            is SubscriptionFilter.InCategory -> subscriptions.filter { it.category == filter.category }
            is SubscriptionFilter.Status -> subscriptions.filter { it.status == filter.status }
            is SubscriptionFilter.Upcoming -> {
                val today = LocalDate.now(clock)
                val limit = today.plusDays(filter.days)
                subscriptions.filter { it.isActive && !it.nextBillingDate.isBefore(today) && !it.nextBillingDate.isAfter(limit) }
            }
            SubscriptionFilter.HighCost -> highCost(subscriptions)
        }

    private fun highCost(subscriptions: List<Subscription>): List<Subscription> {
        val active = subscriptions.filter { it.isActive }
        if (active.isEmpty()) return emptyList()
        // Compare within each currency: an average across currencies would be meaningless.
        return active.groupBy { it.price.currency }.values.flatMap { group ->
            val monthly = group.associateWith { monthlyMinor(it) }
            val average = monthly.values.average()
            group.filter { monthly.getValue(it) > average }
        }
    }
}

enum class SubscriptionSort {
    HIGHEST_COST,
    LOWEST_COST,
    NEXT_PAYMENT,
    ALPHABETICAL,
}

class SortSubscriptionsUseCase @Inject constructor() {

    /**
     * Cost sorts compare monthly equivalents by minor units. Across different currencies that is
     * nominal, not converted; the UI groups by currency where it matters.
     */
    operator fun invoke(subscriptions: List<Subscription>, sort: SubscriptionSort): List<Subscription> =
        when (sort) {
            SubscriptionSort.HIGHEST_COST ->
                subscriptions.sortedWith(compareByDescending<Subscription> { monthlyMinor(it) }.thenBy { it.name.lowercase() })
            SubscriptionSort.LOWEST_COST ->
                subscriptions.sortedWith(compareBy<Subscription> { monthlyMinor(it) }.thenBy { it.name.lowercase() })
            SubscriptionSort.NEXT_PAYMENT ->
                subscriptions.sortedWith(
                    compareBy<Subscription> { !it.isActive }
                        .thenBy { it.nextBillingDate }
                        .thenBy { it.name.lowercase() },
                )
            SubscriptionSort.ALPHABETICAL ->
                subscriptions.sortedWith(compareBy<Subscription> { it.name.lowercase() }.thenBy { it.nextBillingDate })
        }
}

private fun monthlyMinor(subscription: Subscription): Long =
    BillingSchedule.monthlyEquivalent(subscription.price, subscription.billingCycle).amountMinor
