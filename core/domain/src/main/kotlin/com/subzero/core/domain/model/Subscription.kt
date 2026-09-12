package com.subzero.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A recurring charge the user pays for.
 *
 * [anchorDate] is the first billing date the user knows about and is the truth for all date
 * math; [nextBillingDate] is a cached projection that the daily rollover keeps ≥ today.
 */
data class Subscription(
    val id: SubscriptionId,
    val name: String,
    val price: Money,
    val billingCycle: BillingCycle,
    val anchorDate: LocalDate,
    val nextBillingDate: LocalDate,
    val category: Category,
    val status: SubscriptionStatus,
    val usage: DeclaredUsage,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** When [status] last changed; null while it has never left ACTIVE. */
    val statusChangedAt: Instant?,
) {
    val isActive: Boolean get() = status == SubscriptionStatus.ACTIVE

    companion object {
        const val MAX_NAME_LENGTH = 80
        const val MAX_NOTES_LENGTH = 500
    }
}

/** A price that took effect on [effectiveFrom]. The first entry is the original price. */
data class PriceChange(
    val id: PriceChangeId,
    val subscriptionId: SubscriptionId,
    val price: Money,
    val effectiveFrom: LocalDate,
)

/** A single charge that happened (or is estimated to have happened) on [paidOn]. */
data class PaymentRecord(
    val id: PaymentRecordId,
    val subscriptionId: SubscriptionId,
    val amount: Money,
    val paidOn: LocalDate,
    val source: PaymentSource,
)

/** A projected future charge, derived from the schedule; never stored. */
data class UpcomingPayment(
    val subscription: Subscription,
    val date: LocalDate,
    val amount: Money,
)
