package com.subzero.feature.subscriptions.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.usecase.CalculateSpendingHistoryUseCase
import com.subzero.core.domain.usecase.DeleteSubscriptionUseCase
import com.subzero.core.domain.usecase.SetDeclaredUsageUseCase
import com.subzero.core.domain.usecase.SetSubscriptionStatusUseCase
import com.subzero.core.domain.usecase.SpendingHistory
import com.subzero.core.navigation.SubscriptionDetailKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

sealed interface SubscriptionDetailUiState {
    data object Loading : SubscriptionDetailUiState

    /** The subscription no longer exists (deleted here or elsewhere); the screen should close. */
    data object Gone : SubscriptionDetailUiState

    data class Loaded(
        val subscription: Subscription,
        val monthlyEquivalent: Money,
        val yearlyEquivalent: Money,
        val history: SpendingHistory,
        val priceHistory: List<PriceChange>,
        val today: LocalDate,
        val isBusy: Boolean = false,
        val actionFailed: Boolean = false,
    ) : SubscriptionDetailUiState {
        val isActive: Boolean get() = subscription.status == SubscriptionStatus.ACTIVE
        val isCanceled: Boolean get() = subscription.status == SubscriptionStatus.CANCELED
    }
}

@HiltViewModel(assistedFactory = SubscriptionDetailViewModel.Factory::class)
class SubscriptionDetailViewModel @AssistedInject constructor(
    @Assisted key: SubscriptionDetailKey,
    private val repository: SubscriptionRepository,
    private val calculateSpendingHistory: CalculateSpendingHistoryUseCase,
    private val setStatus: SetSubscriptionStatusUseCase,
    private val setDeclaredUsage: SetDeclaredUsageUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: SubscriptionDetailKey): SubscriptionDetailViewModel
    }

    private val id = SubscriptionId(key.subscriptionId)
    private val busy = MutableStateFlow(false)
    private val actionFailed = MutableStateFlow(false)

    val uiState: StateFlow<SubscriptionDetailUiState> = combine(
        repository.observeSubscription(id),
        repository.observePriceHistory(id),
        repository.observePaymentRecords(id),
        busy,
        actionFailed,
    ) { subscription, prices, payments, isBusy, failed ->
        toUiState(subscription, prices, payments, isBusy, failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SubscriptionDetailUiState.Loading,
    )

    fun pause() = changeStatus(SubscriptionStatus.PAUSED)

    fun resume() = changeStatus(SubscriptionStatus.ACTIVE)

    fun markCanceled() = changeStatus(SubscriptionStatus.CANCELED)

    fun setUsage(usage: DeclaredUsage) = perform { setDeclaredUsage(id, usage) }

    /** Deletes; the state becomes [SubscriptionDetailUiState.Gone] through the observed flow. */
    fun delete() = perform { deleteSubscription(id) }

    fun dismissActionError() {
        actionFailed.value = false
    }

    private fun changeStatus(status: SubscriptionStatus) = perform { setStatus(id, status) }

    private fun toUiState(
        subscription: Subscription?,
        prices: List<PriceChange>,
        payments: List<PaymentRecord>,
        isBusy: Boolean,
        failed: Boolean,
    ): SubscriptionDetailUiState {
        if (subscription == null) return SubscriptionDetailUiState.Gone
        return SubscriptionDetailUiState.Loaded(
            subscription = subscription,
            monthlyEquivalent = BillingSchedule.monthlyEquivalent(subscription.price, subscription.billingCycle),
            yearlyEquivalent = BillingSchedule.yearlyEquivalent(subscription.price, subscription.billingCycle),
            history = calculateSpendingHistory(subscription, prices, payments),
            priceHistory = prices,
            today = LocalDate.now(clock),
            isBusy = isBusy,
            actionFailed = failed,
        )
    }

    private fun perform(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        actionFailed.value = false
        viewModelScope.launch {
            runCatching { action() }.onFailure { actionFailed.value = true }
            busy.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
