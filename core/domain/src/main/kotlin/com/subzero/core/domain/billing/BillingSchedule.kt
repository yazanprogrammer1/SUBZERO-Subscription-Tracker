package com.subzero.core.domain.billing

import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * All billing-date arithmetic for SUBZERO.
 *
 * Every date is computed as the *nth occurrence from the anchor*, never by stepping from the
 * previous date. That is what keeps a subscription anchored on the 31st on the 31st after
 * February (Jan 31 → Feb 28 → Mar 31), and a Feb 29 anchor on Feb 29 in the next leap year.
 */
object BillingSchedule {

    private val TWELVE = BigDecimal(12)

    /** The date of the [n]th charge (0 = the anchor itself). */
    fun occurrence(anchor: LocalDate, cycle: BillingCycle, n: Long): LocalDate {
        require(n >= 0) { "Occurrence index must be >= 0" }
        val steps = Math.multiplyExact(n, cycle.every.toLong())
        return when (cycle.unit) {
            CycleUnit.DAY -> anchor.plusDays(steps)
            CycleUnit.WEEK -> anchor.plusWeeks(steps)
            CycleUnit.MONTH -> anchor.plusMonths(steps)
            CycleUnit.YEAR -> anchor.plusYears(steps)
        }
    }

    /** The first charge on or after [date]; the anchor itself when [date] is not after it. */
    fun nextChargeOnOrAfter(anchor: LocalDate, cycle: BillingCycle, date: LocalDate): LocalDate {
        if (!date.isAfter(anchor)) return anchor
        return occurrence(anchor, cycle, indexOfFirstOnOrAfter(anchor, cycle, date))
    }

    /** The first charge strictly after [date]. */
    fun nextChargeAfter(anchor: LocalDate, cycle: BillingCycle, date: LocalDate): LocalDate =
        nextChargeOnOrAfter(anchor, cycle, date.plusDays(1))

    /** Every charge date in the closed range [from, to], in order. */
    fun chargesBetween(
        anchor: LocalDate,
        cycle: BillingCycle,
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> {
        if (to.isBefore(from) || to.isBefore(anchor)) return emptyList()
        val start = maxOf(from, anchor)
        var n = indexOfFirstOnOrAfter(anchor, cycle, start)
        val result = mutableListOf<LocalDate>()
        while (true) {
            val date = occurrence(anchor, cycle, n)
            if (date.isAfter(to)) break
            result += date
            n++
        }
        return result
    }

    /** Yearly-equivalent amount, rounded HALF_EVEN to minor units. */
    fun yearlyEquivalent(price: Money, cycle: BillingCycle): Money =
        price * cycle.occurrencesPerYear

    /** Monthly-equivalent amount: the yearly equivalent divided by twelve. */
    fun monthlyEquivalent(price: Money, cycle: BillingCycle): Money =
        price * cycle.occurrencesPerYear.divide(TWELVE, java.math.MathContext.DECIMAL128)

    /**
     * Index of the first occurrence on or after [date], for a [date] after the anchor.
     * Starts from a unit-based estimate and corrects in both directions, so month-end clamping
     * never produces an off-by-one.
     */
    private fun indexOfFirstOnOrAfter(anchor: LocalDate, cycle: BillingCycle, date: LocalDate): Long {
        val elapsedUnits = when (cycle.unit) {
            CycleUnit.DAY -> ChronoUnit.DAYS.between(anchor, date)
            CycleUnit.WEEK -> ChronoUnit.WEEKS.between(anchor, date)
            CycleUnit.MONTH -> ChronoUnit.MONTHS.between(anchor, date)
            CycleUnit.YEAR -> ChronoUnit.YEARS.between(anchor, date)
        }
        var n = (elapsedUnits / cycle.every).coerceAtLeast(0)
        while (occurrence(anchor, cycle, n).isBefore(date)) n++
        while (n > 0 && !occurrence(anchor, cycle, n - 1).isBefore(date)) n--
        return n
    }
}
