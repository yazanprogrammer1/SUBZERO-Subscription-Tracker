package com.subzero.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.subzero.core.data.database.entity.PaymentRecordEntity
import com.subzero.core.data.database.entity.PriceChangeEntity
import com.subzero.core.data.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY next_billing_date ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun observeById(id: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions ORDER BY next_billing_date ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subscription: SubscriptionEntity)

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Upsert
    suspend fun upsertAll(subscriptions: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    // --- price history ---

    @Query("SELECT * FROM price_changes WHERE subscription_id = :subscriptionId ORDER BY effective_from ASC")
    fun observePriceChanges(subscriptionId: String): Flow<List<PriceChangeEntity>>

    @Query("SELECT * FROM price_changes WHERE subscription_id = :subscriptionId ORDER BY effective_from ASC")
    suspend fun getPriceChanges(subscriptionId: String): List<PriceChangeEntity>

    @Query("SELECT * FROM price_changes ORDER BY effective_from ASC")
    fun observeAllPriceChanges(): Flow<List<PriceChangeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPriceChange(priceChange: PriceChangeEntity)

    // --- payment records ---

    @Query("SELECT * FROM payment_records WHERE subscription_id = :subscriptionId ORDER BY paid_on ASC")
    fun observePaymentRecords(subscriptionId: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE subscription_id = :subscriptionId ORDER BY paid_on ASC")
    suspend fun getPaymentRecords(subscriptionId: String): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records WHERE paid_on BETWEEN :from AND :to ORDER BY paid_on ASC")
    suspend fun getPaymentRecordsBetween(from: LocalDate, to: LocalDate): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records WHERE paid_on BETWEEN :from AND :to ORDER BY paid_on ASC")
    fun observePaymentRecordsBetween(from: LocalDate, to: LocalDate): Flow<List<PaymentRecordEntity>>

    /** Ignores rows that collide on the unique (subscription_id, paid_on) index. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentRecordsIgnoringDuplicates(records: List<PaymentRecordEntity>)

    // --- transactions ---

    @Transaction
    suspend fun insertWithInitialPrice(subscription: SubscriptionEntity, initialPrice: PriceChangeEntity) {
        insert(subscription)
        insertPriceChange(initialPrice)
    }

    @Transaction
    suspend fun updateWithPriceChange(subscription: SubscriptionEntity, priceChange: PriceChangeEntity?) {
        update(subscription)
        if (priceChange != null) insertPriceChange(priceChange)
    }

    @Transaction
    suspend fun applyRollover(records: List<PaymentRecordEntity>, subscriptions: List<SubscriptionEntity>) {
        insertPaymentRecordsIgnoringDuplicates(records)
        upsertAll(subscriptions)
    }
}
