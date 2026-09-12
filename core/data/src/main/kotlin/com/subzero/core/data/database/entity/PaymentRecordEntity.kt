package com.subzero.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** One charge. The unique (subscription, date) index is what makes the rollover idempotent. */
@Entity(
    tableName = "payment_records",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscription_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["subscription_id", "paid_on"], unique = true),
        Index(value = ["paid_on"]),
    ],
)
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "subscription_id") val subscriptionId: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "paid_on") val paidOn: LocalDate,
    val source: String,
)
