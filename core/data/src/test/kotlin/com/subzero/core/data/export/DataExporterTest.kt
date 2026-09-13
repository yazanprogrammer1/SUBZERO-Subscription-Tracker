package com.subzero.core.data.export

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class DataExporterTest {

    private val repository = FakeSubscriptionRepository()
    private val exporter = DataExporter(repository, fixedClock(date("2026-09-13")))

    private fun seed() {
        repository.seed(
            subscription(id = "n", name = "Netflix, Premium", price = usd(1549), anchorDate = date("2026-01-16"), nextBillingDate = date("2026-09-16"), notes = "Says \"family\""),
            subscription(id = "a", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly),
        )
        repository.seedPrices(priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2026-01-16")), priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-08-20")))
        repository.seedPayments(paymentRecord(subscriptionId = "n", paidOn = date("2026-08-16")))
    }

    @Test
    fun `json export is complete and round-trips through the schema`() = runTest {
        seed()
        val text = exporter.export(ExportFormat.JSON)
        val document = Json.decodeFromString<ExportDocument>(text)

        assertThat(document.app).isEqualTo("SUBZERO")
        assertThat(document.schemaVersion).isEqualTo(1)
        assertThat(document.exportedAt).startsWith("2026-09-13")
        val netflix = document.subscriptions.first { it.id == "n" }
        assertThat(netflix.amountMinor).isEqualTo(1549)
        assertThat(netflix.currency).isEqualTo("USD")
        assertThat(netflix.cycleUnit).isEqualTo("MONTH")
        assertThat(netflix.priceHistory.map { it.amountMinor }).containsExactly(1399L, 1549L).inOrder()
        assertThat(netflix.payments).hasSize(1)
        assertThat(netflix.notes).isEqualTo("Says \"family\"")
    }

    @Test
    fun `csv export quotes commas and quotes`() = runTest {
        seed()
        val lines = exporter.export(ExportFormat.CSV).trim().lines()
        assertThat(lines.first()).isEqualTo("name,amount,currency,cycle,first_payment,next_payment,category,status,usage,notes")
        assertThat(lines).hasSize(3)
        val netflix = lines.first { it.startsWith("\"Netflix, Premium\"") }
        assertThat(netflix).contains(",15.49,USD,1 MONTH,2026-01-16,2026-09-16,ENTERTAINMENT,ACTIVE,UNKNOWN,\"Says \"\"family\"\"\"")
        assertThat(lines.first { it.startsWith("Adobe") }).contains(",599.88,USD,1 YEAR,")
    }

    @Test
    fun `empty database exports an empty document`() = runTest {
        val document = Json.decodeFromString<ExportDocument>(exporter.export(ExportFormat.JSON))
        assertThat(document.subscriptions).isEmpty()
        assertThat(exporter.export(ExportFormat.CSV).trim().lines()).hasSize(1)
    }
}
