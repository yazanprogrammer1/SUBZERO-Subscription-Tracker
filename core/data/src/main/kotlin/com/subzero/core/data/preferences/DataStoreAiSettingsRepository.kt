package com.subzero.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.subzero.core.domain.model.AiProvider
import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.repository.AiSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the assistant endpoint in the same preferences file as everything else, except the key,
 * which is encrypted by [KeyCipher] first.
 */
@Singleton
class DataStoreAiSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeyCipher,
) : AiSettingsRepository {

    private object Keys {
        val API_KEY = stringPreferencesKey("ai_api_key")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val MODEL = stringPreferencesKey("ai_model")
        val PROVIDER = stringPreferencesKey("ai_provider")
    }

    override val settings: Flow<AiSettings> = dataStore.data.map { prefs ->
        AiSettings(
            apiKey = prefs[Keys.API_KEY]?.let(cipher::decrypt).orEmpty(),
            baseUrl = prefs[Keys.BASE_URL].orEmpty(),
            model = prefs[Keys.MODEL].orEmpty(),
            provider = prefs[Keys.PROVIDER]?.let { name -> AiProvider.entries.firstOrNull { it.name == name } },
        )
    }

    override suspend fun update(settings: AiSettings) {
        dataStore.edit { prefs ->
            // A blank key means "leave the stored one alone", so the UI never has to read it back
            // to re-save an endpoint or a model.
            if (settings.apiKey.isNotBlank()) {
                val envelope = cipher.encrypt(settings.apiKey)
                    ?: throw KeyStorageException("This device could not store the key securely")
                prefs[Keys.API_KEY] = envelope
            }
            prefs.put(Keys.BASE_URL, settings.baseUrl.trim().trimEnd('/'))
            prefs.put(Keys.MODEL, settings.model.trim())
            settings.provider?.let { prefs[Keys.PROVIDER] = it.name } ?: prefs.remove(Keys.PROVIDER)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            listOf(Keys.API_KEY, Keys.BASE_URL, Keys.MODEL, Keys.PROVIDER).forEach(prefs::remove)
        }
    }

    private fun MutablePreferences.put(key: Preferences.Key<String>, value: String) {
        if (value.isBlank()) remove(key) else set(key, value)
    }
}
