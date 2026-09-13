package com.subzero.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val month: YearMonth = YearMonth.of(2000, 1),
    /** Charges projected from the billing schedules of active subscriptions, keyed by day. */
    val paymentsByDay: Map<LocalDate, List<UpcomingPayment>> = emptyMap(),
    /** Home-currency total of the month; foreign-currency charges are counted, not summed. */
    val monthTotal: Money = Money.zero(CurrencyCode.USD),
    val foreignCurrencyCount: Int = 0,
    val selectedDay: LocalDate? = null,
    val hasSubscriptions: Boolean = false,
) {
    val selectedPayments: List<UpcomingPayment> get() = selectedDay?.let { paymentsByDay[it] }.orEmpty()
    val monthIsEmpty: Boolean get() = paymentsByDay.isEmpty()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: SubscriptionRepository,
    preferences: UserPreferencesRepository,
    private val getUpcomingPayments: GetUpcomingPaymentsUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)

    private data class Selection(val month: YearMonth, val day: LocalDate?)

    private val selection = MutableStateFlow(Selection(month = YearMonth.from(today), day = today))

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.observeSubscriptions(),
        preferences.preferences,
        selection,
    ) { subscriptions, prefs, sel ->
        val payments = getUpcomingPayments(subscriptions, from = sel.month.atDay(1), to = sel.month.atEndOfMonth())
        val home = prefs.homeCurrency
        CalendarUiState(
            isLoading = false,
            today = today,
            month = sel.month,
            paymentsByDay = payments.groupBy { it.date },
            monthTotal = payments.filter { it.amount.currency == home }.fold(Money.zero(home)) { acc, p -> acc + p.amount },
            foreignCurrencyCount = payments.count { it.amount.currency != home },
            selectedDay = sel.day,
            hasSubscriptions = subscriptions.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CalendarUiState(today = today, month = YearMonth.from(today), selectedDay = today),
    )

    fun showMonth(month: YearMonth) = selection.update {
        Selection(month = month, day = if (YearMonth.from(today) == month) today else null)
    }

    fun nextMonth() = showMonth(selection.value.month.plusMonths(1))

    fun previousMonth() = showMonth(selection.value.month.minusMonths(1))

    fun goToToday() = selection.update { Selection(month = YearMonth.from(today), day = today) }

    fun selectDay(day: LocalDate) = selection.update { current ->
        current.copy(day = if (current.day == day) null else day)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
