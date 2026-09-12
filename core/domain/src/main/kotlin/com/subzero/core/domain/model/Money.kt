package com.subzero.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An exact monetary amount: [amountMinor] minor units (cents, pence, fils…) of [currency].
 *
 * Money is never a floating-point number. The only place fractions can appear is when an
 * amount is multiplied by a factor (e.g. converting a yearly price to a monthly equivalent);
 * that goes through [times], which rounds `HALF_EVEN` back to minor units.
 *
 * Arithmetic between different currencies is a programming error and throws.
 */
data class Money(
    val amountMinor: Long,
    val currency: CurrencyCode,
) : Comparable<Money> {

    init {
        require(amountMinor >= 0) { "Money cannot be negative: $amountMinor $currency" }
    }

    val isZero: Boolean get() = amountMinor == 0L

    /** The amount in major units, e.g. 1549 USD → 15.49. */
    fun toMajorUnits(): BigDecimal =
        BigDecimal.valueOf(amountMinor).movePointLeft(currency.fractionDigits)

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amountMinor = Math.addExact(amountMinor, other.amountMinor))
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        require(other.amountMinor <= amountMinor) { "Result would be negative" }
        return copy(amountMinor = amountMinor - other.amountMinor)
    }

    /** Multiplies by an exact factor and rounds to minor units with `HALF_EVEN`. */
    operator fun times(factor: BigDecimal): Money =
        copy(
            amountMinor = BigDecimal.valueOf(amountMinor)
                .multiply(factor)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact(),
        )

    /** Divides by an exact divisor and rounds to minor units with `HALF_EVEN`. */
    operator fun div(divisor: BigDecimal): Money {
        require(divisor.signum() > 0) { "Divisor must be positive" }
        return copy(
            amountMinor = BigDecimal.valueOf(amountMinor)
                .divide(divisor, 0, RoundingMode.HALF_EVEN)
                .longValueExact(),
        )
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountMinor.compareTo(other.amountMinor)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot combine $currency with ${other.currency}; convert first"
        }
    }

    override fun toString(): String = "${toMajorUnits().toPlainString()} $currency"

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0, currency)

        /** Builds Money from a major-unit amount such as 15.49; fails if it has too many decimals. */
        fun ofMajor(major: BigDecimal, currency: CurrencyCode): Money {
            val minor = major.movePointRight(currency.fractionDigits)
            require(minor.stripTrailingZeros().scale() <= 0) {
                "$major has more decimals than $currency allows (${currency.fractionDigits})"
            }
            return Money(minor.longValueExact(), currency)
        }

        /**
         * Parses user input like "15.49" or "15,49" (either decimal separator, no grouping
         * characters) in the given currency. Returns null for anything that is not a
         * non-negative number with a precision the currency allows.
         */
        fun parse(input: String, currency: CurrencyCode): Money? {
            val text = input.trim()
            if (text.isEmpty()) return null
            if (text.count { it == '.' || it == ',' } > 1) return null
            val normalized = text.replace(',', '.')
            if (!normalized.all { it.isDigit() || it == '.' }) return null
            if (normalized == ".") return null
            val major = normalized.toBigDecimalOrNull() ?: return null
            return runCatching { ofMajor(major, currency) }.getOrNull()
        }
    }
}
