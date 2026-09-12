package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PaymentRecordId
import com.subzero.core.domain.model.PaymentSource
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.SubscriptionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Moves every ACTIVE subscription whose next billing date has arrived forward past today,
 * recording one [PaymentRecord] per charge that occurred in the meantime.
 *
 * Runs on app open and from the daily worker. It is idempotent: records are keyed by
 * (subscription, date) in the repository, and a subscription already in the future is skipped.
 * Paused and canceled subscriptions are never touched.
 */
class RollForwardBillingDatesUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    data class Result(val recordedPayments: Int, val updatedSubscriptions: Int)

    suspend operator fun invoke(today: LocalDate = LocalDate.now(clock)): Result {
        val due = repository.getSubscriptions().filter { it.isActive && !it.nextBillingDate.isAfter(today) }
        if (due.isEmpty()) return Result(0, 0)

        val records = mutableListOf<PaymentRecord>()
        val updated = mutableListOf<Subscription>()
        val now = clock.instant()
        for (subscription in due) {
            val charges = BillingSchedule.chargesBetween(
                anchor = subscription.anchorDate,
                cycle = subscription.billingCycle,
                from = subscription.nextBillingDate,
                to = today,
            )
            charges.mapTo(records) { date ->
                PaymentRecord(
                    id = PaymentRecordId.random(),
                    subscriptionId = subscription.id,
                    amount = subscription.price,
                    paidOn = date,
                    source = PaymentSource.RECORDED,
                )
            }
            updated += subscription.copy(
                nextBillingDate = BillingSchedule.nextChargeAfter(subscription.anchorDate, subscription.billingCycle, today),
                updatedAt = now,
            )
        }
        repository.applyRollover(records, updated)
        return Result(recordedPayments = records.size, updatedSubscriptions = updated.size)
    }
}
