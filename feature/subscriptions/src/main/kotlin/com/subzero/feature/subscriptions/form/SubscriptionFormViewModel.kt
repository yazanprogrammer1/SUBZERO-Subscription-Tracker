package com.subzero.feature.subscriptions.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import com.subzero.core.domain.usecase.AddSubscriptionUseCase
import com.subzero.core.domain.usecase.SaveSubscriptionResult
import com.subzero.core.domain.usecase.SubscriptionDraft
import com.subzero.core.domain.usecase.SubscriptionValidationError
import com.subzero.core.domain.usecase.UpdateSubscriptionUseCase
import com.subzero.core.navigation.SubscriptionFormKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The reason the form cannot be shown or saved, mapped to human copy in the UI. */
enum class FormFailure { NOT_FOUND, SAVE_FAILED }

data class SubscriptionFormState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val name: String = "",
    val priceText: String = "",
    val currency: CurrencyCode = CurrencyCode.USD,
    /** The chip selection among the standard cycles; ignored while [customMode] is on. */
    val standardCycle: BillingCycle = BillingCycle.Monthly,
    val customMode: Boolean = false,
    val customEvery: Int = 2,
    val customUnit: CycleUnit = CycleUnit.MONTH,
    val nextPaymentDate: LocalDate = LocalDate.MIN,
    val firstPaymentDate: LocalDate? = null,
    val category: Category = Category.OTHER,
    val usage: DeclaredUsage = DeclaredUsage.UNKNOWN,
    val notes: String = "",
    val today: LocalDate = LocalDate.MIN,
    val errors: Set<SubscriptionValidationError> = emptySet(),
    /** Price text is non-empty but not a valid amount for the currency. */
    val priceInvalid: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Subscription? = null,
    val failure: FormFailure? = null,
    /** Lower-cased names of the other subscriptions, to warn (not block) about duplicates. */
    val otherNames: Set<String> = emptySet(),
) {
    /** True when another subscription already carries this name; adding twice is allowed but worth a heads-up. */
    val isDuplicateName: Boolean get() = name.trim().lowercase() in otherNames

    /** The effective cycle: the custom editor values when in custom mode, else the chip. */
    val cycle: BillingCycle get() = if (customMode) BillingCycle.of(customEvery, customUnit) else standardCycle

    /** The amount as typed, or null when blank/invalid. */
    val price: Money? get() = Money.parse(priceText, currency)

    /** What the schedule actually projects; shown when it differs from the entered next date. */
    val projectedNextDate: LocalDate
        get() = BillingSchedule.nextChargeOnOrAfter(firstPaymentDate ?: nextPaymentDate, cycle, today)

    val showsProjection: Boolean get() = projectedNextDate != nextPaymentDate

    val canSave: Boolean get() = !isLoading && !isSaving && saved == null && failure != FormFailure.NOT_FOUND
}

