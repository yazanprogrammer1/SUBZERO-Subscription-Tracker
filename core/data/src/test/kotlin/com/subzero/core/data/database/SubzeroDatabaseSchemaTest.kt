package com.subzero.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Guards the exported schema for the current version.
 *
 * Room writes the identity hash of the schema it compiled into `room_master_table`; the
 * exported JSON carries the same hash. If an entity changes without a version bump + new
 * export, the two diverge and this test fails before the app ever crashes on a device.
 *
 * When version 2 arrives, add a migration test that creates v1 from `schemas/`, inserts rows,
 * migrates, and checks the rows survived.
 */
@RunWith(RobolectricTestRunner::class)
class SubzeroDatabaseSchemaTest {

    private lateinit var database: SubzeroDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SubzeroDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `exported schema for the current version is committed and matches the entities`() {
        val schemaFile = File("schemas/${SubzeroDatabase::class.java.name}/${SubzeroDatabase.VERSION}.json")
        assertThat(schemaFile.exists()).isTrue()

        val exported = JSONObject(schemaFile.readText()).getJSONObject("database")
        assertThat(exported.getInt("version")).isEqualTo(SubzeroDatabase.VERSION)

        val compiledHash = database.openHelper.readableDatabase
            .query("SELECT identity_hash FROM room_master_table LIMIT 1")
            .use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                cursor.getString(0)
            }
        assertThat(exported.getString("identityHash")).isEqualTo(compiledHash)
    }

    @Test
    fun `payment records are unique per subscription and date so rollover is idempotent`() {
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO subscriptions (id, name, amount_minor, currency, cycle_every, cycle_unit, anchor_date, " +
                "next_billing_date, category, status, usage, notes, created_at, updated_at, status_changed_at) " +
                "VALUES ('s', 'Netflix', 1549, 'USD', 1, 'MONTH', '2026-09-16', '2026-09-16', 'ENTERTAINMENT', " +
                "'ACTIVE', 'UNKNOWN', NULL, 0, 0, NULL)",
        )
        db.execSQL(
            "INSERT INTO payment_records (id, subscription_id, amount_minor, currency, paid_on, source) " +
                "VALUES ('p1', 's', 1549, 'USD', '2026-09-16', 'RECORDED')",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO payment_records (id, subscription_id, amount_minor, currency, paid_on, source) " +
                "VALUES ('p2', 's', 1549, 'USD', '2026-09-16', 'RECORDED')",
        )

        db.query("SELECT COUNT(*) FROM payment_records").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun `deleting a subscription cascades through foreign keys`() {
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO subscriptions (id, name, amount_minor, currency, cycle_every, cycle_unit, anchor_date, " +
                "next_billing_date, category, status, usage, notes, created_at, updated_at, status_changed_at) " +
                "VALUES ('s', 'Netflix', 1549, 'USD', 1, 'MONTH', '2026-09-16', '2026-09-16', 'ENTERTAINMENT', " +
                "'ACTIVE', 'UNKNOWN', NULL, 0, 0, NULL)",
        )
        db.execSQL(
            "INSERT INTO price_changes (id, subscription_id, amount_minor, currency, effective_from) " +
                "VALUES ('c1', 's', 1549, 'USD', '2026-09-16')",
        )
        db.execSQL("DELETE FROM subscriptions WHERE id = 's'")

        db.query("SELECT COUNT(*) FROM price_changes").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(0)
        }
    }
}
