package com.subzero.core.ai

import com.subzero.core.common.coroutines.Dispatcher
import com.subzero.core.common.coroutines.SubzeroDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Which wire protocol the configured endpoint speaks. */
enum class AiProvider {
    /** Anthropic Messages API (`POST {baseUrl}/v1/messages`, `x-api-key`). */
    ANTHROPIC,

    /** OpenAI-style chat completions (`POST {baseUrl}/chat/completions`, `Authorization: Bearer`). */
    OPENAI_COMPATIBLE,
    ;

    companion object {
        fun parse(raw: String): AiProvider = when (raw.trim().lowercase()) {
            "anthropic", "claude" -> ANTHROPIC
            else -> OPENAI_COMPATIBLE
        }
    }
}

/** Where the model lives. Built from BuildConfig; [isAvailable] is false without a key. */
data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val provider: AiProvider = AiProvider.ANTHROPIC,
) {
    val isAvailable: Boolean get() = apiKey.isNotBlank() && baseUrl.startsWith("https://")

    companion object {
        fun fromBuildConfig() = AiConfig(
            apiKey = BuildConfig.AI_API_KEY,
            baseUrl = BuildConfig.AI_BASE_URL.trimEnd('/'),
            model = BuildConfig.AI_MODEL,
            provider = AiProvider.parse(BuildConfig.AI_PROVIDER),
        )
    }
}

/** One turn of the conversation; `role` is `system`, `user` or `assistant`. */
@Serializable
data class ChatMessage(val role: String, val content: String)

/** The minimum SUBZERO needs from a chat model: messages in, one text answer out. */
interface ChatTransport {
    /** Returns the assistant text, or throws [AiException]. */
    suspend fun complete(messages: List<ChatMessage>): String
}

class AiException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * HTTPS-only chat over HttpURLConnection: one endpoint per provider, one JSON shape each, no SDK.
 * Errors of every kind become [AiException] so callers can fall back to the local assistant.
 */
@Singleton
class HttpChatTransport @Inject constructor(
    private val config: AiConfig,
    @Dispatcher(SubzeroDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ChatTransport {

    override suspend fun complete(messages: List<ChatMessage>): String = withContext(ioDispatcher) {
        if (!config.isAvailable) throw AiException("Assistant is not configured")
        val request = when (config.provider) {
            AiProvider.ANTHROPIC -> anthropicRequest(messages)
            AiProvider.OPENAI_COMPATIBLE -> openAiRequest(messages)
        }
        val responseBody = post(request)
        when (config.provider) {
            AiProvider.ANTHROPIC -> parseAnthropic(responseBody)
            AiProvider.OPENAI_COMPATIBLE -> parseOpenAi(responseBody)
        }
    }

    internal class WireRequest(val url: String, val headers: Map<String, String>, val body: String)

    internal fun anthropicRequest(messages: List<ChatMessage>): WireRequest {
        // The Messages API takes the system prompt as a top-level field, not as a message.
        val system = messages.filter { it.role == "system" }.joinToString("\n\n") { it.content }.takeIf { it.isNotBlank() }
        val turns = messages.filter { it.role != "system" }
        // Anthropic documents the endpoint as {host}/v1/messages, so a configured base URL that
        // already ends in /v1 is a natural mistake; accept it rather than posting to /v1/v1.
        return WireRequest(
            url = "${config.baseUrl.removeSuffix("/v1")}/v1/messages",
            headers = mapOf("x-api-key" to config.apiKey, "anthropic-version" to ANTHROPIC_VERSION),
            body = json.encodeToString(AnthropicRequest(model = config.model, maxTokens = MAX_TOKENS, system = system, messages = turns, temperature = TEMPERATURE)),
        )
    }

    internal fun openAiRequest(messages: List<ChatMessage>): WireRequest = WireRequest(
        url = "${config.baseUrl}/chat/completions",
        headers = mapOf("Authorization" to "Bearer ${config.apiKey}"),
        body = json.encodeToString(OpenAiRequest(model = config.model, messages = messages, temperature = TEMPERATURE, maxTokens = MAX_TOKENS)),
    )

    private fun post(request: WireRequest): String {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        return try {
            connection.outputStream.use { it.write(request.body.toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw AiException("HTTP $code${providerMessage(error)?.let { " — $it" }.orEmpty()}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiException("Could not reach the model", e)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * The sentence a provider puts in its error body, whichever of the two common shapes it uses
     * ({"error":{"message":…}} or {"message":…}). Falling back to a slice of the raw body keeps
     * unknown providers diagnosable instead of silently blank.
     */
    internal fun providerMessage(errorBody: String): String? {
        if (errorBody.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(errorBody).jsonObject }.getOrNull()
            ?: return errorBody.take(ERROR_PREVIEW)
        val nested = (root["error"] as? JsonObject)?.get("message")?.jsonPrimitiveOrNull()
        val message = nested ?: root["message"]?.jsonPrimitiveOrNull()
        return (message ?: errorBody.take(ERROR_PREVIEW)).takeIf { it.isNotBlank() }
    }

    private fun JsonElement.jsonPrimitiveOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

    private fun parseAnthropic(responseBody: String): String {
        val response = runCatching { json.decodeFromString<AnthropicResponse>(responseBody) }
            .getOrElse { throw AiException("Unexpected model response", it) }
        return response.content.filter { it.type == "text" }.joinToString("") { it.text.orEmpty() }.trim().takeIf { it.isNotEmpty() }
            ?: throw AiException("Empty model response")
    }

    private fun parseOpenAi(responseBody: String): String {
        val response = runCatching { json.decodeFromString<OpenAiResponse>(responseBody) }
            .getOrElse { throw AiException("Unexpected model response", it) }
        return response.choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AiException("Empty model response")
    }

    @Serializable
    private data class AnthropicRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<ChatMessage>,
        val temperature: Double,
    )

    @Serializable
    private data class AnthropicResponse(val content: List<ContentBlock> = emptyList())

    @Serializable
    private data class ContentBlock(val type: String = "", val text: String? = null)

    @Serializable
    private data class OpenAiRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @SerialName("max_tokens") val maxTokens: Int,
    )

    @Serializable
    private data class OpenAiResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage? = null)

    private companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val TIMEOUT_MS = 20_000
        const val MAX_TOKENS = 400
        const val TEMPERATURE = 0.2
        const val ERROR_PREVIEW = 200
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    }
}
