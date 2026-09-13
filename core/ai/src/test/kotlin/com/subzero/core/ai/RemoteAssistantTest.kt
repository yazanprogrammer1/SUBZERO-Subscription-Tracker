package com.subzero.core.ai

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.assistant.LocalAssistant
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
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
        assertThat(user).doesNotContain("Paused thing")
        assertThat(user).doesNotContain("PIN 1234")
        assertThat(user).doesNotContain("shared with Sam")

        val system = transport.lastMessages.first { it.role == "system" }.content
        assertThat(system).contains("Never tell the user what they should definitely cancel")
        assertThat(system).contains("ONLY the DATA block")
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
        val configured = AiConfig(apiKey = "k", baseUrl = "https://example.invalid", model = "m")

        val off = CompositeAssistant(local, RemoteAssistant(transport), configured, prefs).ask("how much", context)
        assertThat(off.isRemote).isFalse()
        assertThat(off.kind).isEqualTo(AnswerKind.TOTAL_SPEND)

        prefs.setAiEnhancedEnabled(true)
        val on = CompositeAssistant(local, RemoteAssistant(transport), configured, prefs).ask("how much", context)
        assertThat(on.isRemote).isTrue()

        val unconfigured = AiConfig(apiKey = "", baseUrl = "https://example.invalid", model = "m")
        val noKey = CompositeAssistant(local, RemoteAssistant(transport), unconfigured, prefs).ask("how much", context)
        assertThat(noKey.isRemote).isFalse()
    }

    @Test
    fun `composite falls back to local when the model fails and says so`() = runTest {
        val local = LocalAssistant(GetUpcomingPaymentsUseCase(fixedClock(today)))
        val prefs = FakeUserPreferencesRepository().apply { setAiEnhancedEnabled(true) }
        val configured = AiConfig(apiKey = "k", baseUrl = "https://example.invalid", model = "m")

        val answer = CompositeAssistant(local, RemoteAssistant(FakeTransport(reply = null)), configured, prefs).ask("how much", context)

        assertThat(answer.isRemote).isFalse()
        assertThat(answer.fellBack).isTrue()
        assertThat(answer.kind).isEqualTo(AnswerKind.TOTAL_SPEND)
        assertThat(answer.amount).isEqualTo(usd(1549))
    }

    @Test
    fun `config requires a key and https`() {
        assertThat(AiConfig("", "https://api.example.com", "m").isAvailable).isFalse()
        assertThat(AiConfig("k", "http://api.example.com", "m").isAvailable).isFalse()
        assertThat(AiConfig("k", "https://api.example.com", "m").isAvailable).isTrue()
    }
}
