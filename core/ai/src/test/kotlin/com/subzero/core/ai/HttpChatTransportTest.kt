package com.subzero.core.ai

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.AiProvider
import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.testing.FakeAiSettingsRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

class HttpChatTransportTest {

    private val messages = listOf(
        ChatMessage("system", "Answer from the data only."),
        ChatMessage("user", "How much do I spend?"),
    )

    private val transport = HttpChatTransport(
        configSource = AiConfigSource(FakeAiSettingsRepository(), defaults = AiConfig(apiKey = "", baseUrl = "", model = "")),
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun config(provider: AiProvider, baseUrl: String = "https://api.example.com") =
        AiConfig(apiKey = "k", baseUrl = baseUrl, model = "claude-opus-5", provider = provider)

    @Test
    fun `anthropic requests lift the system prompt out of the messages`() {
        val request = transport.anthropicRequest(config(AiProvider.ANTHROPIC), messages)

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
        val request = transport.openAiRequest(config(AiProvider.OPENAI_COMPATIBLE), messages)

        assertThat(request.url).isEqualTo("https://api.example.com/chat/completions")
        assertThat(request.headers).containsEntry("Authorization", "Bearer k")
        assertThat(request.body.substringAfter("\"messages\":")).contains("\"system\"")
    }

    @Test
    fun `a base url that already ends in v1 is not doubled`() {
        val request = transport.anthropicRequest(config(AiProvider.ANTHROPIC, baseUrl = "https://api.example.com/v1"), messages)

        assertThat(request.url).isEqualTo("https://api.example.com/v1/messages")
    }

    @Test
    fun `the provider error sentence is pulled out of either common body shape`() {
        assertThat(transport.providerMessage("""{"error":{"message":"Budget pool quota has been exhausted."}}"""))
            .isEqualTo("Budget pool quota has been exhausted.")
        assertThat(transport.providerMessage("""{"message":"UNAUTHENTICATED","success":false}"""))
            .isEqualTo("UNAUTHENTICATED")
        assertThat(transport.providerMessage("")).isNull()
        // An unknown shape still has to be diagnosable, so the raw body comes through.
        assertThat(transport.providerMessage("<html>502 Bad Gateway</html>")).contains("502 Bad Gateway")
    }

    @Test
    fun `provider names map to a wire protocol and anything unknown stays openai compatible`() {
        assertThat(AiProvider.parse("anthropic")).isEqualTo(AiProvider.ANTHROPIC)
        assertThat(AiProvider.parse(" Claude ")).isEqualTo(AiProvider.ANTHROPIC)
        assertThat(AiProvider.parse("openai")).isEqualTo(AiProvider.OPENAI_COMPATIBLE)
        assertThat(AiProvider.parse("")).isEqualTo(AiProvider.OPENAI_COMPATIBLE)
    }

    @Test
    fun `a config needs a key, an https endpoint and a model`() {
        assertThat(AiConfig("", "https://api.example.com", "m").isAvailable).isFalse()
        assertThat(AiConfig("k", "http://api.example.com", "m").isAvailable).isFalse()
        assertThat(AiConfig("k", "https://api.example.com", "").isAvailable).isFalse()
        assertThat(AiConfig("k", "https://api.example.com", "m").isAvailable).isTrue()
    }

    @Test
    fun `user settings win field by field over what the build shipped`() {
        val shipped = AiConfig("built-in", "https://shipped.example.com", "shipped-model", AiProvider.OPENAI_COMPATIBLE)

        val untouched = shipped.overlaidWith(AiSettings.Empty)
        assertThat(untouched).isEqualTo(shipped)

        val overlaid = shipped.overlaidWith(
            AiSettings(apiKey = "mine", baseUrl = "https://mine.example.com/", provider = AiProvider.ANTHROPIC),
        )
        assertThat(overlaid.apiKey).isEqualTo("mine")
        assertThat(overlaid.baseUrl).isEqualTo("https://mine.example.com")
        assertThat(overlaid.model).isEqualTo("shipped-model")
        assertThat(overlaid.provider).isEqualTo(AiProvider.ANTHROPIC)
    }
}
