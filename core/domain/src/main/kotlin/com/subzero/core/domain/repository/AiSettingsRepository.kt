package com.subzero.core.domain.repository

import com.subzero.core.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow

/**
 * The assistant endpoint the user configured. Implemented in `core:data`, where the key is
 * encrypted before it is written.
 */
interface AiSettingsRepository {

    val settings: Flow<AiSettings>

    /** Saves [settings]; a blank [AiSettings.apiKey] keeps the key already stored. */
    suspend fun update(settings: AiSettings)

    /** Forgets the key, endpoint and model, returning to whatever the build shipped. */
    suspend fun clear()
}
