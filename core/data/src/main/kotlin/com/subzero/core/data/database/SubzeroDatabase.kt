package com.subzero.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.subzero.core.data.database.converter.TimeConverters
import com.subzero.core.data.database.dao.SubscriptionDao
import com.subzero.core.data.database.entity.PaymentRecordEntity
import com.subzero.core.data.database.entity.PriceChangeEntity
import com.subzero.core.data.database.entity.SubscriptionEntity

/**
 * The local source of truth.
 *
 * Schema versions are exported to core/data/schemas/ by the Room Gradle plugin and committed.
 * Every version bump ships with a Migration and a migration test; destructive migration is
 * never enabled.
 */
@Database(
    entities = [
        SubscriptionEntity::class,
        PriceChangeEntity::class,
        PaymentRecordEntity::class,
    ],
    version = SubzeroDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class SubzeroDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val VERSION = 1
        const val NAME = "subzero.db"
    }
}
