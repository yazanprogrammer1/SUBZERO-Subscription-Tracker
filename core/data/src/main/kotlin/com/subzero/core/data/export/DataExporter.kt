package com.subzero.core.data.export

import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.SubscriptionRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Inject

enum class ExportFormat(val mimeType: String, val extension: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
}

/**
 * Turns the local database into a portable document the user owns. JSON is complete (price
 * history and payments included, amounts in minor units with currency); CSV is one row per
 * subscription with major-unit amounts, for spreadsheets.
 */
class DataExporter @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    suspend fun export(format: ExportFormat): String {
        val subscriptions = repository.getSubscriptions()
        return when (format) {
            ExportFormat.JSON -> json(subscriptions)
            ExportFormat.CSV -> csv(subscriptions)
        }
    }

    private suspend fun json(subscriptions: List<Subscription>): String {
        val document = ExportDocument(
            app = "SUBZERO",
            schemaVersion = SCHEMA_VERSION,
            exportedAt = clock.instant().toString(),
            subscriptions = subscriptions.map { subscription ->
                SubscriptionExport(
                    id = subscription.id.value,
                    name = subscription.name,
                    amountMinor = subscription.price.amountMinor,
                    currency = subscription.price.currency.code,
                    cycleEvery = subscription.billingCycle.every,
                    cycleUnit = subscription.billingCycle.unit.name,
                    anchorDate = subscription.anchorDate.toString(),
                    nextBillingDate = subscription.nextBillingDate.toString(),
                    category = subscription.category.name,
                    status = subscription.status.name,
                    usage = subscription.usage.name,
                    notes = subscription.notes,
                    createdAt = subscription.createdAt.toString(),
                    priceHistory = repository.getPriceHistory(subscription.id).map { it.toExport() },
                    payments = repository.getPaymentRecords(subscription.id).map { it.toExport() },
                )
            },
        )
        return jsonFormat.encodeToString(document)
    }

    private fun csv(subscriptions: List<Subscription>): String = buildString {
        appendLine(CSV_HEADER.joinToString(","))
        subscriptions.forEach { s ->
            listOf(
                s.name,
                s.price.toMajorUnits().toPlainString(),
                s.price.currency.code,
                "${s.billingCycle.every} ${s.billingCycle.unit.name}",
                s.anchorDate.toString(),
                s.nextBillingDate.toString(),
                s.category.name,
                s.status.name,
                s.usage.name,
                s.notes.orEmpty(),
            ).joinToString(",") { csvCell(it) }.let(::appendLine)
        }
    }

    /** RFC 4180: quote when needed, double the quotes inside. */
    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun PriceChange.toExport() = PriceChangeExport(amountMinor = price.amountMinor, currency = price.currency.code, effectiveFrom = effectiveFrom.toString())

    private fun PaymentRecord.toExport() = PaymentExport(amountMinor = amount.amountMinor, currency = amount.currency.code, paidOn = paidOn.toString(), source = source.name)

    companion object {
        const val SCHEMA_VERSION = 1
        private val jsonFormat = Json { prettyPrint = true; encodeDefaults = true }
        private val CSV_HEADER = listOf("name", "amount", "currency", "cycle", "first_payment", "next_payment", "category", "status", "usage", "notes")
    }
}

@Serializable
data class ExportDocument(
    val app: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val subscriptions: List<SubscriptionExport>,
)

@Serializable
data class SubscriptionExport(
    val id: String,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val cycleEvery: Int,
    val cycleUnit: String,
    val anchorDate: String,
    val nextBillingDate: String,
    val category: String,
    val status: String,
    val usage: String,
    val notes: String?,
    val createdAt: String,
    val priceHistory: List<PriceChangeExport>,
    val payments: List<PaymentExport>,
)

@Serializable
data class PriceChangeExport(val amountMinor: Long, val currency: String, val effectiveFrom: String)

@Serializable
data class PaymentExport(val amountMinor: Long, val currency: String, val paidOn: String, val source: String)
