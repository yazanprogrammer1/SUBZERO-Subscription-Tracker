package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Spend in one calendar month; [estimated] is the part not backed by a recorded payment. */
data class MonthlySpend(
    val month: YearMonth,
    val total: Money,
    val estimated: Money,
) {
    val hasEstimate: Boolean get() = !estimated.isZero
}

data class SpendingHistory(
    val months: List<MonthlySpend>,
    val total: Money,
) {
    val hasEstimates: Boolean get() = months.any { it.hasEstimate }
}

/**
 * Month-by-month spend for one subscription over a trailing window (the hybrid model, M7).
 *
 * Charges from the day the app learned about the subscription onward come from
 * [PaymentRecord]s written by the rollover. Charges before that day are estimated from the
 * billing schedule and the price that was in effect on each date, and are flagged as such
 * so the UI can label them. Nothing is estimated for dates in the future.
 */
class CalculateSpendingHistoryUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(
        subscription: Subscription,
        priceHistory: List<PriceChange>,
        records: List<PaymentRecord>,
        months: Int = DEFAULT_MONTHS,
    ): SpendingHistory {
        require(months >= 1)
        val currency = subscription.price.currency
        val today = LocalDate.now(clock)
        val lastMonth = YearMonth.from(today)
        val firstMonth = lastMonth.minusMonths((months - 1).toLong())
        val windowStart = firstMonth.atDay(1)
        val windowEnd = minOf(lastMonth.atEndOfMonth(), today)

        val recordedByMonth = records
            .filter { it.amount.currency == currency && it.paidOn in windowStart..windowEnd }
            .groupBy({ YearMonth.from(it.paidOn) }, { it.amount })

        val knownSince = subscription.createdAt.atZone(clock.zone).toLocalDate()
        val estimateEnd = minOf(windowEnd, knownSince.minusDays(1))
        val estimatedByMonth = if (estimateEnd.isBefore(windowStart)) {
            emptyMap()
        } else {
            val sortedPrices = priceHistory.sortedBy { it.effectiveFrom }
            BillingSchedule.chargesBetween(subscription.anchorDate, subscription.billingCycle, windowStart, estimateEnd)
                .groupBy({ YearMonth.from(it) }, { priceOn(it, sortedPrices, subscription.price) })
        }

        val monthly = (0 until months).map { offset ->
            val month = firstMonth.plusMonths(offset.toLong())
            val recorded = recordedByMonth[month].orEmpty().fold(Money.zero(currency)) { acc, m -> acc + m }
            val estimated = estimatedByMonth[month].orEmpty().fold(Money.zero(currency)) { acc, m -> acc + m }
            MonthlySpend(month = month, total = recorded + estimated, estimated = estimated)
        }
        val total = monthly.fold(Money.zero(currency)) { acc, m -> acc + m.total }
        return SpendingHistory(months = monthly, total = total)
    }

    /** The price in effect on [date]: the latest change at or before it, else the earliest known. */
    private fun priceOn(date: LocalDate, sortedPrices: List<PriceChange>, fallback: Money): Money =
        sortedPrices.lastOrNull { !it.effectiveFrom.isAfter(date) }?.price
            ?: sortedPrices.firstOrNull()?.price
            ?: fallback

    companion object {
        const val DEFAULT_MONTHS = 12
    }
}
