package com.subzero.core.domain.usecase

import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import java.time.LocalDate

/** What the add/edit form collects. Validated by [SubscriptionValidator] before it becomes a [Subscription]. */
data class SubscriptionDraft(
    val name: String,
    val price: Money?,
    val billingCycle: BillingCycle,
    /** The next date the user expects to be charged. */
    val nextPaymentDate: LocalDate,
    /**
     * When the subscription first charged, if the user knows. When present it becomes the anchor
     * for all date math and lets SUBZERO estimate spending before the app was installed;
     * otherwise [nextPaymentDate] is the anchor.
     */
    val firstPaymentDate: LocalDate? = null,
    val category: Category,
    val usage: DeclaredUsage = DeclaredUsage.UNKNOWN,
    val notes: String? = null,
)

enum class SubscriptionValidationError {
    NAME_BLANK,
    NAME_TOO_LONG,
    PRICE_MISSING,
    NOTES_TOO_LONG,
    FIRST_PAYMENT_AFTER_NEXT,
}

sealed interface SaveSubscriptionResult {
    data class Saved(val subscription: Subscription) : SaveSubscriptionResult
    data class Invalid(val errors: Set<SubscriptionValidationError>) : SaveSubscriptionResult
    data object NotFound : SaveSubscriptionResult
}

object SubscriptionValidator {
    fun validate(draft: SubscriptionDraft): Set<SubscriptionValidationError> = buildSet {
        val name = draft.name.trim()
        if (name.isEmpty()) add(SubscriptionValidationError.NAME_BLANK)
        if (name.length > Subscription.MAX_NAME_LENGTH) add(SubscriptionValidationError.NAME_TOO_LONG)
        if (draft.price == null) add(SubscriptionValidationError.PRICE_MISSING)
        if ((draft.notes?.length ?: 0) > Subscription.MAX_NOTES_LENGTH) {
            add(SubscriptionValidationError.NOTES_TOO_LONG)
        }
        if (draft.firstPaymentDate?.isAfter(draft.nextPaymentDate) == true) {
            add(SubscriptionValidationError.FIRST_PAYMENT_AFTER_NEXT)
        }
    }
}

/** The date all billing math is anchored on. */
val SubscriptionDraft.anchorDate: LocalDate
    get() = firstPaymentDate ?: nextPaymentDate
