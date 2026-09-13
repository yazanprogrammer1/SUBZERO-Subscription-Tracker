package com.subzero.core.domain.assistant

import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.UpcomingPayment
import java.time.LocalDate

/** Everything an assistant may answer from. Built by the caller; the assistant never fetches. */
data class AssistantContext(
    val subscriptions: List<Subscription>,
    val priceChanges: List<PriceChange>,
    val homeCurrency: CurrencyCode,
    val today: LocalDate,
    /** Payments already recorded, so "what did I actually pay" is answered from facts. */
    val payments: List<PaymentRecord> = emptyList(),
    /** What the previous answer was about, so follow-ups like "when does it charge?" resolve. */
    val focus: AssistantFocus? = null,
    /** Earlier turns, oldest first. Only a hosted model uses them. */
    val history: List<AssistantTurn> = emptyList(),
)

/** The subject of the last answer, carried forward so short follow-ups keep their meaning. */
data class AssistantFocus(
    val subscriptionId: SubscriptionId? = null,
    val category: Category? = null,
)

/** One earlier turn of the conversation. */
data class AssistantTurn(val fromUser: Boolean, val text: String)

/** The window a "what did I pay" question asks about. */
enum class SpendPeriod { THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL_TIME }

/**
 * What the user asked, once understood. Typed so answers are computed, not generated.
 */
sealed interface AssistantIntent {
    data object TotalSpend : AssistantIntent
    data object YearlySpend : AssistantIntent
    data class SpendInCategory(val category: Category) : AssistantIntent
    data object CategoryBreakdown : AssistantIntent
    data class TopSubscriptions(val count: Int) : AssistantIntent
    data class CheapestSubscriptions(val count: Int) : AssistantIntent
    data class SubscriptionDetail(val subscriptionId: SubscriptionId) : AssistantIntent
    data object CancelCandidates : AssistantIntent
    data object NextCharge : AssistantIntent
    data object UpcomingThisMonth : AssistantIntent
    data class UpcomingInDays(val days: Int) : AssistantIntent
    data class RecordedSpend(val period: SpendPeriod) : AssistantIntent
    data object PriceIncreases : AssistantIntent
    data object CountByCategory : AssistantIntent
    data object InactiveSubscriptions : AssistantIntent
    data object Capabilities : AssistantIntent
    data object Greeting : AssistantIntent
    data object Unknown : AssistantIntent
}

/**
 * A structured answer: a headline amount when there is one, a lead sentence, an optional list
 * of subscriptions with a per-item amount, and follow-up questions worth asking next. Wording is
 * localized by the UI from [kind]; the assistant only supplies facts.
 */
data class AssistantAnswer(
    val kind: AnswerKind,
    val amount: Money? = null,
    /** A second figure the wording needs, e.g. the yearly equivalent of [amount]. */
    val secondaryAmount: Money? = null,
    val items: List<AnswerItem> = emptyList(),
    val count: Int = 0,
    val category: Category? = null,
    val date: LocalDate? = null,
    /** The window a [AnswerKind.RECORDED_SPEND] answer covers. */
    val period: SpendPeriod? = null,
    /** The window a [AnswerKind.UPCOMING_IN_DAYS] answer covers. */
    val days: Int = 0,
    /** True when some subscriptions were left out because they are in another currency. */
    val excludedForeignCurrency: Int = 0,
    /** Questions worth asking next, offered as chips. */
    val suggestions: List<AssistantSuggestion> = emptyList(),
    /** What this answer is about; fed back into the next [AssistantContext]. */
    val focus: AssistantFocus? = null,
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
    /** Set when the row stands for a category rather than for the subscription itself. */
    val category: Category? = null,
)

enum class AnswerKind {
    TOTAL_SPEND,
    YEARLY_SPEND,
    SPEND_IN_CATEGORY,
    NOTHING_IN_CATEGORY,
    CATEGORY_BREAKDOWN,
    TOP_SUBSCRIPTIONS,
    CHEAPEST_SUBSCRIPTIONS,
    SUBSCRIPTION_DETAIL,
    CANCEL_CANDIDATES,
    NO_CANCEL_CANDIDATES,
    NO_USAGE_DATA,
    NEXT_CHARGE,
    NO_UPCOMING,
    UPCOMING_THIS_MONTH,
    UPCOMING_IN_DAYS,
    RECORDED_SPEND,
    NO_RECORDED_SPEND,
    PRICE_INCREASES,
    NO_PRICE_INCREASES,
    COUNT_BY_CATEGORY,
    INACTIVE,
    NO_INACTIVE,
    CAPABILITIES,
    GREETING,
    NO_SUBSCRIPTIONS,
    UNKNOWN,

    /** A free-text answer from a hosted model; see [AssistantAnswer.text]. */
    TEXT,
}

/** A follow-up the UI offers as a chip; the chip's localized text becomes the next question. */
enum class AssistantSuggestion {
    TOTAL,
    YEARLY,
    TOP,
    CHEAPEST,
    CANCEL,
    NEXT,
    UPCOMING_WEEK,
    INCREASES,
    BREAKDOWN,
    RECORDED,
    INACTIVE,
    AI_CATEGORY,
    HELP,
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
