package com.subzero.core.domain.repository

import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setHomeCurrency(currency: CurrencyCode)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setNotificationPreferences(preferences: NotificationPreferences)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setDisplayName(name: String?)

    /** Wipes every preference. Used by "Delete all data". */
    suspend fun clear()
}
