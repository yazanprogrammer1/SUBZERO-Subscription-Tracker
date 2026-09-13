package com.subzero.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.data.export.DataManager
import com.subzero.core.data.export.StorageInfo
import com.subzero.core.ai.AiConfig
import com.subzero.core.ai.ChatMessage
import com.subzero.core.ai.ChatTransport
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.notifications.NotificationLedger
import com.subzero.core.notifications.NotificationScheduler
import com.subzero.core.notifications.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot outcomes the screen shows once (snackbar-style), then clears. */
enum class SettingsMessage { EXPORTED, EXPORT_FAILED, DATA_DELETED, DELETE_FAILED, AI_OK, AI_FAILED }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val preferences: UserPreferences,
        /** Whether the OS currently lets SUBZERO post notifications. */
        val notificationsAllowed: Boolean,
        val storage: StorageInfo?,
        val appVersion: String,
        /** A model key is configured in this build, so the enhanced-answers option can be offered. */
        val aiAvailable: Boolean = false,
        /** Model and host the assistant is configured to call, so a failure is diagnosable. */
        val aiTarget: String? = null,
        val isBusy: Boolean = false,
        val message: SettingsMessage? = null,
        /** Provider error text from the last connection test, shown verbatim so setup problems are diagnosable. */
        val aiError: String? = null,
    ) : SettingsUiState {
        val anyNotificationEnabled: Boolean
            get() = preferences.notifications.let { it.upcomingChargeEnabled || it.monthlySummaryEnabled || it.savingsInsightsEnabled }
    }
}

/** App metadata the settings screen shows; provided by the app module. */
data class AppInfo(val versionName: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val subscriptions: SubscriptionRepository,
    private val notifier: Notifier,
    private val scheduler: NotificationScheduler,
    private val ledger: NotificationLedger,
    private val dataManager: DataManager,
    appInfo: AppInfo,
    aiConfig: AiConfig,
    private val chatTransport: ChatTransport,
) : ViewModel() {

    private val aiAvailable = aiConfig.isAvailable
    private val aiTarget = aiConfig.takeIf { it.isAvailable }
        ?.let { "${it.model} · ${it.baseUrl.substringAfter("://").substringBefore('/')}" }

    private data class Transient(
        val notificationsAllowed: Boolean,
        val storage: StorageInfo? = null,
        val isBusy: Boolean = false,
        val message: SettingsMessage? = null,
        val aiError: String? = null,
    )

    private val transient = MutableStateFlow(Transient(notificationsAllowed = notifier.areNotificationsEnabled))

    val uiState: StateFlow<SettingsUiState> = combine(preferences.preferences, transient) { prefs, t ->
        SettingsUiState.Ready(
            preferences = prefs,
            notificationsAllowed = t.notificationsAllowed,
            storage = t.storage,
            appVersion = appInfo.versionName,
            aiAvailable = aiAvailable,
            aiTarget = aiTarget,
            isBusy = t.isBusy,
            message = t.message,
            aiError = t.aiError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState.Loading,
    )

    init {
        refreshStorage()
    }

    /** Called when the screen resumes or a permission request returns, since the OS state can change outside the app. */
    fun refreshPermission() = transient.update { it.copy(notificationsAllowed = notifier.areNotificationsEnabled) }

    fun refreshStorage() {
        viewModelScope.launch {
            val info = runCatching { dataManager.storageInfo() }.getOrNull()
            transient.update { it.copy(storage = info) }
        }
    }

    fun setDisplayName(name: String?) = viewModelScope.launch { preferences.setDisplayName(name) }

    fun setHomeCurrency(currency: CurrencyCode) = viewModelScope.launch { preferences.setHomeCurrency(currency) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    fun setAiEnhancedEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setAiEnhancedEnabled(enabled) }

    /** Sends a one-word request so a wrong key, endpoint or model shows up here, not mid-conversation. */
    fun testAiConnection() = busy {
        runCatching { chatTransport.complete(listOf(ChatMessage("user", "Reply with the single word OK."))) }
            .fold(
                onSuccess = { transient.update { it.copy(aiError = null) }; SettingsMessage.AI_OK },
                onFailure = { e -> transient.update { it.copy(aiError = e.message) }; SettingsMessage.AI_FAILED },
            )
    }

    fun setUpcomingEnabled(enabled: Boolean) = updateNotifications { it.copy(upcomingChargeEnabled = enabled) }

    fun setUpcomingDaysBefore(days: Int) = updateNotifications { it.copy(upcomingChargeDaysBefore = days) }

    fun setMonthlySummaryEnabled(enabled: Boolean) = updateNotifications { it.copy(monthlySummaryEnabled = enabled) }

    fun setSavingsInsightsEnabled(enabled: Boolean) = updateNotifications { it.copy(savingsInsightsEnabled = enabled) }

    /** Writes the export to the document the user picked. */
    fun exportTo(uri: Uri, format: ExportFormat) = busy {
        runCatching { dataManager.writeExport(uri, format) }
            .fold(onSuccess = { SettingsMessage.EXPORTED }, onFailure = { SettingsMessage.EXPORT_FAILED })
    }

    /** Wipes subscriptions, preferences and the notification ledger; onboarding shows again. */
    fun deleteAllData() = busy {
        runCatching {
            subscriptions.deleteAll()
            ledger.clear()
            scheduler.cancel()
            preferences.clear()
        }.fold(onSuccess = { SettingsMessage.DATA_DELETED }, onFailure = { SettingsMessage.DELETE_FAILED })
    }

    fun consumeMessage() = transient.update { it.copy(message = null) }

    private fun updateNotifications(transform: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            preferences.setNotificationPreferences(transform(preferences.preferences.first().notifications))
            scheduler.ensureScheduled()
        }
    }

    private fun busy(block: suspend () -> SettingsMessage) {
        if (transient.value.isBusy) return
        transient.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val message = block()
            transient.update { it.copy(isBusy = false, message = message) }
            refreshStorage()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