@HiltViewModel(assistedFactory = SubscriptionFormViewModel.Factory::class)
class SubscriptionFormViewModel @AssistedInject constructor(
    @Assisted private val key: SubscriptionFormKey,
    private val repository: SubscriptionRepository,
    private val preferences: UserPreferencesRepository,
    private val addSubscription: AddSubscriptionUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: SubscriptionFormKey): SubscriptionFormViewModel
    }

    private val _state = MutableStateFlow(SubscriptionFormState(isEdit = key.subscriptionId != null))
    val state: StateFlow<SubscriptionFormState> = _state.asStateFlow()

    private val editingId: SubscriptionId? = key.subscriptionId?.let(::SubscriptionId)

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val today = LocalDate.now(clock)
        val homeCurrency = preferences.preferences.first().homeCurrency
        val existing = editingId?.let { repository.getSubscription(it) }
        val otherNames = repository.getSubscriptions()
            .filter { it.id != editingId }
            .map { it.name.trim().lowercase() }
            .toSet()
        _state.update { current ->
            when {
                editingId == null -> current.copy(
                    isLoading = false,
                    currency = homeCurrency,
                    nextPaymentDate = today,
                    today = today,
                    otherNames = otherNames,
                )
                existing == null -> current.copy(isLoading = false, today = today, failure = FormFailure.NOT_FOUND)
                else -> current.copy(
                    isLoading = false,
                    name = existing.name,
                    priceText = existing.price.toMajorUnits().toPlainString(),
                    currency = existing.price.currency,
                    standardCycle = existing.billingCycle.takeUnless { it is BillingCycle.Custom } ?: current.standardCycle,
                    customMode = existing.billingCycle is BillingCycle.Custom,
                    customEvery = (existing.billingCycle as? BillingCycle.Custom)?.every ?: current.customEvery,
                    customUnit = (existing.billingCycle as? BillingCycle.Custom)?.unit ?: current.customUnit,
                    nextPaymentDate = existing.nextBillingDate,
                    firstPaymentDate = existing.anchorDate.takeIf { it != existing.nextBillingDate },
                    category = existing.category,
                    usage = existing.usage,
                    notes = existing.notes.orEmpty(),
                    today = today,
                    otherNames = otherNames,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, errors = it.errors - SubscriptionValidationError.NAME_BLANK - SubscriptionValidationError.NAME_TOO_LONG) }

    fun setPriceText(value: String) = _state.update {
        it.copy(priceText = value, priceInvalid = false, errors = it.errors - SubscriptionValidationError.PRICE_MISSING)
    }

    fun setCurrency(value: CurrencyCode) = _state.update { it.copy(currency = value, priceInvalid = false) }

    fun setStandardCycle(value: BillingCycle) {
        require(value !is BillingCycle.Custom)
        _state.update { it.copy(standardCycle = value, customMode = false) }
    }

    fun setCustomMode(enabled: Boolean) = _state.update { it.copy(customMode = enabled) }

    fun setCustomEvery(value: Int) = _state.update { it.copy(customEvery = value.coerceIn(1, BillingCycle.MAX_EVERY)) }

    fun setCustomUnit(value: CycleUnit) = _state.update { it.copy(customUnit = value) }

    fun setNextPaymentDate(value: LocalDate) = _state.update {
        it.copy(nextPaymentDate = value, errors = it.errors - SubscriptionValidationError.FIRST_PAYMENT_AFTER_NEXT)
    }

    fun setFirstPaymentDate(value: LocalDate?) = _state.update {
        it.copy(firstPaymentDate = value, errors = it.errors - SubscriptionValidationError.FIRST_PAYMENT_AFTER_NEXT)
    }

    fun setCategory(value: Category) = _state.update { it.copy(category = value) }

    fun setUsage(value: DeclaredUsage) = _state.update { it.copy(usage = value) }

    fun setNotes(value: String) = _state.update { it.copy(notes = value, errors = it.errors - SubscriptionValidationError.NOTES_TOO_LONG) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val price = current.price
        if (price == null && current.priceText.isNotBlank()) {
            _state.update { it.copy(priceInvalid = true) }
            return
        }
        val draft = SubscriptionDraft(
            name = current.name,
            price = price,
            billingCycle = current.cycle,
            nextPaymentDate = current.nextPaymentDate,
            firstPaymentDate = current.firstPaymentDate,
            category = current.category,
            usage = current.usage,
            notes = current.notes.ifBlank { null },
        )
        _state.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            val result = runCatching {
                val id = editingId
                if (id == null) addSubscription(draft) else updateSubscription(id, draft)
            }.getOrElse { SaveFailed }
            _state.update { s ->
                when (result) {
                    is SaveSubscriptionResult.Saved -> s.copy(isSaving = false, saved = result.subscription)
                    is SaveSubscriptionResult.Invalid -> s.copy(isSaving = false, errors = result.errors)
                    SaveSubscriptionResult.NotFound -> s.copy(isSaving = false, failure = FormFailure.NOT_FOUND)
                    else -> s.copy(isSaving = false, failure = FormFailure.SAVE_FAILED)
                }
            }
        }
    }

    fun dismissFailure() = _state.update { it.copy(failure = null) }

    /** Sentinel for an unexpected exception from the persistence layer. */
    private object SaveFailed
}
