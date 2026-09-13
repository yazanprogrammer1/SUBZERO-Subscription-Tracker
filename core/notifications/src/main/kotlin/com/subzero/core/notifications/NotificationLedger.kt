package com.subzero.core.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the DataStore that backs the ledger, separate from user preferences. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LedgerDataStore

/** The notification kinds the ledger tracks. */
enum class NotificationKind { UPCOMING, MONTHLY_SUMMARY, SAVINGS }

/**
 * Remembers what was already sent so a day with two worker runs (periodic + app launch) never
 * notifies twice. Keyed per kind and, for upcoming charges, per subscription and charge date.
 */
interface NotificationLedger {
    /** True when this kind/key was sent at any point still in the ledger (see [prune]). */
    suspend fun wasSent(kind: NotificationKind, key: String): Boolean
    suspend fun markSent(kind: NotificationKind, key: String, on: LocalDate)

    /** Drops entries older than [keepAfter] so the store does not grow forever. */
    suspend fun prune(keepAfter: LocalDate)
    suspend fun clear()
}

@Singleton
class DataStoreNotificationLedger @Inject constructor(
    @LedgerDataStore private val dataStore: DataStore<Preferences>,
) : NotificationLedger {
    override suspend fun wasSent(kind: NotificationKind, key: String): Boolean =
        dataStore.data.first().contains(prefKey(kind, key))

    override suspend fun markSent(kind: NotificationKind, key: String, on: LocalDate) {
        dataStore.edit { it[prefKey(kind, key)] = on.toString() }
    }

    override suspend fun prune(keepAfter: LocalDate) {
        dataStore.edit { prefs ->
            prefs.asMap().forEach { (key, value) ->
                val date = (value as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (date == null || date.isBefore(keepAfter)) prefs.remove(key)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun prefKey(kind: NotificationKind, key: String) = stringPreferencesKey("${kind.name}:$key")
}
