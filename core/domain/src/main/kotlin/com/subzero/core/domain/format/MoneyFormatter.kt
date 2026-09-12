package com.subzero.core.domain.format

import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Locale-aware money formatting, e.g. 1549 USD → "$15.49" (en-US) or "15,49 $" (fr-FR).
 * Formatting is the only place a Money becomes text; nothing parses these strings back.
 */
class MoneyFormatter(private val locale: Locale) {

    fun format(money: Money): String = formatter(money.currency).format(money.toMajorUnits())

    /** Same as [format] but without fraction digits when the amount is whole, e.g. "$20". */
    fun formatCompact(money: Money): String {
        val hasFraction = money.amountMinor % pow10(money.currency.fractionDigits) != 0L
        val formatter = formatter(money.currency)
        if (!hasFraction) {
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 0
        }
        return formatter.format(money.toMajorUnits())
    }

    /** The currency symbol alone, e.g. "$" or "EUR" when the locale has no symbol. */
    fun symbol(currency: CurrencyCode): String =
        runCatching { Currency.getInstance(currency.code).getSymbol(locale) }
            .getOrDefault(currency.code)

    private fun formatter(currency: CurrencyCode): NumberFormat {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { Currency.getInstance(currency.code) }
            .onSuccess { format.currency = it }
        format.minimumFractionDigits = currency.fractionDigits
        format.maximumFractionDigits = currency.fractionDigits
        return format
    }

    private fun pow10(digits: Int): Long {
        var result = 1L
        repeat(digits) { result *= 10 }
        return result
    }
}
