package com.subzero.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.data.export.StorageInfo
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var upcoming: Boolean? = null
    private var days: Int? = null
    private var summary: Boolean? = null
    private var permissionRequested = false
    private var name: String? = "unset"
    private var theme: ThemeMode? = null
    private var export: ExportFormat? = null
    private var languageSettingsOpened = false
    private var savedAi: AiEndpointDraft? = null
    private var deleted = false

    private fun ready(notifications: NotificationPreferences = NotificationPreferences.Default, allowed: Boolean = true) = SettingsUiState.Ready(
        preferences = UserPreferences(CurrencyCode.USD, ThemeMode.SYSTEM, notifications, true, null),
        notificationsAllowed = allowed,
        storage = StorageInfo(databaseBytes = 12_288, subscriptionCount = 3),
        appVersion = "0.1.0",
    )

    private fun setContent(state: SettingsUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                SettingsScreen(
                    state = state,
                    actions = SettingsActions(
                        onRequestPermission = { permissionRequested = true },
                        onOpenSystemSettings = {},
                        onUpcoming = { upcoming = it },
                        onDaysBefore = { days = it },
                        onSummary = { summary = it },
                        onSavings = {},
                        onName = { name = it },
                        onCurrency = {},
                        onTheme = { theme = it },
                        onOpenLanguageSettings = { languageSettingsOpened = true },
                        onAiEnhanced = {},
                        onTestAi = {},
                        onSaveAi = { savedAi = it },
                        onClearAi = {},
                        onExport = { export = it },
                        onDeleteAll = { deleted = true },
                        onMessageShown = {},
                    ),
                )
            }
        }
    }

    @Test
    fun `toggles report changes`() {
        setContent(ready())
        compose.onNodeWithTag(SettingsTestTags.UPCOMING).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.SUMMARY).performScrollTo().performClick()
        assertThat(upcoming).isTrue()
        assertThat(summary).isTrue()
    }

    @Test
    fun `days before chips appear when upcoming is on`() {
        setContent(ready(NotificationPreferences.Default.copy(upcomingChargeEnabled = true, upcomingChargeDaysBefore = 1)))
        compose.onNodeWithTag(SettingsTestTags.days(3)).performScrollTo().performClick()
        assertThat(days).isEqualTo(3)
    }

    @Test
    fun `permission card appears only when something is on but not allowed`() {
        setContent(ready(NotificationPreferences.Default.copy(monthlySummaryEnabled = true), allowed = false))
        compose.onNodeWithTag(SettingsTestTags.PERMISSION).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Allow notifications").performScrollTo().performClick()
        assertThat(permissionRequested).isTrue()
    }

    @Test
    fun `permission card is hidden when nothing is enabled`() {
        setContent(ready(allowed = false))
        compose.onNodeWithTag(SettingsTestTags.PERMISSION).assertDoesNotExist()
    }

    @Test
    fun `name is saved on demand`() {
        setContent(ready())
        compose.onNodeWithTag(SettingsTestTags.NAME).performTextInput("Alex")
        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).performClick()
        assertThat(name).isEqualTo("Alex")
    }

    @Test
    fun `theme chips report the mode`() {
        setContent(ready())
        compose.onNodeWithTag(SettingsTestTags.theme(ThemeMode.LIGHT)).performScrollTo().performClick()
        assertThat(theme).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `export buttons report the format and storage line is shown`() {
        setContent(ready())
        compose.onNodeWithText("3 subscriptions", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTestTags.EXPORT_CSV).performScrollTo().performClick()
        assertThat(export).isEqualTo(ExportFormat.CSV)
    }

    @Test
    fun `delete all asks for confirmation first`() {
        setContent(ready())
        compose.onNodeWithTag(SettingsTestTags.DELETE).performScrollTo().performClick()
        assertThat(deleted).isFalse()
        compose.onNodeWithText("Delete everything?").assertIsDisplayed()
        compose.onAllNodesWithText("Delete all data").filterToOne(hasAnyAncestor(hasTestTag(SettingsTestTags.CONFIRM_DELETE))).performClick()
        assertThat(deleted).isTrue()
    }

    @Test
    @Config(qualifiers = "ar")
    fun `renders Arabic when the device language is Arabic`() {
        setContent(ready())
        compose.onNodeWithText("الإعدادات").assertIsDisplayed()
        compose.onNodeWithText("الملف الشخصي").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTestTags.LANGUAGE).performScrollTo().performClick()
        assertThat(languageSettingsOpened).isTrue()
    }

    @Test
    fun `about shows the version`() {
        setContent(ready())
        compose.onNodeWithText("SUBZERO 0.1.0").performScrollTo().assertIsDisplayed()
    }
}
