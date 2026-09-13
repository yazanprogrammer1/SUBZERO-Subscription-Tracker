package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CategoryShare(
    val category: Category,
    val monthly: Money,
    val count: Int,
    /** Share of the monthly total, 0..100, rounded; shares may not sum to exactly 100. */
    val sharePercent: Int,
)

/**
 * Monthly spend per category for active home-currency subscriptions, largest first.
 * Categories without subscriptions are omitted.
 */
class CalculateCategoryBreakdownUseCase @Inject constructor() {

    operator fun invoke(subscriptions: List<Subscription>, homeCurrency: CurrencyCode): List<CategoryShare> {
        val active = subscriptions.filter { it.isActive && it.price.currency == homeCurrency }
        if (active.isEmpty()) return emptyList()
        val monthlyOf = active.associateWith { BillingSchedule.monthlyEquivalent(it.price, it.billingCycle) }
        val total = monthlyOf.values.fold(Money.zero(homeCurrency)) { acc, m -> acc + m }
        return active
            .groupBy { it.category }
            .map { (category, items) ->
                val monthly = items.fold(Money.zero(homeCurrency)) { acc, s -> acc + monthlyOf.getValue(s) }
                CategoryShare(
                    category = category,
                    monthly = monthly,
                    count = items.size,
                    sharePercent = if (total.isZero) {
                        0
                    } else {
                        BigDecimal.valueOf(monthly.amountMinor)
                            .multiply(BigDecimal(100))
                            .divide(BigDecimal.valueOf(total.amountMinor), 0, RoundingMode.HALF_EVEN)
                            .toInt()
                    },
                )
            }
            .sortedWith(compareByDescending<CategoryShare> { it.monthly.amountMinor }.thenBy { it.category.name })
    }
}
