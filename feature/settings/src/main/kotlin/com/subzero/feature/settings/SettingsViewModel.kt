package com.subzero.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.notifications.NotificationScheduler
import com.subzero.core.notifications.SubzeroNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val preferences: UserPreferences,
        /** Whether the OS currently lets SUBZERO post notifications. */
        val notificationsAllowed: Boolean,
    ) : SettingsUiState {
        val anyNotificationEnabled: Boolean
            get() = preferences.notifications.let { it.upcomingChargeEnabled || it.monthlySummaryEnabled || it.savingsInsightsEnabled }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val notifier: SubzeroNotifier,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    private val permissionState = MutableStateFlow(notifier.areNotificationsEnabled)

    val uiState: StateFlow<SettingsUiState> = combine(preferences.preferences, permissionState) { prefs, allowed ->
        SettingsUiState.Ready(preferences = prefs, notificationsAllowed = allowed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState.Loading,
    )

    /** Called when the screen resumes or a permission request returns, since the OS state can change outside the app. */
    fun refreshPermission() {
        permissionState.value = notifier.areNotificationsEnabled
    }

    fun setUpcomingEnabled(enabled: Boolean) = updateNotifications { it.copy(upcomingChargeEnabled = enabled) }

    fun setUpcomingDaysBefore(days: Int) = updateNotifications { it.copy(upcomingChargeDaysBefore = days) }

    fun setMonthlySummaryEnabled(enabled: Boolean) = updateNotifications { it.copy(monthlySummaryEnabled = enabled) }

    fun setSavingsInsightsEnabled(enabled: Boolean) = updateNotifications { it.copy(savingsInsightsEnabled = enabled) }

    private fun updateNotifications(transform: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            val updated = transform(preferences.preferences.first().notifications)
            preferences.setNotificationPreferences(updated)
            scheduler.ensureScheduled()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
