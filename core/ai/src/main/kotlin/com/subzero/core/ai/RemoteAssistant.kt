package com.subzero.core.ai

import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.Assistant
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.assistant.AssistantSuggestion
import com.subzero.core.domain.assistant.LocalAssistant
import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers through a hosted model. What leaves the device is a compact, anonymized summary of
 * the subscriptions (name, amount, cycle, category, declared usage, next date), the totals and
 * recent price changes, the earlier turns of this conversation, and the question — never notes,
 * ids, or the display name. The system prompt enforces the product rules from the specification:
 * answer only from the data, never instruct to cancel, admit missing usage data, reply in the
 * user's language.
 */
@Singleton
class RemoteAssistant @Inject constructor(
    private val transport: ChatTransport,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer {
        val messages = buildList {
            add(ChatMessage(role = "system", content = systemPrompt(Locale.getDefault())))
            context.history.forEach { turn ->
                add(ChatMessage(role = if (turn.fromUser) "user" else "assistant", content = turn.text))
            }
            add(ChatMessage(role = "user", content = userMessage(question, context)))
        }
        val text = transport.complete(messages)
        return AssistantAnswer(
            kind = AnswerKind.TEXT,
            text = text,
            isRemote = true,
            suggestions = followUps,
        )
    }

    internal fun userMessage(question: String, context: AssistantContext): String = buildString {
        appendLine("DATA (today ${context.today}, home currency ${context.homeCurrency.code}):")
        appendLine(summarize(context))
        appendLine()
        append("QUESTION: ").append(question.trim())
    }

    /** The rules the model must follow. Kept short: the data is the context, not the prompt. */
    internal fun systemPrompt(locale: Locale): String = """
        You are SUBZERO, a calm assistant inside a subscription-tracking app. You answer questions
        about the user's recurring subscriptions using ONLY the DATA block in the message.
        Rules:
        - Never invent subscriptions, prices, dates or usage. If the data does not contain what is
          needed, say so plainly (for example: "I don't have enough usage data to say").
        - Money: amounts are given in minor units with a currency code; present them as normal
          amounts (1549 USD -> $15.49). Monthly equivalents: yearly / 12, quarterly / 3, weekly x 52 / 12.
        - Only home-currency amounts are summed in the totals; if a subscription is in another
          currency, say it is not included rather than converting it.
        - Never tell the user what they should definitely cancel. You may say a subscription
          "appears to have low usage relative to its cost" and ask if they want to review it.
        - Usage is what the user declared, not measured. Say so if it matters to the answer.
        - Stay inside this app's subject: subscriptions, their cost, timing, usage and history.
          For anything else, say that is outside what you can see and offer a question you can answer.
        - Be concise: two to five short sentences, plain text, no markdown headers or tables.
        - Reply in the language of the question; if unclear, use ${locale.getDisplayLanguage(Locale.ENGLISH)}.
    """.trimIndent()

    /** Everything the model may answer from, machine-readable and free of personal notes. */
    internal fun summarize(context: AssistantContext): String {
        val active = context.subscriptions.filter { it.isActive }
        if (active.isEmpty() && context.subscriptions.isEmpty()) return "(no subscriptions tracked yet)"
        val home = active.filter { it.price.currency == context.homeCurrency }
        val monthly = home.fold(Money.zero(context.homeCurrency)) { acc, s -> acc + BillingSchedule.monthlyEquivalent(s.price, s.billingCycle) }
        val yearly = home.fold(Money.zero(context.homeCurrency)) { acc, s -> acc + BillingSchedule.yearlyEquivalent(s.price, s.billingCycle) }
        return buildString {
            appendLine("ACTIVE (${active.size}):")
            if (active.isEmpty()) appendLine("- none") else active.forEach { appendLine(it.toLine()) }
            val inactive = context.subscriptions.filterNot { it.isActive }
            if (inactive.isNotEmpty()) {
                appendLine("INACTIVE (${inactive.size}): " + inactive.joinToString("; ") { "${it.name.take(MAX_NAME)} (${it.status.name.lowercase()})" })
            }
            appendLine("TOTALS (home currency only, ${home.size} of ${active.size} subscriptions): ${monthly.amountMinor}/month, ${yearly.amountMinor}/year")
            val increases = priceIncreaseLines(context)
            if (increases.isNotEmpty()) {
                appendLine("PRICE CHANGES:")
                increases.forEach { appendLine(it) }
            }
            val recorded = recordedLine(context)
            if (recorded != null) appendLine(recorded)
        }.trimEnd()
    }

    private fun Subscription.toLine(): String {
        val monthly = BillingSchedule.monthlyEquivalent(price, billingCycle)
        return "- ${name.take(MAX_NAME)} | ${price.amountMinor} ${price.currency.code} every ${billingCycle.every} ${billingCycle.unit.name.lowercase()} " +
            "(~${monthly.amountMinor} ${monthly.currency.code}/month) | category ${category.name.lowercase()} | usage ${usage.name.lowercase()} | next $nextBillingDate"
    }

    private fun priceIncreaseLines(context: AssistantContext): List<String> {
        val names = context.subscriptions.associate { it.id to it.name }
        return context.priceChanges
            .groupBy { it.subscriptionId }
            .mapNotNull { (id, changes) ->
                val name = names[id] ?: return@mapNotNull null
                val sorted = changes.sortedBy { it.effectiveFrom }
                if (sorted.size < 2) return@mapNotNull null
                val latest = sorted.last()
                val previous = sorted[sorted.size - 2]
                if (latest.price.currency != previous.price.currency) return@mapNotNull null
                "- ${name.take(MAX_NAME)} | ${previous.price.amountMinor} -> ${latest.price.amountMinor} ${latest.price.currency.code} on ${latest.effectiveFrom}"
            }
    }

    /** Recorded payments, summarized by month so the model can answer "what did I actually pay". */
    private fun recordedLine(context: AssistantContext): String? {
        if (context.payments.isEmpty()) return null
        val byMonth = context.payments
            .filter { it.amount.currency == context.homeCurrency }
            .groupBy { YearMonth.from(it.paidOn) }
            .toSortedMap()
        if (byMonth.isEmpty()) return null
        val months = byMonth.entries.toList().takeLast(RECORDED_MONTHS).joinToString("; ") { (month, payments) ->
            val total = payments.fold(Money.zero(context.homeCurrency)) { acc, payment -> acc + payment.amount }
            "$month=${total.amountMinor} (${payments.size} payments)"
        }
        return "RECORDED PAYMENTS (${context.homeCurrency.code}, minor units): $months"
    }

    private companion object {
        const val MAX_NAME = 60
        const val RECORDED_MONTHS = 12

        /** Chips offered under a model answer; the on-device assistant can answer all of them. */
        val followUps = listOf(
            AssistantSuggestion.TOTAL,
            AssistantSuggestion.TOP,
            AssistantSuggestion.CANCEL,
            AssistantSuggestion.NEXT,
        )
    }
}

/**
 * Routes to the remote model only when the user opted in and a key is configured; otherwise,
 * or on any failure, the on-device assistant answers. The app never depends on the network.
 */
@Singleton
class CompositeAssistant @Inject constructor(
    private val local: LocalAssistant,
    private val remote: RemoteAssistant,
    private val configSource: AiConfigSource,
    private val preferences: UserPreferencesRepository,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer {
        val enhanced = preferences.preferences.first().aiEnhancedEnabled
        if (!enhanced || !configSource.current().isAvailable) return local.ask(question, context)
        return runCatching { remote.ask(question, context) }
            .getOrElse { local.ask(question, context).copy(fellBack = true) }
    }
}
