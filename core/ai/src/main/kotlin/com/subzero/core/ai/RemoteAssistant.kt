package com.subzero.core.ai

import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.Assistant
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.assistant.LocalAssistant
import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers through a hosted model. What leaves the device is a compact, anonymized summary of
 * the active subscriptions (name, amount, cycle, category, declared usage, next date) plus the
 * question — never notes, ids, or the display name. The system prompt enforces the product
 * rules from the specification: answer only from the data, never instruct to cancel, admit
 * missing usage data, reply in the user's language.
 */
@Singleton
class RemoteAssistant @Inject constructor(
    private val transport: ChatTransport,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer {
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt(Locale.getDefault())),
            ChatMessage(role = "user", content = buildString {
                appendLine("DATA (today ${context.today}, home currency ${context.homeCurrency.code}):")
                appendLine(summarize(context))
                appendLine()
                append("QUESTION: ").append(question.trim())
            }),
        )
        val text = transport.complete(messages)
        return AssistantAnswer(kind = AnswerKind.TEXT, text = text, isRemote = true)
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
        - Never tell the user what they should definitely cancel. You may say a subscription
          "appears to have low usage relative to its cost" and ask if they want to review it.
        - Be concise: two to five short sentences, plain text, no markdown headers or tables.
        - Reply in the language of the question; if unclear, use ${locale.getDisplayLanguage(Locale.ENGLISH)}.
    """.trimIndent()

    /** One line per active subscription, machine-readable and free of personal notes. */
    internal fun summarize(context: AssistantContext): String {
        val active = context.subscriptions.filter { it.isActive }
        if (active.isEmpty()) return "(no active subscriptions)"
        return active.joinToString("\n") { it.toLine() }
    }

    private fun Subscription.toLine(): String {
        val monthly = BillingSchedule.monthlyEquivalent(price, billingCycle)
        return "- ${name.take(MAX_NAME)} | ${price.amountMinor} ${price.currency.code} every ${billingCycle.every} ${billingCycle.unit.name.lowercase()} " +
            "(~${monthly.amountMinor} ${monthly.currency.code}/month) | category ${category.name.lowercase()} | usage ${usage.name.lowercase()} | next $nextBillingDate"
    }

    private companion object {
        const val MAX_NAME = 60
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
    private val config: AiConfig,
    private val preferences: UserPreferencesRepository,
) : Assistant {

    override suspend fun ask(question: String, context: AssistantContext): AssistantAnswer {
        val enhanced = preferences.preferences.first().aiEnhancedEnabled
        if (!enhanced || !config.isAvailable) return local.ask(question, context)
        return runCatching { remote.ask(question, context) }
            .getOrElse { local.ask(question, context).copy(fellBack = true) }
    }
}
