package com.subzero.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.CalculateCategoryBreakdownUseCase
import com.subzero.core.domain.usecase.CalculateSpendSummaryUseCase
import com.subzero.core.domain.usecase.CategoryShare
import com.subzero.core.domain.usecase.GenerateInsightsUseCase
import com.subzero.core.domain.usecase.SpendSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** A subscription with its yearly equivalent, for ranked lists. */
data class RankedSubscription(val subscription: Subscription, val monthly: Money, val yearly: Money)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data object NoSubscriptions : InsightsUiState

    data class Ready(
        val today: LocalDate,
        val homeCurrency: CurrencyCode,
        val summary: SpendSummary,
        val insights: List<Insight>,
        val categories: List<CategoryShare>,
        /** Active home-currency subscriptions, most expensive first. */
        val ranked: List<RankedSubscription>,
        /** Active subscriptions grouped by declared usage; the order is rarely → daily → unknown. */
        val byUsage: Map<DeclaredUsage, List<RankedSubscription>>,
    ) : InsightsUiState {
        val unknownUsageCount: Int get() = byUsage[DeclaredUsage.UNKNOWN]?.size ?: 0
    }
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: SubscriptionRepository,
    preferences: UserPreferencesRepository,
    private val generateInsights: GenerateInsightsUseCase,
    private val calculateSpendSummary: CalculateSpendSummaryUseCase,
    private val calculateCategoryBreakdown: CalculateCategoryBreakdownUseCase,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeSubscriptions(),
        preferences.preferences,
        repository.observeAllPriceChanges(),
    ) { subscriptions, prefs, priceChanges ->
        if (subscriptions.isEmpty()) {
            InsightsUiState.NoSubscriptions
        } else {
            val currency = prefs.homeCurrency
            val ranked = subscriptions
                .filter { it.isActive && it.price.currency == currency }
                .map {
                    RankedSubscription(
                        subscription = it,
                        monthly = BillingSchedule.monthlyEquivalent(it.price, it.billingCycle),
                        yearly = BillingSchedule.yearlyEquivalent(it.price, it.billingCycle),
                    )
                }
                .sortedByDescending { it.monthly.amountMinor }
            InsightsUiState.Ready(
                today = LocalDate.now(clock),
                homeCurrency = currency,
                summary = calculateSpendSummary(subscriptions, currency),
                insights = generateInsights(subscriptions, priceChanges, currency),
                categories = calculateCategoryBreakdown(subscriptions, currency),
                ranked = ranked,
                byUsage = usageOrder.associateWith { usage -> ranked.filter { it.subscription.usage == usage } }
                    .filterValues { it.isNotEmpty() },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = InsightsUiState.Loading,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val usageOrder = listOf(DeclaredUsage.RARELY, DeclaredUsage.MONTHLY, DeclaredUsage.WEEKLY, DeclaredUsage.DAILY, DeclaredUsage.UNKNOWN)
    }
}
