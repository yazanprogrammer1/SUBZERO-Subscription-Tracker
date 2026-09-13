package com.subzero.core.data.repository

import com.subzero.core.common.coroutines.Dispatcher
import com.subzero.core.common.coroutines.SubzeroDispatcher
import com.subzero.core.data.database.dao.SubscriptionDao
import com.subzero.core.data.database.toDomain
import com.subzero.core.data.database.toEntity
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
    @Dispatcher(SubzeroDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : SubscriptionRepository {

    override fun observeSubscriptions(): Flow<List<Subscription>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeSubscription(id: SubscriptionId): Flow<Subscription?> =
        dao.observeById(id.value).map { it?.toDomain() }.flowOn(ioDispatcher)

    override suspend fun getSubscriptions(): List<Subscription> = withContext(ioDispatcher) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun getSubscription(id: SubscriptionId): Subscription? = withContext(ioDispatcher) {
        dao.getById(id.value)?.toDomain()
    }

    override suspend fun addSubscription(subscription: Subscription, initialPrice: PriceChange) {
        require(initialPrice.subscriptionId == subscription.id)
        withContext(ioDispatcher) {
            dao.insertWithInitialPrice(subscription.toEntity(), initialPrice.toEntity())
        }
    }

    override suspend fun updateSubscription(subscription: Subscription, priceChange: PriceChange?) {
        require(priceChange == null || priceChange.subscriptionId == subscription.id)
        withContext(ioDispatcher) {
            dao.updateWithPriceChange(subscription.toEntity(), priceChange?.toEntity())
        }
    }

    override suspend fun deleteSubscription(id: SubscriptionId) = withContext(ioDispatcher) {
        dao.deleteById(id.value)
    }

    override fun observePriceHistory(id: SubscriptionId): Flow<List<PriceChange>> =
        dao.observePriceChanges(id.value).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getPriceHistory(id: SubscriptionId): List<PriceChange> = withContext(ioDispatcher) {
        dao.getPriceChanges(id.value).map { it.toDomain() }
    }

    override fun observeAllPriceChanges(): Flow<List<PriceChange>> =
        dao.observeAllPriceChanges().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observePaymentRecordsBetween(from: LocalDate, to: LocalDate): Flow<List<PaymentRecord>> =
        dao.observePaymentRecordsBetween(from, to).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observePaymentRecords(id: SubscriptionId): Flow<List<PaymentRecord>> =
        dao.observePaymentRecords(id.value).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getPaymentRecords(id: SubscriptionId): List<PaymentRecord> = withContext(ioDispatcher) {
        dao.getPaymentRecords(id.value).map { it.toDomain() }
    }

    override suspend fun getPaymentRecordsBetween(from: LocalDate, to: LocalDate): List<PaymentRecord> =
        withContext(ioDispatcher) {
            dao.getPaymentRecordsBetween(from, to).map { it.toDomain() }
        }

    override suspend fun applyRollover(records: List<PaymentRecord>, subscriptions: List<Subscription>) {
        withContext(ioDispatcher) {
            dao.applyRollover(records.map { it.toEntity() }, subscriptions.map { it.toEntity() })
        }
    }
}
