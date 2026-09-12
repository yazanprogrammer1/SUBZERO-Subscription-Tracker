package com.subzero.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.common.coroutines.ApplicationScope
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.RollForwardBillingDatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    /** Preferences not read yet; the splash screen stays up. */
    data object Loading : MainUiState

    data class Ready(
        val onboardingCompleted: Boolean,
        val themeMode: ThemeMode,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    rollForwardBillingDates: RollForwardBillingDatesUseCase,
    @ApplicationScope applicationScope: CoroutineScope,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferences.preferences
        .map { MainUiState.Ready(onboardingCompleted = it.onboardingCompleted, themeMode = it.themeMode) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MainUiState.Loading,
        )

    init {
        // Bring billing dates up to date on every launch. Runs in the application scope so a
        // quick process death does not leave a half-applied rollover behind a cancelled job.
        applicationScope.launch { rollForwardBillingDates() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
