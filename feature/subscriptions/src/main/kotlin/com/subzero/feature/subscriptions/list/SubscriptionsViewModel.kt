package com.subzero.feature.subscriptions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.usecase.FilterSubscriptionsUseCase
import com.subzero.core.domain.usecase.SearchSubscriptionsUseCase
import com.subzero.core.domain.usecase.SortSubscriptionsUseCase
import com.subzero.core.domain.usecase.SubscriptionFilter
import com.subzero.core.domain.usecase.SubscriptionSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val searchActive: Boolean = false,
    val filter: SubscriptionFilter = SubscriptionFilter.All,
    val sort: SubscriptionSort = SubscriptionSort.NEXT_PAYMENT,
    /** After search, filter and sort. */
    val items: List<Subscription> = emptyList(),
    /** Before any narrowing, to tell "nothing yet" apart from "no matches". */
    val totalCount: Int = 0,
    val today: LocalDate = LocalDate.MIN,
) {
    val isEmpty: Boolean get() = !isLoading && totalCount == 0
    val hasNoMatches: Boolean get() = !isLoading && totalCount > 0 && items.isEmpty()
    val isNarrowed: Boolean get() = query.isNotBlank() || filter != SubscriptionFilter.All
}

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    repository: SubscriptionRepository,
    private val search: SearchSubscriptionsUseCase,
    private val filterUseCase: FilterSubscriptionsUseCase,
    private val sortUseCase: SortSubscriptionsUseCase,
    private val clock: Clock,
) : ViewModel() {

    private data class Controls(
        val query: String = "",
        val searchActive: Boolean = false,
        val filter: SubscriptionFilter = SubscriptionFilter.All,
        val sort: SubscriptionSort = SubscriptionSort.NEXT_PAYMENT,
    )

    private val controls = MutableStateFlow(Controls())

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        repository.observeSubscriptions(),
        controls,
    ) { subscriptions, c ->
        val narrowed = sortUseCase(filterUseCase(search(subscriptions, c.query), c.filter), c.sort)
        SubscriptionsUiState(
            isLoading = false,
            query = c.query,
            searchActive = c.searchActive,
            filter = c.filter,
            sort = c.sort,
            items = narrowed,
            totalCount = subscriptions.size,
            today = LocalDate.now(clock),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SubscriptionsUiState(today = LocalDate.now(clock)),
    )

    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun setSearchActive(active: Boolean) = controls.update {
        it.copy(searchActive = active, query = if (active) it.query else "")
    }

    fun setFilter(filter: SubscriptionFilter) = controls.update { it.copy(filter = filter) }

    fun setSort(sort: SubscriptionSort) = controls.update { it.copy(sort = sort) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
