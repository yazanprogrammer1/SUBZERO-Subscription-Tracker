package com.subzero.core.domain.model

import java.util.Currency

/**
 * An ISO 4217 currency code such as "USD", "EUR" or "JPY".
 *
 * Minor-unit digits come from the JDK currency table so JPY (0) and KWD (3) are handled without
 * any special casing in the app.
 */
@JvmInline
value class CurrencyCode(val code: String) {
    init {
        require(code.length == 3 && code.all { it.isUpperCase() && it in 'A'..'Z' }) {
            "Currency code must be three uppercase letters, was: $code"
        }
    }

    /** Number of digits after the decimal separator (2 for USD, 0 for JPY, 3 for KWD). */
    val fractionDigits: Int
        get() = runCatching { Currency.getInstance(code).defaultFractionDigits }
            .getOrDefault(DEFAULT_FRACTION_DIGITS)
            .let { if (it < 0) DEFAULT_FRACTION_DIGITS else it }

    /** True when the JDK knows this code; unknown codes still work with two fraction digits. */
    val isKnown: Boolean
        get() = runCatching { Currency.getInstance(code) }.isSuccess

    override fun toString(): String = code

    companion object {
        private const val DEFAULT_FRACTION_DIGITS = 2

        val USD = CurrencyCode("USD")
        val EUR = CurrencyCode("EUR")
        val GBP = CurrencyCode("GBP")

        /** All currencies the JDK knows, sorted by code. Used by the currency picker. */
        fun available(): List<CurrencyCode> =
            Currency.getAvailableCurrencies()
                .filter { it.defaultFractionDigits >= 0 }
                .map { CurrencyCode(it.currencyCode) }
                .sortedBy { it.code }
    }
}
