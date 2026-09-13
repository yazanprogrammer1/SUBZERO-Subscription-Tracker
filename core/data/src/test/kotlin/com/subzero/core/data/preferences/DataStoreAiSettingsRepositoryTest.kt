package com.subzero.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.AiProvider
import com.subzero.core.domain.model.AiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DataStoreAiSettingsRepositoryTest {

    /**
     * A reversible stand-in for the Keystore, which Robolectric does not provide. It is enough to
     * pin the contract the repository depends on: what lands in preferences is not the key itself.
     */
    private class ReversibleCipher(private val working: Boolean = true) : KeyCipher {
        override fun encrypt(plaintext: String): String? =
            if (working) "enc(" + plaintext.reversed() + ")" else null

        override fun decrypt(stored: String): String? =
            stored.removeSurrounding("enc(", ")").takeIf { it != stored }?.reversed()
    }

    private val cipher = ReversibleCipher()

    // DataStore needs a scope that actually runs; a TestScope would never drive its writer.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreAiSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(context.cacheDir, "ai-settings-test.preferences_pb").apply { delete() }
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = DataStoreAiSettingsRepository(dataStore, cipher)
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun `settings round-trip and the endpoint is normalized`() = runTest {
        repository.update(
            AiSettings(
                apiKey = "sk-secret",
                baseUrl = "  https://api.example.com/  ",
                model = " claude-opus-5 ",
                provider = AiProvider.ANTHROPIC,
            ),
        )

        val stored = repository.settings.first()
        assertThat(stored.apiKey).isEqualTo("sk-secret")
        assertThat(stored.baseUrl).isEqualTo("https://api.example.com")
        assertThat(stored.model).isEqualTo("claude-opus-5")
        assertThat(stored.provider).isEqualTo(AiProvider.ANTHROPIC)
    }

    @Test
    fun `the key is not written in the clear`() = runTest {
        repository.update(AiSettings(apiKey = "sk-secret", baseUrl = "https://api.example.com", model = "m"))

        val raw = dataStore.data.first()[stringPreferencesKey("ai_api_key")]
        assertThat(raw).isNotNull()
        assertThat(raw).doesNotContain("sk-secret")
    }

    @Test
    fun `a blank key keeps the stored one so the model can be edited alone`() = runTest {
        repository.update(AiSettings(apiKey = "sk-secret", baseUrl = "https://api.example.com", model = "first"))

        repository.update(AiSettings(apiKey = "", baseUrl = "https://api.example.com", model = "second"))

        val stored = repository.settings.first()
        assertThat(stored.apiKey).isEqualTo("sk-secret")
        assertThat(stored.model).isEqualTo("second")
    }

    @Test
    fun `clear forgets everything`() = runTest {
        repository.update(AiSettings(apiKey = "sk-secret", baseUrl = "https://api.example.com", model = "m", provider = AiProvider.ANTHROPIC))

        repository.clear()

        assertThat(repository.settings.first()).isEqualTo(AiSettings.Empty)
    }

    @Test
    fun `a device that cannot encrypt fails the save instead of dropping the key`() = runTest {
        val broken = DataStoreAiSettingsRepository(dataStore, ReversibleCipher(working = false))

        val thrown = runCatching { broken.update(AiSettings(apiKey = "sk-secret", baseUrl = "https://api.example.com", model = "m")) }

        assertThat(thrown.exceptionOrNull()).isInstanceOf(KeyStorageException::class.java)
        assertThat(repository.settings.first().apiKey).isEmpty()
    }

    @Test
    fun `ciphertext that cannot be decrypted reads as no key rather than crashing`() = runTest {
        dataStore.edit { it[stringPreferencesKey("ai_api_key")] = "not-a-valid-envelope" }

        assertThat(repository.settings.first().apiKey).isEmpty()
    }
}
