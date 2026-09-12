package com.subzero.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the user chose on the last page; the app shell acts on it after onboarding is stored. */
enum class OnboardingOutcome { AddFirstSubscription, Skipped }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _completed = MutableStateFlow<OnboardingOutcome?>(null)

    /** Emits once onboarding has been persisted as completed. */
    val completed: StateFlow<OnboardingOutcome?> = _completed.asStateFlow()

    fun complete(outcome: OnboardingOutcome) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            _completed.value = outcome
        }
    }
}
