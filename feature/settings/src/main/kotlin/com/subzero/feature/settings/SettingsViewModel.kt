package com.subzero.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.data.export.DataManager
import com.subzero.core.data.export.StorageInfo
import com.subzero.core.ai.AiConfigSource
import com.subzero.core.ai.AiException
import com.subzero.core.ai.ChatMessage
import com.subzero.core.ai.ChatTransport
import com.subzero.core.domain.model.AiProvider
import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.repository.AiSettingsRepository
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
enum class SettingsMessage { EXPORTED, EXPORT_FAILED, DATA_DELETED, DELETE_FAILED, AI_OK, AI_FAILED, AI_SAVED, AI_SAVE_FAILED, AI_CLEARED }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val preferences: UserPreferences,
        /** Whether the OS currently lets SUBZERO post notifications. */
        val notificationsAllowed: Boolean,
        val storage: StorageInfo?,
        val appVersion: String,
        /** The endpoint the assistant would call, and whether it is usable as configured. */
        val ai: AiUiState = AiUiState(),
        val isBusy: Boolean = false,
        val message: SettingsMessage? = null,
    ) : SettingsUiState {
        val anyNotificationEnabled: Boolean
            get() = preferences.notifications.let { it.upcomingChargeEnabled || it.monthlySummaryEnabled || it.savingsInsightsEnabled }
    }
}

/**
 * What Settings shows and edits for enhanced answers. The key itself is never read back out of
 * storage into the UI: [keySet] says whether one is stored, and an empty field leaves it alone.
 */
data class AiUiState(
    val provider: AiProvider = AiProvider.ANTHROPIC,
    val baseUrl: String = "",
    val model: String = "",
    val keySet: Boolean = false,
    /** Key, https endpoint and model are all present, so a request can be made. */
    val available: Boolean = false,
    /** How the last connection test failed, ready for the screen to word. */
    val failure: AiFailure? = null,
) {
    val target: String? get() = if (available) "$model · ${baseUrl.substringAfter("://").substringBefore('/')}" else null
}

/** Why a connection test failed, mapped from the provider's response so the UI can word it. */
data class AiFailure(val kind: AiFailureKind, val providerMessage: String?)

enum class AiFailureKind {
    /** The key was refused for this endpoint (401/403). */
    REJECTED,

    /** The account has no credit left (402). */
    NO_CREDIT,

    /** No such endpoint or model at that address (404). */
    NOT_FOUND,

    /** Too many requests (429). */
    RATE_LIMITED,

    /** The provider is failing (5xx). */
    PROVIDER_ERROR,

    /** The request never got a reply: no network, bad host, timeout. */
    UNREACHABLE,

    /** Anything else, including a reply that could not be parsed. */
    UNKNOWN,
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
    private val aiConfigSource: AiConfigSource,
    private val aiSettings: AiSettingsRepository,
    private val chatTransport: ChatTransport,
) : ViewModel() {

    private data class Transient(
        val notificationsAllowed: Boolean,
        val storage: StorageInfo? = null,
        val isBusy: Boolean = false,
        val message: SettingsMessage? = null,
        val aiFailure: AiFailure? = null,
    )

    private val transient = MutableStateFlow(Transient(notificationsAllowed = notifier.areNotificationsEnabled))

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.preferences,
        transient,
        aiConfigSource.config,
    ) { prefs, t, aiConfig ->
        SettingsUiState.Ready(
            preferences = prefs,
            notificationsAllowed = t.notificationsAllowed,
            storage = t.storage,
            appVersion = appInfo.versionName,
            ai = AiUiState(
                provider = aiConfig.provider,
                baseUrl = aiConfig.baseUrl,
                model = aiConfig.model,
                keySet = aiConfig.apiKey.isNotBlank(),
                available = aiConfig.isAvailable,
                failure = t.aiFailure,
            ),
            isBusy = t.isBusy,
            message = t.message,
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

    /**
     * Saves the endpoint the user typed. A blank [key] keeps whatever key is already stored, so
     * editing the model does not force the user to paste the key again.
     */
    fun saveAiSettings(provider: AiProvider, baseUrl: String, model: String, key: String) = busy {
        runCatching {
            aiSettings.update(AiSettings(apiKey = key.trim(), baseUrl = baseUrl.trim(), model = model.trim(), provider = provider))
        }.fold(
            onSuccess = { transient.update { it.copy(aiFailure = null) }; SettingsMessage.AI_SAVED },
            onFailure = { SettingsMessage.AI_SAVE_FAILED },
        )
    }

    /** Forgets the key and endpoint, returning to whatever the build shipped. */
    fun clearAiSettings() = busy {
        aiSettings.clear()
        transient.update { it.copy(aiFailure = null) }
        SettingsMessage.AI_CLEARED
    }

    /** Sends a one-word request so a wrong key, endpoint or model shows up here, not mid-conversation. */
    fun testAiConnection() = busy {
        runCatching { chatTransport.complete(listOf(ChatMessage("user", "Reply with the single word OK."))) }
            .fold(
                onSuccess = { transient.update { it.copy(aiFailure = null) }; SettingsMessage.AI_OK },
                onFailure = { e ->
                    transient.update { it.copy(aiFailure = e.toAiFailure()) }
                    SettingsMessage.AI_FAILED
                },
            )
    }

    /** Turns a transport failure into the reason the screen shows. */
    private fun Throwable.toAiFailure(): AiFailure {
        val ai = this as? AiException
        val kind = when (val status = ai?.status) {
            null -> AiFailureKind.UNREACHABLE
            401, 403 -> AiFailureKind.REJECTED
            402 -> AiFailureKind.NO_CREDIT
            404 -> AiFailureKind.NOT_FOUND
            429 -> AiFailureKind.RATE_LIMITED
            in 500..599 -> AiFailureKind.PROVIDER_ERROR
            else -> AiFailureKind.UNKNOWN
        }
        return AiFailure(kind = kind, providerMessage = ai?.providerMessage ?: message)
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
