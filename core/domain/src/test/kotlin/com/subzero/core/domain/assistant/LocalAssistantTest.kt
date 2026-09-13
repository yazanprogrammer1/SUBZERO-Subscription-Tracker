package com.subzero.core.domain.assistant

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.eur
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LocalAssistantTest {

    private val today = date("2026-09-12")
    private val assistant = LocalAssistant(GetUpcomingPaymentsUseCase(fixedClock(today)))

    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"), usage = DeclaredUsage.DAILY)
    private val chatgpt = subscription(id = "c", name = "ChatGPT", price = usd(2000), category = Category.AI, anchorDate = date("2026-09-20"))
    private val claude = subscription(id = "cl", name = "Claude", price = usd(1500), category = Category.AI, anchorDate = date("2026-10-02"), usage = DeclaredUsage.RARELY)
    private val adobe = subscription(id = "a", name = "Adobe", price = usd(59988), billingCycle = BillingCycle.Yearly, category = Category.SOFTWARE, anchorDate = date("2026-11-01"))
    private val deezer = subscription(id = "d", name = "Deezer", price = eur(1099), anchorDate = date("2026-09-14"))
    private val paused = subscription(id = "p", name = "Paused", price = usd(9999), status = SubscriptionStatus.PAUSED)

    private fun context(vararg subs: com.subzero.core.domain.model.Subscription, prices: List<com.subzero.core.domain.model.PriceChange> = emptyList()) =
        AssistantContext(subscriptions = subs.toList(), priceChanges = prices, homeCurrency = CurrencyCode.USD, today = today)

    @Test
    fun `parses the questions from the specification`() {
        assertThat(assistant.parse("How much am I spending on AI?")).isEqualTo(AssistantIntent.SpendInCategory(Category.AI))
        assertThat(assistant.parse("What subscriptions cost me the most?")).isEqualTo(AssistantIntent.TopSubscriptions(3))
        assertThat(assistant.parse("What can I cancel?")).isEqualTo(AssistantIntent.CancelCandidates)
        assertThat(assistant.parse("How much do I pay per month?")).isEqualTo(AssistantIntent.TotalSpend)
        assertThat(assistant.parse("When is my next charge?")).isEqualTo(AssistantIntent.NextCharge)
        assertThat(assistant.parse("What is coming up this month?")).isEqualTo(AssistantIntent.UpcomingThisMonth)
        assertThat(assistant.parse("Which prices went up?")).isEqualTo(AssistantIntent.PriceIncreases)
        assertThat(assistant.parse("How many subscriptions do I have?")).isEqualTo(AssistantIntent.CountByCategory)
        assertThat(assistant.parse("top 5 subscriptions")).isEqualTo(AssistantIntent.TopSubscriptions(5))
        assertThat(assistant.parse("how much for streaming")).isEqualTo(AssistantIntent.SpendInCategory(Category.ENTERTAINMENT))
        assertThat(assistant.parse("tell me a joke")).isEqualTo(AssistantIntent.Unknown)
        assertThat(assistant.parse("")).isEqualTo(AssistantIntent.Unknown)
    }

    @Test
    fun `answers spend in a category with the items and excludes foreign currency`() = runTest {
        val answer = assistant.ask("How much am I spending on AI?", context(netflix, chatgpt, claude, adobe))
        assertThat(answer.kind).isEqualTo(AnswerKind.SPEND_IN_CATEGORY)
        assertThat(answer.amount).isEqualTo(usd(3500))
        assertThat(answer.items.map { it.subscription.name }).containsExactly("ChatGPT", "Claude").inOrder()
        assertThat(answer.category).isEqualTo(Category.AI)

        val streaming = assistant.ask("streaming", context(netflix, deezer))
        assertThat(streaming.amount).isEqualTo(usd(1549))
        assertThat(streaming.excludedForeignCurrency).isEqualTo(1)
    }

    @Test
    fun `answers total and top subscriptions from active home currency ones`() = runTest {
        val total = assistant.ask("how much do I spend", context(netflix, chatgpt, adobe, deezer, paused))
        assertThat(total.kind).isEqualTo(AnswerKind.TOTAL_SPEND)
        assertThat(total.amount).isEqualTo(usd(1549 + 2000 + 4999))
        assertThat(total.count).isEqualTo(3)
        assertThat(total.excludedForeignCurrency).isEqualTo(1)

        val top = assistant.ask("What costs me the most?", context(netflix, chatgpt, adobe, claude))
        assertThat(top.items.map { it.subscription.name }).containsExactly("Adobe", "ChatGPT", "Netflix").inOrder()
    }

    @Test
    fun `cancel candidates come only from declared usage and never guess`() = runTest {
        val withData = assistant.ask("What can I cancel?", context(netflix, chatgpt, claude))
        assertThat(withData.kind).isEqualTo(AnswerKind.CANCEL_CANDIDATES)
        assertThat(withData.items.single().subscription.name).isEqualTo("Claude")
        assertThat(withData.amount).isEqualTo(usd(1500))

        val noRarely = assistant.ask("What can I cancel?", context(netflix, chatgpt))
        assertThat(noRarely.kind).isEqualTo(AnswerKind.NO_CANCEL_CANDIDATES)
        assertThat(noRarely.count).isEqualTo(1)

        val noUsage = assistant.ask("What can I cancel?", context(chatgpt, adobe))
        assertThat(noUsage.kind).isEqualTo(AnswerKind.NO_USAGE_DATA)
    }

    @Test
    fun `next charge and upcoming this month`() = runTest {
        val next = assistant.ask("When is my next charge?", context(netflix, chatgpt, deezer))
        assertThat(next.kind).isEqualTo(AnswerKind.NEXT_CHARGE)
        assertThat(next.items.single().subscription.name).isEqualTo("Deezer")
        assertThat(next.date).isEqualTo(date("2026-09-14"))

        val month = assistant.ask("what is coming up this month", context(netflix, chatgpt, claude, deezer))
        assertThat(month.kind).isEqualTo(AnswerKind.UPCOMING_THIS_MONTH)
        assertThat(month.items.map { it.subscription.name }).containsExactly("Deezer", "Netflix", "ChatGPT").inOrder()
        assertThat(month.amount).isEqualTo(usd(1549 + 2000))
        assertThat(month.excludedForeignCurrency).isEqualTo(1)
    }

    @Test
    fun `price increases list the change`() = runTest {
        val prices = listOf(
            priceChange(subscriptionId = "n", price = usd(1399), effectiveFrom = date("2026-01-16")),
            priceChange(subscriptionId = "n", price = usd(1549), effectiveFrom = date("2026-08-20")),
            priceChange(subscriptionId = "c", price = usd(2000), effectiveFrom = date("2026-01-01")),
        )
        val answer = assistant.ask("did any price go up?", context(netflix, chatgpt, prices = prices))
        assertThat(answer.kind).isEqualTo(AnswerKind.PRICE_INCREASES)
        val item = answer.items.single()
        assertThat(item.subscription.name).isEqualTo("Netflix")
        assertThat(item.previousAmount).isEqualTo(usd(1399))
        assertThat(item.amount).isEqualTo(usd(1549))

        assertThat(assistant.ask("price increase", context(chatgpt)).kind).isEqualTo(AnswerKind.NO_PRICE_INCREASES)
    }

    @Test
    fun `parses the same questions in Arabic`() {
        assertThat(assistant.parse("كم أنفق على الذكاء الاصطناعي؟")).isEqualTo(AssistantIntent.SpendInCategory(Category.AI))
        assertThat(assistant.parse("ما هو الاشتراك الأغلى؟")).isEqualTo(AssistantIntent.TopSubscriptions(3))
        assertThat(assistant.parse("ما الذي يمكنني إلغاؤه؟")).isEqualTo(AssistantIntent.CancelCandidates)
        assertThat(assistant.parse("كم أدفع شهريًا؟")).isEqualTo(AssistantIntent.TotalSpend)
        assertThat(assistant.parse("متى الخصم القادم؟")).isEqualTo(AssistantIntent.NextCharge)
        assertThat(assistant.parse("أي الأسعار ارتفعت؟")).isEqualTo(AssistantIntent.PriceIncreases)
        assertThat(assistant.parse("كم عدد اشتراكاتي؟")).isEqualTo(AssistantIntent.CountByCategory)
        assertThat(assistant.parse("احكِ لي نكتة")).isEqualTo(AssistantIntent.Unknown)
    }

    @Test
    fun `normalizing folds diacritics, letter variants and Arabic-Indic digits`() {
        assertThat(assistant.normalize("أَغْلَى")).isEqualTo("اغلي")
        assertThat(assistant.normalize("الدفعة")).isEqualTo("الدفعه")
        assertThat(assistant.normalize("أعلى ٥ اشتراكات")).isEqualTo("اعلي 5 اشتراكات")
    }

    @Test
    fun `answers an Arabic question from the data`() = runTest {
        val answer = assistant.ask("كم أنفق على الذكاء الاصطناعي؟", context(netflix, chatgpt, claude))
        assertThat(answer.kind).isEqualTo(AnswerKind.SPEND_IN_CATEGORY)
        assertThat(answer.category).isEqualTo(Category.AI)
        assertThat(answer.amount).isEqualTo(usd(3500))
    }

    @Test
    fun `empty data and unknown questions are honest`() = runTest {
        assertThat(assistant.ask("how much", context()).kind).isEqualTo(AnswerKind.NO_SUBSCRIPTIONS)
        assertThat(assistant.ask("tell me a joke", context(netflix)).kind).isEqualTo(AnswerKind.UNKNOWN)
        assertThat(assistant.ask("gaming", context(netflix)).kind).isEqualTo(AnswerKind.NOTHING_IN_CATEGORY)
    }
}
