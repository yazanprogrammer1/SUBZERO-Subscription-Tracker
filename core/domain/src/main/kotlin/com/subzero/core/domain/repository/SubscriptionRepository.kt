package com.subzero.core.domain.repository

import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Local source of truth for subscriptions, their price history and their payment records.
 * Implemented in `core:data`; the domain never sees Room.
 */
interface SubscriptionRepository {

    fun observeSubscriptions(): Flow<List<Subscription>>

    fun observeSubscription(id: SubscriptionId): Flow<Subscription?>

    suspend fun getSubscriptions(): List<Subscription>

    suspend fun getSubscription(id: SubscriptionId): Subscription?

    /** Inserts the subscription together with its first [PriceChange], atomically. */
    suspend fun addSubscription(subscription: Subscription, initialPrice: PriceChange)

    /** Updates the subscription and, when given, appends a new [PriceChange] in one transaction. */
    suspend fun updateSubscription(subscription: Subscription, priceChange: PriceChange? = null)

    /** Deletes the subscription and everything that references it. */
    suspend fun deleteSubscription(id: SubscriptionId)

    fun observePriceHistory(id: SubscriptionId): Flow<List<PriceChange>>

    suspend fun getPriceHistory(id: SubscriptionId): List<PriceChange>

    fun observePaymentRecords(id: SubscriptionId): Flow<List<PaymentRecord>>

    suspend fun getPaymentRecords(id: SubscriptionId): List<PaymentRecord>

    /** All payment records with [PaymentRecord.paidOn] in the closed range, across subscriptions. */
    suspend fun getPaymentRecordsBetween(from: LocalDate, to: LocalDate): List<PaymentRecord>

    /**
     * Applies a billing rollover atomically: inserts [records] (ignoring any that already exist
     * for the same subscription and date) and updates [subscriptions].
     */
    suspend fun applyRollover(records: List<PaymentRecord>, subscriptions: List<Subscription>)
}
