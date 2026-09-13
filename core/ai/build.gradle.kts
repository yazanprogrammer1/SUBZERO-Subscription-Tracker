import java.util.Properties

plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.subzero.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The model API key and endpoint come from the untracked local.properties (or CI env vars) and
// reach the app only as BuildConfig fields. Nothing here is committed. Without a key the remote
// assistant is unavailable and the app uses the on-device one.
val localProperties = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

fun setting(name: String, envName: String, default: String = ""): String =
    localProperties?.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: default

android {
    namespace = "com.subzero.core.ai"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "AI_API_KEY", "\"${setting("subzero.ai.apiKey", "SUBZERO_AI_API_KEY")}\"")
        buildConfigField("String", "AI_BASE_URL", "\"${setting("subzero.ai.baseUrl", "SUBZERO_AI_BASE_URL", "https://api.anthropic.com")}\"")
        buildConfigField("String", "AI_MODEL", "\"${setting("subzero.ai.model", "SUBZERO_AI_MODEL", "claude-opus-5")}\"")
        buildConfigField("String", "AI_PROVIDER", "\"${setting("subzero.ai.provider", "SUBZERO_AI_PROVIDER", "anthropic")}\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.kotlinx.coroutines.test)
}
