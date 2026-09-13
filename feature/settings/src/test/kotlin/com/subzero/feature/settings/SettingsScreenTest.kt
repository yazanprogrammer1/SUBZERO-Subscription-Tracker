package com.subzero.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var upcoming: Boolean? = null
    private var days: Int? = null
    private var summary: Boolean? = null
    private var permissionRequested = false

    private fun ready(notifications: NotificationPreferences, allowed: Boolean = true) = SettingsUiState.Ready(
        preferences = UserPreferences(CurrencyCode.USD, ThemeMode.SYSTEM, notifications, true, null),
        notificationsAllowed = allowed,
    )

    private fun setContent(state: SettingsUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                SettingsScreen(
                    state = state,
                    onRequestPermission = { permissionRequested = true },
                    onOpenSystemSettings = {},
                    onUpcoming = { upcoming = it },
                    onDaysBefore = { days = it },
                    onSummary = { summary = it },
                    onSavings = {},
                )
            }
        }
    }

    @Test
    fun `toggles report changes`() {
        setContent(ready(NotificationPreferences.Default))
        compose.onNodeWithTag(SettingsTestTags.UPCOMING).performClick()
        compose.onNodeWithTag(SettingsTestTags.SUMMARY).performScrollTo().performClick()
        assertThat(upcoming).isTrue()
        assertThat(summary).isTrue()
    }

    @Test
    fun `days before chips appear when upcoming is on`() {
        setContent(ready(NotificationPreferences.Default.copy(upcomingChargeEnabled = true, upcomingChargeDaysBefore = 1)))
        compose.onNodeWithTag(SettingsTestTags.days(3)).performClick()
        assertThat(days).isEqualTo(3)
    }

    @Test
    fun `permission card appears only when something is on but not allowed`() {
        setContent(ready(NotificationPreferences.Default.copy(monthlySummaryEnabled = true), allowed = false))
        compose.onNodeWithTag(SettingsTestTags.PERMISSION).assertIsDisplayed()
        compose.onNodeWithText("Allow notifications").performClick()
        assertThat(permissionRequested).isTrue()
    }

    @Test
    fun `permission card is hidden when nothing is enabled`() {
        setContent(ready(NotificationPreferences.Default, allowed = false))
        compose.onNodeWithTag(SettingsTestTags.PERMISSION).assertDoesNotExist()
    }
}
