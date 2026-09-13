package com.subzero.feature.settings

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.data.export.DataManager
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.data.export.StorageInfo
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.subscription
import com.subzero.core.notifications.NotificationKind
import com.subzero.core.notifications.NotificationLedger
import com.subzero.core.notifications.NotificationScheduler
import com.subzero.core.notifications.Notifier
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()
    private val subscriptions = FakeSubscriptionRepository()
    private var scheduled = 0
    private var cancelled = 0
    private val scheduler = object : NotificationScheduler {
        override fun ensureScheduled() { scheduled++ }
        override fun cancel() { cancelled++ }
    }
    private val ledger = object : NotificationLedger {
        var cleared = false
        override suspend fun wasSent(kind: NotificationKind, key: String) = false
        override suspend fun markSent(kind: NotificationKind, key: String, on: LocalDate) = Unit
        override suspend fun prune(keepAfter: LocalDate) = Unit
        override suspend fun clear() { cleared = true }
    }
    private val notifier = object : Notifier {
        override val areNotificationsEnabled = true
        override fun postUpcomingCharges(payments: List<UpcomingPayment>, today: LocalDate) = Unit
        override fun postMonthlySummary(month: YearMonth, total: com.subzero.core.domain.model.Money, subscriptionCount: Int) = Unit
        override fun postSavings(monthly: com.subzero.core.domain.model.Money, subscriptionCount: Int) = Unit
    }

    private class FakeDataManager(private val failWrite: Boolean = false) : DataManager {
        var written: Pair<Uri, ExportFormat>? = null
        override suspend fun writeExport(uri: Uri, format: ExportFormat) {
            if (failWrite) error("boom")
            written = uri to format
        }
        override suspend fun storageInfo() = StorageInfo(databaseBytes = 12_000, subscriptionCount = 2)
    }

    private fun viewModel(dataManager: DataManager = FakeDataManager()) = SettingsViewModel(
        preferences = preferences,
        subscriptions = subscriptions,
        notifier = notifier,
        scheduler = scheduler,
        ledger = ledger,
        dataManager = dataManager,
        appInfo = AppInfo("0.1.0"),
    )

    @Test
    fun `ready state carries preferences storage and version`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem() as SettingsUiState.Ready
            assertThat(state.appVersion).isEqualTo("0.1.0")
            assertThat(state.notificationsAllowed).isTrue()
            assertThat(state.storage?.subscriptionCount).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preference setters persist`() = runTest {
        val vm = viewModel()
        vm.setDisplayName("Alex")
        vm.setHomeCurrency(CurrencyCode.EUR)
        vm.setThemeMode(ThemeMode.LIGHT)
        vm.setUpcomingEnabled(true)
        vm.setUpcomingDaysBefore(3)
        val prefs = preferences.preferences.first()
        assertThat(prefs.displayName).isEqualTo("Alex")
        assertThat(prefs.homeCurrency).isEqualTo(CurrencyCode.EUR)
        assertThat(prefs.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(prefs.notifications.upcomingChargeEnabled).isTrue()
        assertThat(prefs.notifications.upcomingChargeDaysBefore).isEqualTo(3)
        assertThat(scheduled).isEqualTo(2)
    }

    @Test
    fun `export writes to the chosen uri and reports success`() = runTest {
        val manager = FakeDataManager()
        val vm = viewModel(manager)
        vm.uiState.test {
            awaitItem()
            vm.exportTo(Uri.parse("content://docs/export.json"), ExportFormat.JSON)
            assertThat(manager.written?.second).isEqualTo(ExportFormat.JSON)
            assertThat((expectMostRecentItem() as SettingsUiState.Ready).message).isEqualTo(SettingsMessage.EXPORTED)
            vm.consumeMessage()
            assertThat((expectMostRecentItem() as SettingsUiState.Ready).message).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `export failure is reported as a human message`() = runTest {
        val vm = viewModel(FakeDataManager(failWrite = true))
        vm.uiState.test {
            awaitItem()
            vm.exportTo(Uri.parse("content://docs/export.csv"), ExportFormat.CSV)
            assertThat((expectMostRecentItem() as SettingsUiState.Ready).message).isEqualTo(SettingsMessage.EXPORT_FAILED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete all wipes subscriptions ledger schedule and preferences`() = runTest {
        subscriptions.seed(subscription(id = "n"))
        preferences.setOnboardingCompleted(true)
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.deleteAllData()
            assertThat(subscriptions.subscriptionsSnapshot).isEmpty()
            assertThat(ledger.cleared).isTrue()
            assertThat(cancelled).isEqualTo(1)
            assertThat(preferences.preferences.first().onboardingCompleted).isFalse()
            assertThat((expectMostRecentItem() as SettingsUiState.Ready).message).isEqualTo(SettingsMessage.DATA_DELETED)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
