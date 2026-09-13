package com.subzero.core.domain.model

/** The wire protocol an assistant endpoint speaks. */
enum class AiProvider {
    /** Anthropic Messages API: `POST {baseUrl}/v1/messages` with `x-api-key`. */
    ANTHROPIC,

    /** OpenAI-style chat completions: `POST {baseUrl}/chat/completions` with a bearer token. */
    OPENAI_COMPATIBLE,
    ;

    companion object {
        fun parse(raw: String): AiProvider = when (raw.trim().lowercase()) {
            "anthropic", "claude" -> ANTHROPIC
            else -> OPENAI_COMPATIBLE
        }
    }
}

/**
 * What the user configured for enhanced answers, on the device.
 *
 * Every field is an override: blank (or null for [provider]) means "use what the build shipped".
 * A key set here is stored encrypted and never leaves the device except as the `Authorization`
 * or `x-api-key` header of a request to [baseUrl].
 */
data class AiSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val provider: AiProvider? = null,
) {
    val isEmpty: Boolean get() = apiKey.isBlank() && baseUrl.isBlank() && model.isBlank() && provider == null

    companion object {
        val Empty = AiSettings()
    }
}
