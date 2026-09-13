package com.subzero.feature.settings

import android.net.Uri
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.ai.AiConfig
import com.subzero.core.ai.AiConfigSource
import com.subzero.core.ai.AiException
import com.subzero.core.ai.ChatMessage
import com.subzero.core.ai.ChatTransport
import com.subzero.core.data.export.DataManager
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.data.export.StorageInfo
import com.subzero.core.domain.model.AiProvider
import com.subzero.core.domain.model.AiSettings
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.testing.FakeAiSettingsRepository
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
        aiConfigSource = AiConfigSource(aiSettings, defaults = AiConfig(apiKey = "", baseUrl = "", model = "")),
        aiSettings = aiSettings,
        chatTransport = transport,
    )

    private val aiSettings = FakeAiSettingsRepository()
    private var transport: ChatTransport = object : ChatTransport {
        override suspend fun complete(messages: List<ChatMessage>): String = throw AiException("no network in tests")
    }

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

    @Test
    fun `saving a provider makes the assistant available without a rebuild`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertThat((awaitItem() as SettingsUiState.Ready).ai.available).isFalse()

            vm.saveAiSettings(AiProvider.ANTHROPIC, "https://api.example.com/", "claude-opus-5", "sk-test")

            val ready = awaitUntil { it.ai.available }
            assertThat(ready.ai.available).isTrue()
            assertThat(ready.ai.keySet).isTrue()
            assertThat(ready.ai.model).isEqualTo("claude-opus-5")
            assertThat(ready.ai.target).isEqualTo("claude-opus-5 · api.example.com")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editing the model keeps the stored key and clearing forgets everything`() = runTest {
        val vm = viewModel()
        vm.saveAiSettings(AiProvider.ANTHROPIC, "https://api.example.com", "claude-opus-5", "sk-test")

        vm.saveAiSettings(AiProvider.ANTHROPIC, "https://api.example.com", "claude-haiku-4-5", "")
        assertThat(aiSettings.settings.first().apiKey).isEqualTo("sk-test")
        assertThat(aiSettings.settings.first().model).isEqualTo("claude-haiku-4-5")

        vm.clearAiSettings()
        assertThat(aiSettings.settings.first()).isEqualTo(AiSettings.Empty)
    }

    @Test
    fun `a failed test maps the provider status to a reason the screen can word`() = runTest {
        transport = object : ChatTransport {
            override suspend fun complete(messages: List<ChatMessage>): String =
                throw AiException("HTTP 401", status = 401, providerMessage = "unauthorized client detected")
        }
        val vm = viewModel()
        vm.saveAiSettings(AiProvider.ANTHROPIC, "https://api.example.com", "claude-opus-5", "sk-test")

        vm.uiState.test {
            awaitItem()
            vm.testAiConnection()

            val ready = awaitUntil { it.ai.failure != null }
            assertThat(ready.ai.failure?.kind).isEqualTo(AiFailureKind.REJECTED)
            assertThat(ready.ai.failure?.providerMessage).isEqualTo("unauthorized client detected")
            assertThat(ready.message).isEqualTo(SettingsMessage.AI_FAILED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no reply at all reads as unreachable, not as a rejected key`() = runTest {
        transport = object : ChatTransport {
            override suspend fun complete(messages: List<ChatMessage>): String = throw AiException("Could not reach the model")
        }
        val vm = viewModel()
        vm.saveAiSettings(AiProvider.ANTHROPIC, "https://api.example.com", "claude-opus-5", "sk-test")

        vm.uiState.test {
            awaitItem()
            vm.testAiConnection()

            assertThat(awaitUntil { it.ai.failure != null }.ai.failure?.kind).isEqualTo(AiFailureKind.UNREACHABLE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Waits for the first Ready state matching [predicate]; states before it are intermediate. */
    private suspend fun ReceiveTurbine<SettingsUiState>.awaitUntil(
        predicate: (SettingsUiState.Ready) -> Boolean,
    ): SettingsUiState.Ready {
        while (true) {
            val state = awaitItem()
            if (state is SettingsUiState.Ready && predicate(state)) return state
        }
    }
}
