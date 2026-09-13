package com.subzero.core.domain.assistant

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import java.time.YearMonth
import javax.inject.Inject

/**
 * Understands a small set of question shapes and answers them from the data, on device.
 *
 * Parsing is keyword based and deliberately conservative: anything it does not recognise
 * becomes [AnswerKind.UNKNOWN] with the supported questions offered, rather than a guess.
 * Category words are matched in English; localized synonyms can be added per language.
 */
class LocalAssistant @Inject constructor(
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer =
        answer(parse(question), context)

    fun parse(question: String): AssistantIntent {
        val q = question.lowercase().trim()
        if (q.isBlank()) return AssistantIntent.Unknown
        val category = Category.entries.firstOrNull { c -> categoryWords.getValue(c).any { it in q } }
        return when {
            q.containsAny("cancel", "waste", "wasting", "rarely", "don't use", "not using", "drop", "save") -> AssistantIntent.CancelCandidates
            q.containsAny("increase", "went up", "go up", "gone up", "raised", "price change", "more expensive", "higher") -> AssistantIntent.PriceIncreases
            q.containsAny("next charge", "next payment", "when is", "when's", "charged next", "due next", "coming up next") -> AssistantIntent.NextCharge
            q.containsAny("this month", "coming up", "upcoming", "due this") -> AssistantIntent.UpcomingThisMonth
            q.containsAny("most expensive", "cost me the most", "costs me the most", "biggest", "largest", "top ") ->
                AssistantIntent.TopSubscriptions(count = numberIn(q) ?: DEFAULT_TOP)
            q.containsAny("how many") && category == null -> AssistantIntent.CountByCategory
            category != null && q.containsAny("how many") -> AssistantIntent.SpendInCategory(category)
            category != null -> AssistantIntent.SpendInCategory(category)
            q.containsAny("how much", "spend", "spending", "pay", "total", "per month", "per year", "a month", "a year", "monthly", "yearly", "annual") ->
                AssistantIntent.TotalSpend
            else -> AssistantIntent.Unknown
        }
    }

    fun answer(intent: AssistantIntent, context: AssistantContext): AssistantAnswer {
        val active = context.subscriptions.filter { it.isActive }
        if (active.isEmpty() && intent != AssistantIntent.Unknown) return AssistantAnswer(AnswerKind.NO_SUBSCRIPTIONS)
        val home = active.filter { it.price.currency == context.homeCurrency }
        val foreign = active.size - home.size
        return when (intent) {
            AssistantIntent.TotalSpend -> AssistantAnswer(
                kind = AnswerKind.TOTAL_SPEND,
                amount = home.sumMonthly(context),
                count = home.size,
                excludedForeignCurrency = foreign,
            )
            is AssistantIntent.SpendInCategory -> {
                val inCategory = home.filter { it.category == intent.category }
                if (inCategory.isEmpty()) {
                    AssistantAnswer(AnswerKind.NOTHING_IN_CATEGORY, category = intent.category)
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.SPEND_IN_CATEGORY,
                        amount = inCategory.sumMonthly(context),
                        items = inCategory.sortedByDescending { it.monthly().amountMinor }.map { AnswerItem(it, it.monthly()) },
                        count = inCategory.size,
                        category = intent.category,
                        excludedForeignCurrency = active.count { it.category == intent.category && it.price.currency != context.homeCurrency },
                    )
                }
            }
            is AssistantIntent.TopSubscriptions -> AssistantAnswer(
                kind = AnswerKind.TOP_SUBSCRIPTIONS,
                items = home.sortedByDescending { it.monthly().amountMinor }.take(intent.count).map { AnswerItem(it, it.monthly()) },
                count = home.size,
                excludedForeignCurrency = foreign,
            )
            AssistantIntent.CancelCandidates -> {
                val rarely = home.filter { it.usage == DeclaredUsage.RARELY }
                val unknown = home.count { it.usage == DeclaredUsage.UNKNOWN }
                when {
                    rarely.isNotEmpty() -> AssistantAnswer(
                        kind = AnswerKind.CANCEL_CANDIDATES,
                        amount = rarely.sumMonthly(context),
                        items = rarely.sortedByDescending { it.monthly().amountMinor }.map { AnswerItem(it, it.monthly()) },
                        count = rarely.size,
                    )
                    unknown == home.size -> AssistantAnswer(AnswerKind.NO_USAGE_DATA, count = unknown)
                    else -> AssistantAnswer(AnswerKind.NO_CANCEL_CANDIDATES, count = unknown)
                }
            }
            AssistantIntent.NextCharge -> {
                val next = getUpcomingPayments.next(active, from = context.today)
                if (next == null) {
                    AssistantAnswer(AnswerKind.NO_UPCOMING)
                } else {
                    AssistantAnswer(kind = AnswerKind.NEXT_CHARGE, amount = next.amount, items = listOf(next.toAnswerItem()), date = next.date)
                }
            }
            AssistantIntent.UpcomingThisMonth -> {
                val month = YearMonth.from(context.today)
                val payments = getUpcomingPayments(active, from = context.today, to = month.atEndOfMonth())
                if (payments.isEmpty()) {
                    AssistantAnswer(AnswerKind.NO_UPCOMING)
                } else {
                    val inHome = payments.filter { it.amount.currency == context.homeCurrency }
                    AssistantAnswer(
                        kind = AnswerKind.UPCOMING_THIS_MONTH,
                        amount = inHome.fold(Money.zero(context.homeCurrency)) { acc, p -> acc + p.amount },
                        items = payments.map { it.toAnswerItem() },
                        count = payments.size,
                        excludedForeignCurrency = payments.size - inHome.size,
                    )
                }
            }
            AssistantIntent.PriceIncreases -> {
                val byId = active.associateBy { it.id }
                val increases = context.priceChanges
                    .groupBy { it.subscriptionId }
                    .mapNotNull { (id, changes) ->
                        val subscription = byId[id] ?: return@mapNotNull null
                        val sorted = changes.sortedBy { it.effectiveFrom }
                        if (sorted.size < 2) return@mapNotNull null
                        val latest = sorted.last()
                        val previous = sorted[sorted.size - 2]
                        if (latest.price.currency != previous.price.currency || latest.price.amountMinor <= previous.price.amountMinor) return@mapNotNull null
                        AnswerItem(subscription, latest.price, previousAmount = previous.price, date = latest.effectiveFrom)
                    }
                    .sortedByDescending { it.date }
                if (increases.isEmpty()) AssistantAnswer(AnswerKind.NO_PRICE_INCREASES) else AssistantAnswer(AnswerKind.PRICE_INCREASES, items = increases, count = increases.size)
            }
            AssistantIntent.CountByCategory -> AssistantAnswer(
                kind = AnswerKind.COUNT_BY_CATEGORY,
                items = active.groupBy { it.category }.entries
                    .sortedByDescending { it.value.size }
                    .flatMap { (_, items) -> items.take(1).map { AnswerItem(it, it.monthly()) } },
                count = active.size,
            )
            AssistantIntent.Unknown -> AssistantAnswer(AnswerKind.UNKNOWN)
        }
    }

    private fun Subscription.monthly(): Money = BillingSchedule.monthlyEquivalent(price, billingCycle)

    private fun List<Subscription>.sumMonthly(context: AssistantContext): Money =
        fold(Money.zero(context.homeCurrency)) { acc, s -> acc + s.monthly() }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { it in this }

    private fun numberIn(q: String): Int? = Regex("\\b(\\d{1,2})\\b").find(q)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, MAX_TOP)

    companion object {
        const val DEFAULT_TOP = 3
        const val MAX_TOP = 10

        /** English words that identify a category in a question. */
        val categoryWords: Map<Category, List<String>> = mapOf(
            Category.ENTERTAINMENT to listOf("entertainment", "streaming", "video", "music", "tv"),
            Category.AI to listOf(" ai", "ai ", "artificial", "chatgpt", "claude", "gemini", "copilot"),
            Category.CLOUD to listOf("cloud", "storage", "backup"),
            Category.SOFTWARE to listOf("software", "apps", "tools"),
            Category.FITNESS to listOf("fitness", "gym", "health", "workout"),
            Category.EDUCATION to listOf("education", "learning", "courses", "course"),
            Category.PRODUCTIVITY to listOf("productivity", "notes", "tasks"),
            Category.GAMING to listOf("gaming", "games", "game"),
            Category.OTHER to listOf("other"),
        )
    }
}
