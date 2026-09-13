package com.subzero.core.domain.assistant

import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.UpcomingPayment
import java.time.LocalDate

/** Everything an assistant may answer from. Built by the caller; the assistant never fetches. */
data class AssistantContext(
    val subscriptions: List<Subscription>,
    val priceChanges: List<PriceChange>,
    val homeCurrency: CurrencyCode,
    val today: LocalDate,
)

/**
 * What the user asked, once understood. Typed so answers are computed, not generated.
 */
sealed interface AssistantIntent {
    data object TotalSpend : AssistantIntent
    data class SpendInCategory(val category: Category) : AssistantIntent
    data class TopSubscriptions(val count: Int) : AssistantIntent
    data object CancelCandidates : AssistantIntent
    data object NextCharge : AssistantIntent
    data object UpcomingThisMonth : AssistantIntent
    data object PriceIncreases : AssistantIntent
    data object CountByCategory : AssistantIntent
    data object Unknown : AssistantIntent
}

/**
 * A structured answer: a headline amount when there is one, a lead sentence, an optional list
 * of subscriptions with a per-item amount, and an optional follow-up. Wording is localized by
 * the UI from [kind]; the assistant only supplies facts.
 */
data class AssistantAnswer(
    val kind: AnswerKind,
    val amount: Money? = null,
    val items: List<AnswerItem> = emptyList(),
    val count: Int = 0,
    val category: Category? = null,
    val date: LocalDate? = null,
    /** True when some subscriptions were left out because they are in another currency. */
    val excludedForeignCurrency: Int = 0,
    /** Free text, only for [AnswerKind.TEXT] answers produced by a hosted model. */
    val text: String? = null,
    /** Whether a hosted model produced this answer (shown to the user). */
    val isRemote: Boolean = false,
    /** True when the hosted model was requested but the on-device assistant answered instead. */
    val fellBack: Boolean = false,
)

data class AnswerItem(
    val subscription: Subscription,
    val amount: Money,
    /** For price changes: the previous amount. */
    val previousAmount: Money? = null,
    val date: LocalDate? = null,
)

enum class AnswerKind {
    TOTAL_SPEND,
    SPEND_IN_CATEGORY,
    NOTHING_IN_CATEGORY,
    TOP_SUBSCRIPTIONS,
    CANCEL_CANDIDATES,
    NO_CANCEL_CANDIDATES,
    NO_USAGE_DATA,
    NEXT_CHARGE,
    NO_UPCOMING,
    UPCOMING_THIS_MONTH,
    PRICE_INCREASES,
    NO_PRICE_INCREASES,
    COUNT_BY_CATEGORY,
    NO_SUBSCRIPTIONS,
    UNKNOWN,

    /** A free-text answer from a hosted model; see [AssistantAnswer.text]. */
    TEXT,
}

/**
 * The assistant contract. [LocalAssistant] answers deterministically on-device; a remote model
 * can implement the same contract later and fall back to the local one when unavailable.
 */
interface Assistant {
    suspend fun ask(question: String, context: AssistantContext): AssistantAnswer
}

/** A [UpcomingPayment] paired with the answer it belongs to; convenience for UI mapping. */
fun UpcomingPayment.toAnswerItem() = AnswerItem(subscription = subscription, amount = amount, date = date)
