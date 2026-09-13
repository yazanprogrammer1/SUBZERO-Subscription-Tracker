package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Spend in one calendar month across all home-currency subscriptions. */
data class MonthTotal(
    val month: YearMonth,
    val total: Money,
    /** Part of [total] that is scheduled but has not happened yet (current month only). */
    val projected: Money,
    /** Part of [total] estimated from schedules before the app knew about a subscription. */
    val estimated: Money,
)

data class SpendTrend(
    val months: List<MonthTotal>,
    /** Change of the current month versus the previous one, in percent with one decimal; null when the previous month was zero. */
    val changeVersusPreviousPercent: BigDecimal?,
) {
    val current: MonthTotal get() = months.last()
    val previous: MonthTotal? get() = months.getOrNull(months.size - 2)
}

/**
 * Month-by-month totals for the dashboard trend. Past months combine recorded payments with
 * labelled estimates (per subscription, via [CalculateSpendingHistoryUseCase]); the current
 * month additionally includes charges still scheduled between tomorrow and month end, so the
 * number answers "what does this month cost me", not "what has been charged so far".
 * Only ACTIVE subscriptions contribute projections; foreign-currency ones are excluded entirely.
 */
class CalculateSpendTrendUseCase @Inject constructor(
    private val clock: Clock,
    private val calculateSpendingHistory: CalculateSpendingHistoryUseCase,
) {
    operator fun invoke(
        subscriptions: List<Subscription>,
        priceChanges: List<PriceChange>,
        records: List<PaymentRecord>,
        homeCurrency: CurrencyCode,
        months: Int = DEFAULT_MONTHS,
    ): SpendTrend {
        require(months >= 2)
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val firstMonth = currentMonth.minusMonths((months - 1).toLong())
        val zero = Money.zero(homeCurrency)

        val inHome = subscriptions.filter { it.price.currency == homeCurrency }
        val pricesById = priceChanges.groupBy(PriceChange::subscriptionId)
        val recordsById = records.groupBy(PaymentRecord::subscriptionId)

        val totals = Array(months) { zero }
        val estimated = Array(months) { zero }
        for (subscription in inHome) {
            val history = calculateSpendingHistory(
                subscription = subscription,
                priceHistory = pricesById[subscription.id].orEmpty(),
                records = recordsById[subscription.id].orEmpty(),
                months = months,
            )
            history.months.forEachIndexed { i, month ->
                totals[i] += month.total
                estimated[i] += month.estimated
            }
        }

        val projected = inHome.filter { it.isActive }.fold(zero) { acc, subscription ->
            val remaining = BillingSchedule.chargesBetween(
                anchor = subscription.anchorDate,
                cycle = subscription.billingCycle,
                from = today.plusDays(1),
                to = currentMonth.atEndOfMonth(),
            )
            acc + subscription.price * BigDecimal(remaining.size)
        }
        totals[months - 1] += projected

        val monthTotals = (0 until months).map { i ->
            MonthTotal(
                month = firstMonth.plusMonths(i.toLong()),
                total = totals[i],
                projected = if (i == months - 1) projected else zero,
                estimated = estimated[i],
            )
        }
        val previous = monthTotals[months - 2].total
        val change = if (previous.isZero) {
            null
        } else {
            BigDecimal.valueOf(monthTotals.last().total.amountMinor - previous.amountMinor)
                .multiply(BigDecimal(100))
                .divide(BigDecimal.valueOf(previous.amountMinor), 1, RoundingMode.HALF_EVEN)
        }
        return SpendTrend(months = monthTotals, changeVersusPreviousPercent = change)
    }

    companion object {
        const val DEFAULT_MONTHS = 6
    }
}
