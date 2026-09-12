package com.subzero.core.domain.model

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.testing.jpy
import com.subzero.core.domain.testing.usd
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `minor units convert to major units per currency`() {
        assertThat(usd(1549).toMajorUnits()).isEqualTo(BigDecimal("15.49"))
        assertThat(jpy(1500).toMajorUnits()).isEqualTo(BigDecimal("1500"))
        assertThat(Money(12345, CurrencyCode("KWD")).toMajorUnits()).isEqualTo(BigDecimal("12.345"))
    }

    @Test
    fun `ofMajor rejects more decimals than the currency allows`() {
        assertThat(Money.ofMajor(BigDecimal("15.49"), CurrencyCode.USD)).isEqualTo(usd(1549))
        assertThat(Money.ofMajor(BigDecimal("15.490"), CurrencyCode.USD)).isEqualTo(usd(1549))
        assertThrows(IllegalArgumentException::class.java) {
            Money.ofMajor(BigDecimal("15.495"), CurrencyCode.USD)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Money.ofMajor(BigDecimal("15.5"), CurrencyCode("JPY"))
        }
    }

    @Test
    fun `negative amounts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Money(-1, CurrencyCode.USD) }
    }

    @Test
    fun `zero amount is valid`() {
        assertThat(Money.zero(CurrencyCode.USD).isZero).isTrue()
        assertThat(usd(0) + usd(0)).isEqualTo(usd(0))
    }

    @Test
    fun `addition and subtraction stay in minor units`() {
        assertThat(usd(1549) + usd(2000)).isEqualTo(usd(3549))
        assertThat(usd(3549) - usd(2000)).isEqualTo(usd(1549))
    }

    @Test
    fun `subtraction below zero is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { usd(100) - usd(101) }
    }

    @Test
    fun `mixing currencies throws`() {
        assertThrows(IllegalArgumentException::class.java) { usd(100) + jpy(100) }
        assertThrows(IllegalArgumentException::class.java) { usd(100).compareTo(jpy(100)) }
    }

    @Test
    fun `multiplication rounds half even to minor units`() {
        assertThat(usd(1000) * BigDecimal("0.125")).isEqualTo(usd(125))
        // 1001 * 0.125 = 125.125 -> 125 (half-even, and .125 is below .5 anyway)
        assertThat(usd(1001) * BigDecimal("0.125")).isEqualTo(usd(125))
        // 1004 * 0.125 = 125.5 -> 126 (round to even)
        assertThat(usd(1004) * BigDecimal("0.125")).isEqualTo(usd(126))
        // 1012 * 0.125 = 126.5 -> 126 (round to even)
        assertThat(usd(1012) * BigDecimal("0.125")).isEqualTo(usd(126))
    }

    @Test
    fun `division rounds half even`() {
        assertThat(usd(1000) / BigDecimal(12)).isEqualTo(usd(83))
        assertThat(usd(1200) / BigDecimal(12)).isEqualTo(usd(100))
    }

    @Test
    fun `extremely large amounts do not overflow silently`() {
        val huge = Money(Long.MAX_VALUE - 1, CurrencyCode.USD)
        assertThat(huge.toMajorUnits()).isEqualTo(BigDecimal("92233720368547758.06"))
        assertThrows(ArithmeticException::class.java) { huge + usd(2) }
        assertThrows(ArithmeticException::class.java) { huge * BigDecimal(2) }
    }

    @Test
    fun `parse accepts either decimal separator and rejects garbage`() {
        assertThat(Money.parse("15.49", CurrencyCode.USD)).isEqualTo(usd(1549))
        assertThat(Money.parse("15,49", CurrencyCode.USD)).isEqualTo(usd(1549))
        assertThat(Money.parse(" 20 ", CurrencyCode.USD)).isEqualTo(usd(2000))
        assertThat(Money.parse("0", CurrencyCode.USD)).isEqualTo(usd(0))
        assertThat(Money.parse(".5", CurrencyCode.USD)).isEqualTo(usd(50))
        assertThat(Money.parse("1500", CurrencyCode("JPY"))).isEqualTo(jpy(1500))

        assertThat(Money.parse("", CurrencyCode.USD)).isNull()
        assertThat(Money.parse(".", CurrencyCode.USD)).isNull()
        assertThat(Money.parse("-5", CurrencyCode.USD)).isNull()
        assertThat(Money.parse("1,299.00", CurrencyCode.USD)).isNull()
        assertThat(Money.parse("15.495", CurrencyCode.USD)).isNull()
        assertThat(Money.parse("15.5", CurrencyCode("JPY"))).isNull()
        assertThat(Money.parse("abc", CurrencyCode.USD)).isNull()
    }

    @Test
    fun `currency fraction digits come from the JDK table`() {
        assertThat(CurrencyCode.USD.fractionDigits).isEqualTo(2)
        assertThat(CurrencyCode("JPY").fractionDigits).isEqualTo(0)
        assertThat(CurrencyCode("KWD").fractionDigits).isEqualTo(3)
        assertThat(CurrencyCode("ZZZ").fractionDigits).isEqualTo(2)
        assertThat(CurrencyCode("ZZZ").isKnown).isFalse()
    }

    @Test
    fun `currency code must be three uppercase letters`() {
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode("usd") }
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode("US") }
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode("") }
    }
}
