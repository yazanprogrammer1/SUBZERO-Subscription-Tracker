package com.subzero.core.domain.testing

import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.repository.AiSettingsRepository
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * In-memory [SubscriptionRepository] with the same semantics as the Room implementation:
 * atomic writes, cascade delete, and (subscription, date)-unique payment records.
 */
class FakeSubscriptionRepository : SubscriptionRepository {

    private val subscriptions = MutableStateFlow<Map<SubscriptionId, Subscription>>(emptyMap())
    private val priceChanges = MutableStateFlow<List<PriceChange>>(emptyList())
    private val payments = MutableStateFlow<List<PaymentRecord>>(emptyList())

    val subscriptionsSnapshot: List<Subscription> get() = subscriptions.value.values.toList()
    val priceChangesSnapshot: List<PriceChange> get() = priceChanges.value
    val paymentsSnapshot: List<PaymentRecord> get() = payments.value

    fun seed(vararg items: Subscription) {
        subscriptions.update { current -> current + items.associateBy { it.id } }
    }

    fun seedPrices(vararg items: PriceChange) = priceChanges.update { it + items }

    fun seedPayments(vararg items: PaymentRecord) = payments.update { it + items }

    override fun observeSubscriptions(): Flow<List<Subscription>> =
        subscriptions.map { it.values.sortedBy { s -> s.nextBillingDate } }

    override fun observeSubscription(id: SubscriptionId): Flow<Subscription?> =
        subscriptions.map { it[id] }

    override suspend fun getSubscriptions(): List<Subscription> =
        subscriptions.value.values.sortedBy { it.nextBillingDate }

    override suspend fun getSubscription(id: SubscriptionId): Subscription? = subscriptions.value[id]

    override suspend fun addSubscription(subscription: Subscription, initialPrice: PriceChange) {
        subscriptions.update { it + (subscription.id to subscription) }
        priceChanges.update { it + initialPrice }
    }

    override suspend fun updateSubscription(subscription: Subscription, priceChange: PriceChange?) {
        subscriptions.update { it + (subscription.id to subscription) }
        if (priceChange != null) priceChanges.update { it + priceChange }
    }

    override suspend fun deleteSubscription(id: SubscriptionId) {
        subscriptions.update { it - id }
        priceChanges.update { list -> list.filterNot { it.subscriptionId == id } }
        payments.update { list -> list.filterNot { it.subscriptionId == id } }
    }

    override suspend fun deleteAll() {
        subscriptions.value = emptyMap()
        priceChanges.value = emptyList()
        payments.value = emptyList()
    }

    override fun observePriceHistory(id: SubscriptionId): Flow<List<PriceChange>> =
        priceChanges.map { list -> list.filter { it.subscriptionId == id }.sortedBy { it.effectiveFrom } }

    override suspend fun getPriceHistory(id: SubscriptionId): List<PriceChange> =
        priceChanges.value.filter { it.subscriptionId == id }.sortedBy { it.effectiveFrom }

    override fun observeAllPriceChanges(): Flow<List<PriceChange>> =
        priceChanges.map { list -> list.sortedBy { it.effectiveFrom } }

    override fun observePaymentRecordsBetween(from: LocalDate, to: LocalDate): Flow<List<PaymentRecord>> =
        payments.map { list -> list.filter { it.paidOn in from..to }.sortedBy { it.paidOn } }

    override fun observePaymentRecords(id: SubscriptionId): Flow<List<PaymentRecord>> =
        payments.map { list -> list.filter { it.subscriptionId == id }.sortedBy { it.paidOn } }

    override suspend fun getPaymentRecords(id: SubscriptionId): List<PaymentRecord> =
        payments.value.filter { it.subscriptionId == id }.sortedBy { it.paidOn }

    override suspend fun getPaymentRecordsBetween(from: LocalDate, to: LocalDate): List<PaymentRecord> =
        payments.value.filter { it.paidOn in from..to }.sortedBy { it.paidOn }

    override suspend fun applyRollover(records: List<PaymentRecord>, subscriptions: List<Subscription>) {
        payments.update { existing ->
            val taken = existing.map { it.subscriptionId to it.paidOn }.toHashSet()
            existing + records.filter { (it.subscriptionId to it.paidOn) !in taken }
        }
        this.subscriptions.update { it + subscriptions.associateBy { s -> s.id } }
    }
}

class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(
        homeCurrency = CurrencyCode.USD,
        themeMode = ThemeMode.SYSTEM,
        notifications = NotificationPreferences.Default,
        onboardingCompleted = false,
        displayName = null,
    ),
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = state

    override suspend fun setHomeCurrency(currency: CurrencyCode) = state.update { it.copy(homeCurrency = currency) }

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setNotificationPreferences(preferences: NotificationPreferences) =
        state.update { it.copy(notifications = preferences) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        state.update { it.copy(onboardingCompleted = completed) }

    override suspend fun setDisplayName(name: String?) = state.update { it.copy(displayName = name) }

    override suspend fun setAiEnhancedEnabled(enabled: Boolean) = state.update { it.copy(aiEnhancedEnabled = enabled) }

    override suspend fun clear() = state.update {
        UserPreferences(
            homeCurrency = it.homeCurrency,
            themeMode = ThemeMode.SYSTEM,
            notifications = NotificationPreferences.Default,
            onboardingCompleted = false,
            displayName = null,
        )
    }
}

/** In-memory [AiSettingsRepository]; a blank key on update keeps the stored one, like the real one. */
class FakeAiSettingsRepository(initial: AiSettings = AiSettings.Empty) : AiSettingsRepository {

    private val state = MutableStateFlow(initial)

    override val settings: Flow<AiSettings> = state

    override suspend fun update(settings: AiSettings) = state.update { stored ->
        settings.copy(apiKey = settings.apiKey.ifBlank { stored.apiKey })
    }

    override suspend fun clear() = state.update { AiSettings.Empty }
}
