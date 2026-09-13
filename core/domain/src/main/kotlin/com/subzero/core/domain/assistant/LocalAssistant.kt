package com.subzero.core.domain.assistant

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Understands the questions this app can answer and answers them from the data, on device.
 *
 * Parsing is keyword based and deliberately conservative: anything it does not recognise
 * becomes [AnswerKind.UNKNOWN] with the supported questions offered, rather than a guess.
 * English and Arabic keywords are recognised; Arabic text is normalized first (diacritics
 * removed, alef and ya variants folded, Arabic-Indic digits converted) so spelling variants match.
 *
 * Two things make follow-ups work: a question naming a service is answered about that service,
 * and a short question with a pronoun ("when is it charged?", "كم سعره؟") is answered about
 * whatever the previous answer was about, carried in [AssistantContext.focus].
 */
class LocalAssistant @Inject constructor(
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer =
        answer(parse(question, context), context)

    fun parse(question: String, context: AssistantContext? = null): AssistantIntent {
        val q = normalize(question)
        if (q.isBlank()) return AssistantIntent.Unknown

        val named = q.namedSubscription(context?.subscriptions.orEmpty())
        val category = Category.entries.firstOrNull { c -> categoryWords.getValue(c).any { normalize(it) in q } }

        return when {
            q.isGreeting() -> AssistantIntent.Greeting
            q.containsAny(
                "what can you do", "what can i ask", "how do i use", "help me", "your help",
                "ماذا تستطيع", "ماذا يمكنك", "ما الذي تستطيع", "كيف استخدم", "ساعدني", "المساعده",
            ) -> AssistantIntent.Capabilities
            named != null -> AssistantIntent.SubscriptionDetail(named.id)
            q.isAboutFocus(context) -> AssistantIntent.SubscriptionDetail(checkNotNull(context?.focus?.subscriptionId))
            q.containsAny(
                "did i spend", "have i spent", "did i pay", "have i paid", "actually spent", "actually paid", "spent", "paid so far",
                "كم صرفت", "كم دفعت", "كم انفقت", "فعليا", "الشهر الماضي", "الشهر الفائت", "العام الماضي",
            ) -> AssistantIntent.RecordedSpend(periodIn(q))
            q.containsAny(
                "increase", "went up", "go up", "gone up", "raised", "price change", "more expensive", "higher",
                "ارتفع", "ارتفعت", "زاد", "زادت", "زياده", "رفع السعر", "تغير السعر",
            ) -> AssistantIntent.PriceIncreases
            q.containsAny(
                "paused", "canceled", "cancelled", "inactive", "stopped",
                "متوقف", "الملغاه", "الملغيه", "غير نشط", "اوقفتها",
            ) -> AssistantIntent.InactiveSubscriptions
            q.containsAny(
                // "الغ" covers إلغاء / إلغاؤه / ألغي once hamza and waw variants are folded.
                "cancel", "waste", "wasting", "rarely", "don't use", "not using", "drop", "save",
                "الغ", "اوقف", "اهدر", "نادرا", "لا استخدم", "توفير", "اوفر", "استغني",
            ) -> AssistantIntent.CancelCandidates
            q.containsAny(
                "next charge", "next payment", "when is", "when's", "charged next", "due next", "coming up next",
                "متى", "الخصم القادم", "الدفعه القادمه", "الدفعه التاليه",
            ) -> AssistantIntent.NextCharge
            q.containsAny("next week", "this week", "next 7", "coming days", "الاسبوع القادم", "هذا الاسبوع", "الايام القادمه", "الايام المقبله") ->
                AssistantIntent.UpcomingInDays(days = numberIn(q, max = MAX_DAYS) ?: WEEK_DAYS)
            q.containsAny("next", "القادمه", "المقبله") && q.containsAny("days", "يوم", "ايام") ->
                AssistantIntent.UpcomingInDays(days = numberIn(q, max = MAX_DAYS) ?: WEEK_DAYS)
            q.containsAny("this month", "coming up", "upcoming", "due this", "هذا الشهر", "الشهر الحالي") ->
                AssistantIntent.UpcomingThisMonth
            q.containsAny(
                "cheapest", "least expensive", "lowest", "smallest",
                "الارخص", "ارخص", "اقل تكلفه", "الاقل تكلفه",
            ) -> AssistantIntent.CheapestSubscriptions(count = numberIn(q) ?: DEFAULT_TOP)
            q.containsAny(
                "most expensive", "cost me the most", "costs me the most", "biggest", "largest", "top ",
                "اغلى", "الاكبر", "اكثر تكلفه", "اعلى تكلفه",
            ) -> AssistantIntent.TopSubscriptions(count = numberIn(q) ?: DEFAULT_TOP)
            q.containsAny(
                "breakdown", "by category", "per category", "where does my money", "where is my money", "split",
                "حسب الفئه", "على الفئات", "اين تذهب", "توزيع",
            ) -> AssistantIntent.CategoryBreakdown
            q.containsAny(*countWords) && category == null -> AssistantIntent.CountByCategory
            category != null && q.containsAny(*countWords) -> AssistantIntent.SpendInCategory(category)
            category != null -> AssistantIntent.SpendInCategory(category)
            q.containsAny(*yearWords) && q.containsAny(*spendWords) -> AssistantIntent.YearlySpend
            q.containsAny(*spendWords) -> AssistantIntent.TotalSpend
            else -> AssistantIntent.Unknown
        }
    }

    /**
     * Folds a question into a form keywords can be matched against: lowercase, no Arabic
     * diacritics or tatweel, alef/ya/ta-marbuta variants unified, Arabic-Indic digits as ASCII.
     */
    internal fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text.lowercase()) {
            when {
                char in 'ً'..'ْ' || char == 'ـ' -> Unit // diacritics and tatweel
                char in '٠'..'٩' -> builder.append('0' + (char - '٠')) // Arabic-Indic digits
                char in '۰'..'۹' -> builder.append('0' + (char - '۰')) // extended digits
                else -> builder.append(arabicFolding[char] ?: char)
            }
        }
        return builder.toString().trim()
    }

    @Suppress("CyclomaticComplexMethod")
    fun answer(intent: AssistantIntent, context: AssistantContext): AssistantAnswer {
        val active = context.subscriptions.filter { it.isActive }
        if (active.isEmpty() && intent.needsActiveSubscriptions) {
            return AssistantAnswer(AnswerKind.NO_SUBSCRIPTIONS, suggestions = listOf(AssistantSuggestion.HELP))
        }
        val home = active.filter { it.price.currency == context.homeCurrency }
        val foreign = active.size - home.size
        return when (intent) {
            AssistantIntent.TotalSpend -> AssistantAnswer(
                kind = AnswerKind.TOTAL_SPEND,
                amount = home.sumMonthly(context),
                secondaryAmount = home.sumYearly(context),
                count = home.size,
                excludedForeignCurrency = foreign,
                suggestions = listOf(AssistantSuggestion.YEARLY, AssistantSuggestion.BREAKDOWN, AssistantSuggestion.TOP),
            )
            AssistantIntent.YearlySpend -> AssistantAnswer(
                kind = AnswerKind.YEARLY_SPEND,
                amount = home.sumYearly(context),
                secondaryAmount = home.sumMonthly(context),
                count = home.size,
                excludedForeignCurrency = foreign,
                suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.CANCEL, AssistantSuggestion.BREAKDOWN),
            )
            is AssistantIntent.SpendInCategory -> {
                val inCategory = home.filter { it.category == intent.category }
                if (inCategory.isEmpty()) {
                    AssistantAnswer(
                        kind = AnswerKind.NOTHING_IN_CATEGORY,
                        category = intent.category,
                        suggestions = listOf(AssistantSuggestion.BREAKDOWN, AssistantSuggestion.TOTAL),
                        focus = AssistantFocus(category = intent.category),
                    )
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.SPEND_IN_CATEGORY,
                        amount = inCategory.sumMonthly(context),
                        secondaryAmount = inCategory.sumYearly(context),
                        items = inCategory.byMonthlyDescending(),
                        count = inCategory.size,
                        category = intent.category,
                        excludedForeignCurrency = active.count { it.category == intent.category && it.price.currency != context.homeCurrency },
                        suggestions = listOf(AssistantSuggestion.BREAKDOWN, AssistantSuggestion.TOTAL, AssistantSuggestion.CANCEL),
                        focus = AssistantFocus(category = intent.category),
                    )
                }
            }
            AssistantIntent.CategoryBreakdown -> {
                val rows = home.groupBy { it.category }
                    .map { (category, subscriptions) ->
                        AnswerItem(
                            subscription = subscriptions.maxBy { it.monthly().amountMinor },
                            amount = subscriptions.sumMonthly(context),
                            category = category,
                        )
                    }
                    .sortedByDescending { it.amount.amountMinor }
                AssistantAnswer(
                    kind = AnswerKind.CATEGORY_BREAKDOWN,
                    amount = home.sumMonthly(context),
                    items = rows,
                    count = rows.size,
                    excludedForeignCurrency = foreign,
                    suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.YEARLY, AssistantSuggestion.CANCEL),
                )
            }
            is AssistantIntent.TopSubscriptions -> AssistantAnswer(
                kind = AnswerKind.TOP_SUBSCRIPTIONS,
                items = home.byMonthlyDescending().take(intent.count),
                count = home.size,
                excludedForeignCurrency = foreign,
                suggestions = listOf(AssistantSuggestion.CHEAPEST, AssistantSuggestion.CANCEL, AssistantSuggestion.INCREASES),
            )
            is AssistantIntent.CheapestSubscriptions -> AssistantAnswer(
                kind = AnswerKind.CHEAPEST_SUBSCRIPTIONS,
                items = home.byMonthlyDescending().reversed().take(intent.count),
                count = home.size,
                excludedForeignCurrency = foreign,
                suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.TOTAL),
            )
            is AssistantIntent.SubscriptionDetail -> {
                val subscription = context.subscriptions.firstOrNull { it.id == intent.subscriptionId }
                    ?: return AssistantAnswer(AnswerKind.UNKNOWN, suggestions = defaultSuggestions)
                AssistantAnswer(
                    kind = AnswerKind.SUBSCRIPTION_DETAIL,
                    amount = subscription.monthly(),
                    secondaryAmount = BillingSchedule.yearlyEquivalent(subscription.price, subscription.billingCycle),
                    items = listOf(AnswerItem(subscription, subscription.price)),
                    category = subscription.category,
                    date = subscription.nextBillingDate.takeIf { subscription.isActive },
                    suggestions = listOf(AssistantSuggestion.NEXT, AssistantSuggestion.TOP, AssistantSuggestion.INCREASES),
                    focus = AssistantFocus(subscriptionId = subscription.id, category = subscription.category),
                )
            }
            AssistantIntent.CancelCandidates -> {
                val rarely = home.filter { it.usage == DeclaredUsage.RARELY }
                val unknown = home.count { it.usage == DeclaredUsage.UNKNOWN }
                when {
                    rarely.isNotEmpty() -> AssistantAnswer(
                        kind = AnswerKind.CANCEL_CANDIDATES,
                        amount = rarely.sumMonthly(context),
                        secondaryAmount = rarely.sumYearly(context),
                        items = rarely.byMonthlyDescending(),
                        count = rarely.size,
                        suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.TOTAL),
                    )
                    unknown == home.size -> AssistantAnswer(
                        kind = AnswerKind.NO_USAGE_DATA,
                        count = unknown,
                        suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.TOTAL),
                    )
                    else -> AssistantAnswer(
                        kind = AnswerKind.NO_CANCEL_CANDIDATES,
                        count = unknown,
                        suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.BREAKDOWN),
                    )
                }
            }
            AssistantIntent.NextCharge -> {
                val next = getUpcomingPayments.next(active, from = context.today)
                if (next == null) {
                    AssistantAnswer(AnswerKind.NO_UPCOMING, suggestions = listOf(AssistantSuggestion.INACTIVE, AssistantSuggestion.TOTAL))
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.NEXT_CHARGE,
                        amount = next.amount,
                        items = listOf(next.toAnswerItem()),
                        date = next.date,
                        suggestions = listOf(AssistantSuggestion.UPCOMING_WEEK, AssistantSuggestion.TOTAL),
                        focus = AssistantFocus(subscriptionId = next.subscription.id, category = next.subscription.category),
                    )
                }
            }
            AssistantIntent.UpcomingThisMonth -> {
                val month = YearMonth.from(context.today)
                upcomingAnswer(
                    context = context,
                    active = active,
                    to = month.atEndOfMonth(),
                    kind = AnswerKind.UPCOMING_THIS_MONTH,
                )
            }
            is AssistantIntent.UpcomingInDays -> upcomingAnswer(
                context = context,
                active = active,
                to = context.today.plusDays(intent.days.toLong()),
                kind = AnswerKind.UPCOMING_IN_DAYS,
                days = intent.days,
            )
            is AssistantIntent.RecordedSpend -> {
                val range = intent.period.range(context.today)
                val inPeriod = context.payments.filter { it.paidOn in range }
                val inHome = inPeriod.filter { it.amount.currency == context.homeCurrency }
                if (inHome.isEmpty()) {
                    AssistantAnswer(
                        kind = AnswerKind.NO_RECORDED_SPEND,
                        period = intent.period,
                        suggestions = listOf(AssistantSuggestion.TOTAL, AssistantSuggestion.UPCOMING_WEEK),
                    )
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.RECORDED_SPEND,
                        amount = inHome.fold(Money.zero(context.homeCurrency)) { acc, payment -> acc + payment.amount },
                        count = inHome.size,
                        period = intent.period,
                        date = range.start,
                        excludedForeignCurrency = inPeriod.size - inHome.size,
                        suggestions = listOf(AssistantSuggestion.TOTAL, AssistantSuggestion.TOP, AssistantSuggestion.YEARLY),
                    )
                }
            }
            AssistantIntent.PriceIncreases -> {
                val increases = priceIncreases(context, active)
                if (increases.isEmpty()) {
                    AssistantAnswer(AnswerKind.NO_PRICE_INCREASES, suggestions = listOf(AssistantSuggestion.TOP, AssistantSuggestion.TOTAL))
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.PRICE_INCREASES,
                        items = increases,
                        count = increases.size,
                        suggestions = listOf(AssistantSuggestion.CANCEL, AssistantSuggestion.TOTAL),
                    )
                }
            }
            AssistantIntent.CountByCategory -> AssistantAnswer(
                kind = AnswerKind.COUNT_BY_CATEGORY,
                items = active.groupBy { it.category }.entries
                    .sortedByDescending { it.value.size }
                    .flatMap { (_, items) -> items.take(1).map { AnswerItem(it, it.monthly()) } },
                count = active.size,
                suggestions = listOf(AssistantSuggestion.BREAKDOWN, AssistantSuggestion.TOTAL),
            )
            AssistantIntent.InactiveSubscriptions -> {
                val inactive = context.subscriptions.filterNot { it.isActive }
                if (inactive.isEmpty()) {
                    AssistantAnswer(AnswerKind.NO_INACTIVE, suggestions = listOf(AssistantSuggestion.TOTAL, AssistantSuggestion.CANCEL))
                } else {
                    AssistantAnswer(
                        kind = AnswerKind.INACTIVE,
                        items = inactive.byMonthlyDescending(),
                        count = inactive.size,
                        suggestions = listOf(AssistantSuggestion.TOTAL, AssistantSuggestion.CANCEL),
                    )
                }
            }
            AssistantIntent.Capabilities -> AssistantAnswer(AnswerKind.CAPABILITIES, suggestions = defaultSuggestions)
            AssistantIntent.Greeting -> AssistantAnswer(AnswerKind.GREETING, suggestions = defaultSuggestions)
            AssistantIntent.Unknown -> AssistantAnswer(AnswerKind.UNKNOWN, suggestions = defaultSuggestions)
        }
    }

    private fun upcomingAnswer(
        context: AssistantContext,
        active: List<Subscription>,
        to: LocalDate,
        kind: AnswerKind,
        days: Int = 0,
    ): AssistantAnswer {
        val payments = getUpcomingPayments(active, from = context.today, to = to)
        if (payments.isEmpty()) {
            return AssistantAnswer(
                kind = AnswerKind.NO_UPCOMING,
                days = days,
                suggestions = listOf(AssistantSuggestion.TOTAL, AssistantSuggestion.NEXT),
            )
        }
        val inHome = payments.filter { it.amount.currency == context.homeCurrency }
        return AssistantAnswer(
            kind = kind,
            amount = inHome.fold(Money.zero(context.homeCurrency)) { acc, payment -> acc + payment.amount },
            items = payments.map { it.toAnswerItem() },
            count = payments.size,
            days = days,
            excludedForeignCurrency = payments.size - inHome.size,
            suggestions = listOf(AssistantSuggestion.NEXT, AssistantSuggestion.TOTAL, AssistantSuggestion.RECORDED),
        )
    }

    private fun priceIncreases(context: AssistantContext, active: List<Subscription>): List<AnswerItem> {
        val byId = active.associateBy { it.id }
        return context.priceChanges
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
    }

    private fun Subscription.monthly(): Money = BillingSchedule.monthlyEquivalent(price, billingCycle)

    private fun List<Subscription>.byMonthlyDescending(): List<AnswerItem> =
        sortedByDescending { it.monthly().amountMinor }.map { AnswerItem(it, it.monthly()) }

    private fun List<Subscription>.sumMonthly(context: AssistantContext): Money =
        fold(Money.zero(context.homeCurrency)) { acc, s -> acc + s.monthly() }

    private fun List<Subscription>.sumYearly(context: AssistantContext): Money =
        fold(Money.zero(context.homeCurrency)) { acc, s -> acc + BillingSchedule.yearlyEquivalent(s.price, s.billingCycle) }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { normalize(it) in this }

    /**
     * The subscription this question is about, if it names one. Matching is on whole words so
     * "Duo" does not match "duolingo", and names that are themselves question words (someone
     * really did call a subscription "Paused") are left to the keyword rules instead.
     */
    private fun String.namedSubscription(subscriptions: List<Subscription>): Subscription? =
        subscriptions.firstOrNull { subscription ->
            val name = normalize(subscription.name)
            name.length >= MIN_NAME_MATCH && name !in reservedNames && containsWord(name)
        }

    private fun String.containsWord(word: String): Boolean {
        var index = indexOf(word)
        while (index >= 0) {
            val startsCleanly = index == 0 || !this[index - 1].isLetterOrDigit()
            val end = index + word.length
            val endsCleanly = end == length || !this[end].isLetterOrDigit()
            if (startsCleanly && endsCleanly) return true
            index = indexOf(word, index + 1)
        }
        return false
    }

    private fun String.isGreeting(): Boolean = greetings.any { this == it || startsWith("$it ") || startsWith("$it,") }

    /** A short question with a pronoun and no other subject belongs to the previous answer. */
    private fun String.isAboutFocus(context: AssistantContext?): Boolean {
        if (context?.focus?.subscriptionId == null) return false
        if (split(" ").size > MAX_FOLLOW_UP_WORDS) return false
        return containsAny("it", "its", "this one", "that one", "سعره", "تكلفته", "موعده", "عنه", "له", "هذا الاشتراك")
    }

    private fun periodIn(q: String): SpendPeriod = when {
        q.containsAny("last month", "الشهر الماضي", "الشهر الفائت") -> SpendPeriod.LAST_MONTH
        q.containsAny("this year", "هذا العام", "هذه السنه", "السنه الحاليه") -> SpendPeriod.THIS_YEAR
        q.containsAny("all time", "ever", "in total", "الاجمالي", "منذ البدايه") -> SpendPeriod.ALL_TIME
        else -> SpendPeriod.THIS_MONTH
    }

    private fun numberIn(q: String, max: Int = MAX_TOP): Int? =
        Regex("\\b(\\d{1,3})\\b").find(q)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, max)

    /** Intents that only make sense once something is being tracked. */
    private val AssistantIntent.needsActiveSubscriptions: Boolean
        get() = when (this) {
            AssistantIntent.Unknown,
            AssistantIntent.Capabilities,
            AssistantIntent.Greeting,
            AssistantIntent.InactiveSubscriptions,
            is AssistantIntent.SubscriptionDetail,
            is AssistantIntent.RecordedSpend,
            -> false
            else -> true
        }

    private fun SpendPeriod.range(today: LocalDate): ClosedRange<LocalDate> = when (this) {
        SpendPeriod.THIS_MONTH -> YearMonth.from(today).atDay(1)..today
        SpendPeriod.LAST_MONTH -> YearMonth.from(today).minusMonths(1).let { it.atDay(1)..it.atEndOfMonth() }
        SpendPeriod.THIS_YEAR -> today.withDayOfYear(1)..today
        SpendPeriod.ALL_TIME -> LocalDate.MIN..today
    }

    companion object {
        const val DEFAULT_TOP = 3
        const val MAX_TOP = 10
        const val WEEK_DAYS = 7
        const val MAX_DAYS = 90

        /** Names shorter than this match too much other text to be treated as a subject. */
        private const val MIN_NAME_MATCH = 3

        /** Names that are also question words; the keyword rules own these. */
        private val reservedNames = setOf(
            "paused", "canceled", "cancelled", "inactive", "stopped", "cancel", "next", "upcoming",
            "total", "cheapest", "help", "monthly", "yearly",
            "متوقف", "الملغاه", "القادمه", "الاجمالي",
        )

        /** Above this length a question is standing on its own, not following up. */
        private const val MAX_FOLLOW_UP_WORDS = 6

        /** Offered when the assistant has nothing better to suggest. */
        private val defaultSuggestions = listOf(
            AssistantSuggestion.TOTAL,
            AssistantSuggestion.TOP,
            AssistantSuggestion.CANCEL,
            AssistantSuggestion.NEXT,
            AssistantSuggestion.BREAKDOWN,
            AssistantSuggestion.RECORDED,
        )

        /** Words that identify a category in a question, in English and Arabic. */
        val categoryWords: Map<Category, List<String>> = mapOf(
            Category.ENTERTAINMENT to listOf("entertainment", "streaming", "video", "music", "tv", "ترفيه", "افلام", "موسيقى", "تلفزيون", "مسلسلات"),
            Category.AI to listOf(" ai", "ai ", "artificial", "chatgpt", "claude", "gemini", "copilot", "ذكاء", "اصطناعي", "كلود", "جيميني"),
            Category.CLOUD to listOf("cloud", "storage", "backup", "سحاب", "سحابي", "تخزين", "نسخ احتياطي"),
            Category.SOFTWARE to listOf("software", "apps", "tools", "برمجيات", "برامج", "ادوات"),
            Category.FITNESS to listOf("fitness", "gym", "health", "workout", "لياقه", "رياضه", "صحه", "نادي"),
            Category.EDUCATION to listOf("education", "learning", "courses", "course", "تعليم", "دورات", "دوره", "تعلم"),
            Category.PRODUCTIVITY to listOf("productivity", "notes", "tasks", "انتاجيه", "ملاحظات", "مهام"),
            Category.GAMING to listOf("gaming", "games", "game", "العاب", "لعبه"),
            Category.OTHER to listOf("other", "اخرى"),
        )

        /** "How many" in both languages, used twice in [parse]. */
        private val countWords = arrayOf("how many", "كم عدد", "عدد الاشتراكات")

        private val yearWords = arrayOf("per year", "a year", "yearly", "annual", "annually", "سنويا", "في السنه", "بالسنه", "سنويه")

        private val spendWords = arrayOf(
            "how much", "spend", "spending", "pay", "total", "per month", "a month", "monthly", "cost",
            "كم ادفع", "كم انفق", "كم اصرف", "اجمالي", "المجموع", "شهريا", "في الشهر", "التكلفه",
        )

        private val greetings = listOf("hi", "hello", "hey", "yo", "مرحبا", "اهلا", "السلام عليكم", "هلا", "صباح الخير", "مساء الخير")

        /** Arabic letters folded to one spelling before matching. */
        private val arabicFolding = mapOf(
            'أ' to 'ا', 'إ' to 'ا', 'آ' to 'ا', 'ٱ' to 'ا',
            'ة' to 'ه', 'ى' to 'ي', 'ئ' to 'ي', 'ؤ' to 'و',
        )
    }
}
