package com.subzero.core.domain.format

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.testing.jpy
import com.subzero.core.domain.testing.usd
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {

    private val us = MoneyFormatter(Locale.US)

    @Test
    fun `formats with the currency symbol and fraction digits`() {
        assertThat(us.format(usd(1549))).isEqualTo("$15.49")
        assertThat(us.format(usd(104976))).isEqualTo("$1,049.76")
        assertThat(us.format(usd(0))).isEqualTo("$0.00")
        assertThat(us.format(jpy(1500))).isEqualTo("¥1,500")
        assertThat(us.format(Money(12345, CurrencyCode("KWD")))).isEqualTo("KWD12.345")
    }

    @Test
    fun `compact format drops zero fractions only`() {
        assertThat(us.formatCompact(usd(2000))).isEqualTo("$20")
        assertThat(us.formatCompact(usd(1549))).isEqualTo("$15.49")
        assertThat(us.formatCompact(jpy(1500))).isEqualTo("¥1,500")
    }

    @Test
    fun `other locales place the symbol per their conventions`() {
        val de = MoneyFormatter(Locale.GERMANY)
        assertThat(de.format(Money(1549, CurrencyCode.EUR))).isEqualTo("15,49 €")
    }

    @Test
    fun `unknown currency codes fall back to the code itself`() {
        assertThat(us.symbol(CurrencyCode("ZZZ"))).isEqualTo("ZZZ")
        assertThat(us.symbol(CurrencyCode.USD)).isEqualTo("$")
    }
}
