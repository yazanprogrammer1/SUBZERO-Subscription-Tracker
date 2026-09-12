package com.subzero.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Room row for a subscription. Money is stored as minor units + currency code; the billing
 * cycle as (every, unit); dates as ISO strings (sortable) and instants as epoch millis.
 */
@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["next_billing_date"]),
        Index(value = ["status"]),
    ],
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "cycle_every") val cycleEvery: Int,
    @ColumnInfo(name = "cycle_unit") val cycleUnit: String,
    @ColumnInfo(name = "anchor_date") val anchorDate: LocalDate,
    @ColumnInfo(name = "next_billing_date") val nextBillingDate: LocalDate,
    val category: String,
    val status: String,
    val usage: String,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "status_changed_at") val statusChangedAt: Instant?,
)
