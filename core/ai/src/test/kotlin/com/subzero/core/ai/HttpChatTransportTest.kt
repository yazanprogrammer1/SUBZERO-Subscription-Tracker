package com.subzero.core.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

class HttpChatTransportTest {

    private val messages = listOf(
        ChatMessage("system", "Answer from the data only."),
        ChatMessage("user", "How much do I spend?"),
    )

    private fun transport(provider: AiProvider, baseUrl: String = "https://api.example.com") = HttpChatTransport(
        config = AiConfig(apiKey = "k", baseUrl = baseUrl, model = "claude-opus-5", provider = provider),
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `anthropic requests lift the system prompt out of the messages`() {
        val request = transport(AiProvider.ANTHROPIC).anthropicRequest(messages)

        assertThat(request.url).isEqualTo("https://api.example.com/v1/messages")
        assertThat(request.headers).containsEntry("x-api-key", "k")
        assertThat(request.headers).containsEntry("anthropic-version", "2023-06-01")
        assertThat(request.headers).doesNotContainKey("Authorization")
        assertThat(request.body).contains("\"system\":\"Answer from the data only.\"")
        assertThat(request.body).contains("\"max_tokens\":")
        // Only the user turn is left in messages; the system turn was hoisted.
        assertThat(request.body.substringAfter("\"messages\":")).doesNotContain("\"system\"")
    }

    @Test
    fun `openai compatible requests keep every message and use bearer auth`() {
        val request = transport(AiProvider.OPENAI_COMPATIBLE).openAiRequest(messages)

        assertThat(request.url).isEqualTo("https://api.example.com/chat/completions")
        assertThat(request.headers).containsEntry("Authorization", "Bearer k")
        assertThat(request.body.substringAfter("\"messages\":")).contains("\"system\"")
    }

    @Test
    fun `a base url that already ends in v1 is not doubled`() {
        val request = transport(AiProvider.ANTHROPIC, baseUrl = "https://api.example.com/v1").anthropicRequest(messages)

        assertThat(request.url).isEqualTo("https://api.example.com/v1/messages")
    }

    @Test
    fun `the provider error sentence is pulled out of either common body shape`() {
        val t = transport(AiProvider.ANTHROPIC)

        assertThat(t.providerMessage("""{"error":{"message":"Budget pool quota has been exhausted."}}"""))
            .isEqualTo("Budget pool quota has been exhausted.")
        assertThat(t.providerMessage("""{"message":"UNAUTHENTICATED","success":false}"""))
            .isEqualTo("UNAUTHENTICATED")
        assertThat(t.providerMessage("")).isNull()
        // An unknown shape still has to be diagnosable, so the raw body comes through.
        assertThat(t.providerMessage("<html>502 Bad Gateway</html>")).contains("502 Bad Gateway")
    }

    @Test
    fun `provider names map to a wire protocol and anything unknown stays openai compatible`() {
        assertThat(AiProvider.parse("anthropic")).isEqualTo(AiProvider.ANTHROPIC)
        assertThat(AiProvider.parse(" Claude ")).isEqualTo(AiProvider.ANTHROPIC)
        assertThat(AiProvider.parse("openai")).isEqualTo(AiProvider.OPENAI_COMPATIBLE)
        assertThat(AiProvider.parse("")).isEqualTo(AiProvider.OPENAI_COMPATIBLE)
    }
}
