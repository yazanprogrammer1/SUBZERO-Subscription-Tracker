package com.subzero.core.ai

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.assistant.AssistantTurn
import com.subzero.core.domain.assistant.LocalAssistant
import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeAiSettingsRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class RemoteAssistantTest {

    private class FakeTransport(private val reply: String? = "You spend about $35 a month on AI.") : ChatTransport {
        var lastMessages: List<ChatMessage> = emptyList()
        override suspend fun complete(messages: List<ChatMessage>): String {
            lastMessages = messages
            return reply ?: throw AiException("down")
        }
    }

    private val today = date("2026-09-12")
    private val context = AssistantContext(
        subscriptions = listOf(
            subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"), notes = "shared with Sam, PIN 1234", usage = DeclaredUsage.DAILY),
            subscription(id = "p", name = "Paused thing", price = usd(999), status = SubscriptionStatus.PAUSED),
        ),
        priceChanges = emptyList(),
        homeCurrency = CurrencyCode.USD,
        today = today,
    )

    @Test
    fun `sends only an anonymized summary of active subscriptions plus the question`() = runTest {
        val transport = FakeTransport()
        val answer = RemoteAssistant(transport).ask("How much on AI?", context)

        assertThat(answer.kind).isEqualTo(AnswerKind.TEXT)
        assertThat(answer.isRemote).isTrue()
        assertThat(answer.text).contains("$35")

        val user = transport.lastMessages.last { it.role == "user" }.content
        assertThat(user).contains("Netflix | 1549 USD every 1 month")
        assertThat(user).contains("usage daily")
        assertThat(user).contains("QUESTION: How much on AI?")
        assertThat(user).contains("TOTALS (home currency only, 1 of 1 subscriptions): 1549/month, 18588/year")
        // Paused subscriptions are named with their status so "what did I pause?" can be answered,
        // but they stay out of the totals. Notes never leave the device.
        assertThat(user).contains("INACTIVE (1): Paused thing (paused)")
        assertThat(user).doesNotContain("PIN 1234")
        assertThat(user).doesNotContain("shared with Sam")

        val system = transport.lastMessages.first { it.role == "system" }.content
        assertThat(system).contains("Never tell the user what they should definitely cancel")
        assertThat(system).contains("ONLY the DATA block")
    }

    @Test
    fun `earlier turns are replayed so follow-ups keep their meaning`() = runTest {
        val transport = FakeTransport()
        val withHistory = context.copy(
            history = listOf(
                AssistantTurn(fromUser = true, text = "How much on AI?"),
                AssistantTurn(fromUser = false, text = "You spend about $35 a month on AI."),
            ),
        )

        RemoteAssistant(transport).ask("and what about entertainment?", withHistory)

        val roles = transport.lastMessages.map { it.role }
        assertThat(roles).containsExactly("system", "user", "assistant", "user").inOrder()
        assertThat(transport.lastMessages[1].content).isEqualTo("How much on AI?")
        assertThat(transport.lastMessages[2].content).contains("$35 a month")
        assertThat(transport.lastMessages.last().content).contains("QUESTION: and what about entertainment?")
    }

    @Test
    fun `recorded payments are summarized by month`() = runTest {
        val transport = FakeTransport()
        val withPayments = context.copy(
            payments = listOf(
                paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-08-16")),
                paymentRecord(subscriptionId = "n", amount = usd(1549), paidOn = date("2026-09-16")),
            ),
        )

        RemoteAssistant(transport).ask("what did I pay?", withPayments)

        val user = transport.lastMessages.last().content
        assertThat(user).contains("RECORDED PAYMENTS (USD, minor units): 2026-08=1549 (1 payments); 2026-09=1549 (1 payments)")
    }

    @Test
    fun `system prompt names the fallback language`() {
        assertThat(RemoteAssistant(FakeTransport()).systemPrompt(Locale.forLanguageTag("ar"))).contains("Arabic")
    }

    @Test
    fun `composite uses local unless enhanced is on and configured`() = runTest {
        val local = LocalAssistant(GetUpcomingPaymentsUseCase(fixedClock(today)))
        val prefs = FakeUserPreferencesRepository()
        val transport = FakeTransport()
        val configured = configSource(AiSettings(apiKey = "k", baseUrl = "https://example.invalid", model = "m"))

        val off = CompositeAssistant(local, RemoteAssistant(transport), configured, prefs).ask("how much", context)
        assertThat(off.isRemote).isFalse()
        assertThat(off.kind).isEqualTo(AnswerKind.TOTAL_SPEND)

        prefs.setAiEnhancedEnabled(true)
        val on = CompositeAssistant(local, RemoteAssistant(transport), configured, prefs).ask("how much", context)
        assertThat(on.isRemote).isTrue()

        val unconfigured = configSource(AiSettings(baseUrl = "https://example.invalid", model = "m"))
        val noKey = CompositeAssistant(local, RemoteAssistant(transport), unconfigured, prefs).ask("how much", context)
        assertThat(noKey.isRemote).isFalse()
    }

    @Test
    fun `composite falls back to local when the model fails and says so`() = runTest {
        val local = LocalAssistant(GetUpcomingPaymentsUseCase(fixedClock(today)))
        val prefs = FakeUserPreferencesRepository().apply { setAiEnhancedEnabled(true) }
        val configured = configSource(AiSettings(apiKey = "k", baseUrl = "https://example.invalid", model = "m"))

        val answer = CompositeAssistant(local, RemoteAssistant(FakeTransport(reply = null)), configured, prefs).ask("how much", context)

        assertThat(answer.isRemote).isFalse()
        assertThat(answer.fellBack).isTrue()
        assertThat(answer.kind).isEqualTo(AnswerKind.TOTAL_SPEND)
        assertThat(answer.amount).isEqualTo(usd(1549))
    }

}

/** An [AiConfigSource] over settings held in memory; the build defaults are empty under test. */
private fun configSource(settings: AiSettings) =
    AiConfigSource(FakeAiSettingsRepository(settings), defaults = AiConfig(apiKey = "", baseUrl = "", model = ""))
