package com.subzero.core.domain.billing

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.jpy
import com.subzero.core.domain.testing.usd
import org.junit.Test

/** Edge cases from the specification (§43) as named tests. */
class BillingScheduleTest {

    // --- occurrence: anchored, never iterative ---

    @Test
    fun `monthly anchored on the 31st clamps to month end and returns to the 31st`() {
        val anchor = date("2026-01-31")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 1)).isEqualTo(date("2026-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 2)).isEqualTo(date("2026-03-31"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 3)).isEqualTo(date("2026-04-30"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 4)).isEqualTo(date("2026-05-31"))
    }

    @Test
    fun `monthly anchored on the 31st in a leap year gives Feb 29`() {
        val anchor = date("2028-01-31")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 1)).isEqualTo(date("2028-02-29"))
    }

    @Test
    fun `yearly anchored on Feb 29 falls back to Feb 28 and returns on the next leap year`() {
        val anchor = date("2024-02-29")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Yearly, 1)).isEqualTo(date("2025-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Yearly, 2)).isEqualTo(date("2026-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Yearly, 4)).isEqualTo(date("2028-02-29"))
    }

    @Test
    fun `monthly anchored on Feb 29 keeps day 29 in later months`() {
        val anchor = date("2024-02-29")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 1)).isEqualTo(date("2024-03-29"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 12)).isEqualTo(date("2025-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Monthly, 13)).isEqualTo(date("2025-03-29"))
    }

    @Test
    fun `quarterly steps three months from the anchor`() {
        val anchor = date("2026-11-30")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Quarterly, 1)).isEqualTo(date("2027-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Quarterly, 2)).isEqualTo(date("2027-05-30"))
    }

    @Test
    fun `weekly and daily custom cycles step by days`() {
        val anchor = date("2026-12-28")
        assertThat(BillingSchedule.occurrence(anchor, BillingCycle.Weekly, 1)).isEqualTo(date("2027-01-04"))
        val every10Days = BillingCycle.of(10, CycleUnit.DAY)
        assertThat(BillingSchedule.occurrence(anchor, every10Days, 1)).isEqualTo(date("2027-01-07"))
    }

    @Test
    fun `custom every two years`() {
        val anchor = date("2024-02-29")
        val biennial = BillingCycle.of(2, CycleUnit.YEAR)
        assertThat(BillingSchedule.occurrence(anchor, biennial, 1)).isEqualTo(date("2026-02-28"))
        assertThat(BillingSchedule.occurrence(anchor, biennial, 2)).isEqualTo(date("2028-02-29"))
    }

    // --- nextChargeOnOrAfter ---

    @Test
    fun `next charge is the anchor when the date is not after it`() {
        val anchor = date("2026-09-16")
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-09-01"))).isEqualTo(anchor)
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, anchor)).isEqualTo(anchor)
    }

    @Test
    fun `next charge on the charge day is that day`() {
        val anchor = date("2026-01-16")
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-09-16")))
            .isEqualTo(date("2026-09-16"))
    }

    @Test
    fun `next charge the day after a charge is the following occurrence`() {
        val anchor = date("2026-01-16")
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-09-17")))
            .isEqualTo(date("2026-10-16"))
        assertThat(BillingSchedule.nextChargeAfter(anchor, BillingCycle.Monthly, date("2026-09-16")))
            .isEqualTo(date("2026-10-16"))
    }

    @Test
    fun `next charge for a 31st anchor across a clamped month is correct`() {
        val anchor = date("2026-01-31")
        // Feb 28 is the February charge; asking on Feb 28 returns Feb 28.
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-02-28")))
            .isEqualTo(date("2026-02-28"))
        // Asking on Mar 1 returns Mar 31, not Mar 28.
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-03-01")))
            .isEqualTo(date("2026-03-31"))
        // Asking on Mar 30 still returns Mar 31.
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2026-03-30")))
            .isEqualTo(date("2026-03-31"))
    }

    @Test
    fun `next charge years in the future is computed without iteration issues`() {
        val anchor = date("2016-02-29")
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Monthly, date("2036-03-01")))
            .isEqualTo(date("2036-03-29"))
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Yearly, date("2036-03-01")))
            .isEqualTo(date("2037-02-28"))
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Weekly, date("2036-03-01")))
            .isEqualTo(date("2036-03-03"))
    }

    @Test
    fun `next charge for yearly anchored Feb 29 asked on Feb 28 of a common year`() {
        val anchor = date("2024-02-29")
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Yearly, date("2026-02-28")))
            .isEqualTo(date("2026-02-28"))
        assertThat(BillingSchedule.nextChargeOnOrAfter(anchor, BillingCycle.Yearly, date("2026-03-01")))
            .isEqualTo(date("2027-02-28"))
    }

    // --- chargesBetween ---

    @Test
    fun `charges between returns every occurrence inside the closed range`() {
        val anchor = date("2026-01-31")
        val charges = BillingSchedule.chargesBetween(anchor, BillingCycle.Monthly, date("2026-02-01"), date("2026-05-31"))
        assertThat(charges).containsExactly(
            date("2026-02-28"),
            date("2026-03-31"),
            date("2026-04-30"),
            date("2026-05-31"),
        ).inOrder()
    }

    @Test
    fun `charges between clamps to the anchor and returns empty for ranges before it`() {
        val anchor = date("2026-09-16")
        assertThat(BillingSchedule.chargesBetween(anchor, BillingCycle.Monthly, date("2026-01-01"), date("2026-09-15"))).isEmpty()
        assertThat(BillingSchedule.chargesBetween(anchor, BillingCycle.Monthly, date("2026-01-01"), date("2026-09-16")))
            .containsExactly(date("2026-09-16"))
        assertThat(BillingSchedule.chargesBetween(anchor, BillingCycle.Monthly, date("2026-10-01"), date("2026-09-01"))).isEmpty()
    }

    @Test
    fun `weekly charges between lists each week`() {
        val anchor = date("2026-09-01")
        val charges = BillingSchedule.chargesBetween(anchor, BillingCycle.Weekly, date("2026-09-01"), date("2026-09-30"))
        assertThat(charges).hasSize(5)
        assertThat(charges.first()).isEqualTo(date("2026-09-01"))
        assertThat(charges.last()).isEqualTo(date("2026-09-29"))
    }

    // --- normalization ---

    @Test
    fun `monthly and yearly equivalents follow the documented factors`() {
        assertThat(BillingSchedule.yearlyEquivalent(usd(1549), BillingCycle.Monthly)).isEqualTo(usd(18588))
        assertThat(BillingSchedule.monthlyEquivalent(usd(1549), BillingCycle.Monthly)).isEqualTo(usd(1549))

        assertThat(BillingSchedule.yearlyEquivalent(usd(12000), BillingCycle.Yearly)).isEqualTo(usd(12000))
        assertThat(BillingSchedule.monthlyEquivalent(usd(12000), BillingCycle.Yearly)).isEqualTo(usd(1000))

        assertThat(BillingSchedule.yearlyEquivalent(usd(3000), BillingCycle.Quarterly)).isEqualTo(usd(12000))
        assertThat(BillingSchedule.monthlyEquivalent(usd(3000), BillingCycle.Quarterly)).isEqualTo(usd(1000))

        assertThat(BillingSchedule.yearlyEquivalent(usd(1000), BillingCycle.Weekly)).isEqualTo(usd(52000))
        assertThat(BillingSchedule.monthlyEquivalent(usd(1000), BillingCycle.Weekly)).isEqualTo(usd(4333))
    }

    @Test
    fun `custom cycles normalize by unit`() {
        assertThat(BillingSchedule.yearlyEquivalent(usd(10000), BillingCycle.of(2, CycleUnit.YEAR))).isEqualTo(usd(5000))
        assertThat(BillingSchedule.monthlyEquivalent(usd(10000), BillingCycle.of(2, CycleUnit.YEAR))).isEqualTo(usd(417))
        assertThat(BillingSchedule.yearlyEquivalent(usd(500), BillingCycle.of(6, CycleUnit.MONTH))).isEqualTo(usd(1000))
        assertThat(BillingSchedule.yearlyEquivalent(usd(100), BillingCycle.of(73, CycleUnit.DAY))).isEqualTo(usd(500))
        assertThat(BillingSchedule.yearlyEquivalent(usd(100), BillingCycle.of(2, CycleUnit.WEEK))).isEqualTo(usd(2600))
    }

    @Test
    fun `yearly equivalent of a monthly yen price stays whole`() {
        assertThat(BillingSchedule.yearlyEquivalent(jpy(1500), BillingCycle.Monthly)).isEqualTo(jpy(18000))
        assertThat(BillingSchedule.monthlyEquivalent(jpy(10000), BillingCycle.Yearly)).isEqualTo(jpy(833))
    }

    @Test
    fun `zero amounts normalize to zero`() {
        assertThat(BillingSchedule.yearlyEquivalent(usd(0), BillingCycle.Weekly)).isEqualTo(usd(0))
        assertThat(BillingSchedule.monthlyEquivalent(usd(0), BillingCycle.Yearly)).isEqualTo(usd(0))
    }

    // --- BillingCycle factory ---

    @Test
    fun `factory resolves standard cycles and rejects redundant customs`() {
        assertThat(BillingCycle.of(1, CycleUnit.MONTH)).isEqualTo(BillingCycle.Monthly)
        assertThat(BillingCycle.of(3, CycleUnit.MONTH)).isEqualTo(BillingCycle.Quarterly)
        assertThat(BillingCycle.of(1, CycleUnit.WEEK)).isEqualTo(BillingCycle.Weekly)
        assertThat(BillingCycle.of(1, CycleUnit.YEAR)).isEqualTo(BillingCycle.Yearly)
        assertThat(BillingCycle.of(2, CycleUnit.MONTH)).isEqualTo(BillingCycle.Custom(2, CycleUnit.MONTH))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            BillingCycle.Custom(1, CycleUnit.MONTH)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            BillingCycle.Custom(0, CycleUnit.DAY)
        }
    }
}
