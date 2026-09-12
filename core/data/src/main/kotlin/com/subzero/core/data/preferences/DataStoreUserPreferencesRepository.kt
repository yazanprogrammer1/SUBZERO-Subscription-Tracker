package com.subzero.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val HOME_CURRENCY = stringPreferencesKey("home_currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFY_UPCOMING = booleanPreferencesKey("notify_upcoming")
        val NOTIFY_UPCOMING_DAYS = intPreferencesKey("notify_upcoming_days")
        val NOTIFY_MONTHLY = booleanPreferencesKey("notify_monthly_summary")
        val NOTIFY_SAVINGS = booleanPreferencesKey("notify_savings")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val defaults = NotificationPreferences.Default
        UserPreferences(
            homeCurrency = prefs[Keys.HOME_CURRENCY]?.let { runCatching { CurrencyCode(it) }.getOrNull() }
                ?: defaultCurrency(),
            themeMode = prefs[Keys.THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            notifications = NotificationPreferences(
                upcomingChargeEnabled = prefs[Keys.NOTIFY_UPCOMING] ?: defaults.upcomingChargeEnabled,
                upcomingChargeDaysBefore = prefs[Keys.NOTIFY_UPCOMING_DAYS] ?: defaults.upcomingChargeDaysBefore,
                monthlySummaryEnabled = prefs[Keys.NOTIFY_MONTHLY] ?: defaults.monthlySummaryEnabled,
                savingsInsightsEnabled = prefs[Keys.NOTIFY_SAVINGS] ?: defaults.savingsInsightsEnabled,
            ),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME],
        )
    }

    override suspend fun setHomeCurrency(currency: CurrencyCode) {
        dataStore.edit { it[Keys.HOME_CURRENCY] = currency.code }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setNotificationPreferences(preferences: NotificationPreferences) {
        dataStore.edit {
            it[Keys.NOTIFY_UPCOMING] = preferences.upcomingChargeEnabled
            it[Keys.NOTIFY_UPCOMING_DAYS] = preferences.upcomingChargeDaysBefore
            it[Keys.NOTIFY_MONTHLY] = preferences.monthlySummaryEnabled
            it[Keys.NOTIFY_SAVINGS] = preferences.savingsInsightsEnabled
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setDisplayName(name: String?) {
        dataStore.edit {
            val trimmed = name?.trim()
            if (trimmed.isNullOrEmpty()) it.remove(Keys.DISPLAY_NAME) else it[Keys.DISPLAY_NAME] = trimmed
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /** The currency of the device locale, or USD when the locale has none (e.g. "en"). */
    private fun defaultCurrency(): CurrencyCode =
        runCatching { CurrencyCode(Currency.getInstance(Locale.getDefault()).currencyCode) }
            .getOrDefault(CurrencyCode.USD)
}
