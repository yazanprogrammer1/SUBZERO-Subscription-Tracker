package com.subzero.core.domain.model

import java.math.BigDecimal
import java.math.MathContext

enum class CycleUnit { DAY, WEEK, MONTH, YEAR }

/**
 * How often a subscription charges. Every cycle is "[every] × [unit]"; the named objects are the
 * common cases and [Custom] covers the rest. Use [of] to build cycles from stored values so that
 * `(1, MONTH)` always resolves to [Monthly] and equality is meaningful.
 */
sealed interface BillingCycle {
    val every: Int
    val unit: CycleUnit

    data object Weekly : BillingCycle {
        override val every = 1
        override val unit = CycleUnit.WEEK
    }

    data object Monthly : BillingCycle {
        override val every = 1
        override val unit = CycleUnit.MONTH
    }

    data object Quarterly : BillingCycle {
        override val every = 3
        override val unit = CycleUnit.MONTH
    }

    data object Yearly : BillingCycle {
        override val every = 1
        override val unit = CycleUnit.YEAR
    }

    data class Custom(override val every: Int, override val unit: CycleUnit) : BillingCycle {
        init {
            require(every in 1..MAX_EVERY) { "Custom cycle must repeat every 1..$MAX_EVERY units" }
            require(standardFor(every, unit) == null) {
                "($every, $unit) is a standard cycle; use BillingCycle.of()"
            }
        }
    }

    /**
     * How many times this cycle charges per year, as an exact fraction.
     * Weekly = 52, daily = 365 / every. This is the single definition used for normalization.
     */
    val occurrencesPerYear: BigDecimal
        get() {
            val perYear: BigDecimal = when (unit) {
                CycleUnit.DAY -> BigDecimal(DAYS_PER_YEAR)
                CycleUnit.WEEK -> BigDecimal(WEEKS_PER_YEAR)
                CycleUnit.MONTH -> BigDecimal(MONTHS_PER_YEAR)
                CycleUnit.YEAR -> BigDecimal.ONE
            }
            return perYear.divide(BigDecimal(every), MathContext.DECIMAL128)
        }

    companion object {
        const val MAX_EVERY = 365
        private const val DAYS_PER_YEAR = 365
        private const val WEEKS_PER_YEAR = 52
        private const val MONTHS_PER_YEAR = 12

        /**
         * A getter rather than a stored list: the data objects and this companion initialize
         * together on the JVM, and a stored list would capture nulls.
         */
        val standard: List<BillingCycle>
            get() = listOf(Weekly, Monthly, Quarterly, Yearly)

        fun of(every: Int, unit: CycleUnit): BillingCycle =
            standardFor(every, unit) ?: Custom(every, unit)

        private fun standardFor(every: Int, unit: CycleUnit): BillingCycle? =
            standard.firstOrNull { it.every == every && it.unit == unit }
    }
}
