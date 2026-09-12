package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.PriceChangeId
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.repository.SubscriptionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class AddSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: SubscriptionDraft): SaveSubscriptionResult {
        val errors = SubscriptionValidator.validate(draft)
        if (errors.isNotEmpty()) return SaveSubscriptionResult.Invalid(errors)
        val price = checkNotNull(draft.price)

        val now = clock.instant()
        val today = LocalDate.now(clock)
        val id = SubscriptionId.random()
        val subscription = Subscription(
            id = id,
            name = draft.name.trim(),
            price = price,
            billingCycle = draft.billingCycle,
            anchorDate = draft.anchorDate,
            nextBillingDate = BillingSchedule.nextChargeOnOrAfter(
                anchor = draft.anchorDate,
                cycle = draft.billingCycle,
                date = today,
            ),
            category = draft.category,
            status = SubscriptionStatus.ACTIVE,
            usage = draft.usage,
            notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
            statusChangedAt = null,
        )
        val initialPrice = PriceChange(
            id = PriceChangeId.random(),
            subscriptionId = id,
            price = price,
            effectiveFrom = draft.anchorDate,
        )
        repository.addSubscription(subscription, initialPrice)
        return SaveSubscriptionResult.Saved(subscription)
    }
}

class UpdateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: SubscriptionId, draft: SubscriptionDraft): SaveSubscriptionResult {
        val errors = SubscriptionValidator.validate(draft)
        if (errors.isNotEmpty()) return SaveSubscriptionResult.Invalid(errors)
        val existing = repository.getSubscription(id) ?: return SaveSubscriptionResult.NotFound
        val price = checkNotNull(draft.price)

        val today = LocalDate.now(clock)
        val scheduleChanged =
            draft.anchorDate != existing.anchorDate || draft.billingCycle != existing.billingCycle
        val updated = existing.copy(
            name = draft.name.trim(),
            price = price,
            billingCycle = draft.billingCycle,
            anchorDate = draft.anchorDate,
            nextBillingDate = if (scheduleChanged) {
                BillingSchedule.nextChargeOnOrAfter(draft.anchorDate, draft.billingCycle, today)
            } else {
                existing.nextBillingDate
            },
            category = draft.category,
            usage = draft.usage,
            notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = clock.instant(),
        )
        val priceChange = if (price != existing.price) {
            PriceChange(
                id = PriceChangeId.random(),
                subscriptionId = id,
                price = price,
                effectiveFrom = today,
            )
        } else {
            null
        }
        repository.updateSubscription(updated, priceChange)
        return SaveSubscriptionResult.Saved(updated)
    }
}

class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(id: SubscriptionId) = repository.deleteSubscription(id)
}

class SetSubscriptionStatusUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    /**
     * Changes the status. Resuming a paused or canceled subscription re-projects the next
     * billing date from the anchor so it is never in the past.
     */
    suspend operator fun invoke(id: SubscriptionId, status: SubscriptionStatus): Boolean {
        val existing = repository.getSubscription(id) ?: return false
        if (existing.status == status) return true
        val now = clock.instant()
        val nextBillingDate = if (status == SubscriptionStatus.ACTIVE) {
            BillingSchedule.nextChargeOnOrAfter(existing.anchorDate, existing.billingCycle, LocalDate.now(clock))
        } else {
            existing.nextBillingDate
        }
        repository.updateSubscription(
            existing.copy(
                status = status,
                nextBillingDate = nextBillingDate,
                updatedAt = now,
                statusChangedAt = now,
            ),
        )
        return true
    }
}

class SetDeclaredUsageUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: SubscriptionId, usage: DeclaredUsage): Boolean {
        val existing = repository.getSubscription(id) ?: return false
        if (existing.usage == usage) return true
        repository.updateSubscription(existing.copy(usage = usage, updatedAt = clock.instant()))
        return true
    }
}
