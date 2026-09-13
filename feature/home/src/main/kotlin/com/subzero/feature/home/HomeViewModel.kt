package com.subzero.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.CalculatePotentialSavingsUseCase
import com.subzero.core.domain.usecase.CalculateSpendSummaryUseCase
import com.subzero.core.domain.usecase.CalculateSpendTrendUseCase
import com.subzero.core.domain.usecase.GenerateInsightsUseCase
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.domain.usecase.PotentialSavings
import com.subzero.core.domain.usecase.SpendSummary
import com.subzero.core.domain.usecase.SpendTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** No subscriptions yet: the dashboard has nothing to show but the invitation. */
    data class Empty(val displayName: String?) : HomeUiState

    data class Dashboard(
        val displayName: String?,
        val today: LocalDate,
        val homeCurrency: CurrencyCode,
        val summary: SpendSummary,
        val nextPayment: UpcomingPayment?,
        val trend: SpendTrend,
        val savings: PotentialSavings,
        /** The single most relevant insight, if any. */
        val insight: Insight?,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: SubscriptionRepository,
    preferences: UserPreferencesRepository,
    private val calculateSpendSummary: CalculateSpendSummaryUseCase,
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val calculateSpendTrend: CalculateSpendTrendUseCase,
    private val calculatePotentialSavings: CalculatePotentialSavingsUseCase,
    private val generateInsights: GenerateInsightsUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)
    private val trendStart: LocalDate = YearMonth.from(today).minusMonths((TREND_MONTHS - 1).toLong()).atDay(1)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSubscriptions(),
        preferences.preferences,
        repository.observeAllPriceChanges(),
        repository.observePaymentRecordsBetween(trendStart, today),
    ) { subscriptions, prefs, priceChanges, records ->
        if (subscriptions.isEmpty()) {
            HomeUiState.Empty(prefs.displayName)
        } else {
            val currency = prefs.homeCurrency
            HomeUiState.Dashboard(
                displayName = prefs.displayName,
                today = today,
                homeCurrency = currency,
                summary = calculateSpendSummary(subscriptions, currency),
                nextPayment = getUpcomingPayments.next(subscriptions, from = today),
                trend = calculateSpendTrend(subscriptions, priceChanges, records, currency, months = TREND_MONTHS),
                savings = calculatePotentialSavings(subscriptions, currency),
                insight = generateInsights(subscriptions, priceChanges, currency).firstOrNull { it !is Insight.PotentialSavings },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState.Loading,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TREND_MONTHS = 6
    }
}
