package com.subzero.feature.subscriptions.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.paymentRecord
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CalculateSpendingHistoryUseCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private var paused = false
    private var resumed = false
    private var deleted = false
    private var edited = false
    private var gone = false
    private var usage: DeclaredUsage? = null

    private fun loaded(status: SubscriptionStatus = SubscriptionStatus.ACTIVE): SubscriptionDetailUiState.Loaded {
        val sub = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"), status = status, notes = "Family plan")
        val prices = listOf(priceChange(price = usd(1399), effectiveFrom = date("2026-01-16")), priceChange(price = usd(1549), effectiveFrom = date("2026-04-10")))
        val payments = listOf(paymentRecord(paidOn = date("2026-08-16")))
        return SubscriptionDetailUiState.Loaded(
            subscription = sub,
            monthlyEquivalent = usd(1549),
            yearlyEquivalent = usd(18588),
            history = CalculateSpendingHistoryUseCase(fixedClock(today))(sub, prices, payments),
            priceHistory = prices,
            today = today,
        )
    }

    private fun setContent(state: SubscriptionDetailUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                SubscriptionDetailScreen(
                    state = state,
                    onBack = {},
                    onEdit = { edited = true },
                    onPause = { paused = true },
                    onResume = { resumed = true },
                    onMarkCanceled = {},
                    onDelete = { deleted = true },
                    onUsage = { usage = it },
                    onDismissError = {},
                    onGone = { gone = true },
                )
            }
        }
    }

    @Test
    fun `header shows name price next payment and equivalents`() {
        setContent(loaded())
        compose.onNodeWithText("Netflix").assertIsDisplayed()
        compose.onAllNodesWithText("$15.49").onFirst().assertIsDisplayed()
        compose.onNodeWithText("In 4 days").assertIsDisplayed()
        compose.onNodeWithText("$185.88 / year", substring = true).assertExists()
    }

    @Test
    fun `price history and notes are listed`() {
        setContent(loaded())
        compose.onNodeWithText("$13.99").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Family plan").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `usage chips report the choice`() {
        setContent(loaded())
        compose.onNodeWithTag(DetailTestTags.usage(DeclaredUsage.RARELY)).performScrollTo().performClick()
        assertThat(usage).isEqualTo(DeclaredUsage.RARELY)
    }

    @Test
    fun `active subscription offers pause and delete asks for confirmation`() {
        setContent(loaded())
        compose.onNodeWithTag(DetailTestTags.PAUSE).performScrollTo().performClick()
        assertThat(paused).isTrue()

        compose.onNodeWithTag(DetailTestTags.DELETE).performScrollTo().performClick()
        assertThat(deleted).isFalse()
        compose.onNodeWithText("Delete Netflix?").assertIsDisplayed()
        compose.onAllNodesWithText("Delete").filterToOne(hasAnyAncestor(hasTestTag(DetailTestTags.CONFIRM_DELETE))).performClick()
        assertThat(deleted).isTrue()
    }

    @Test
    fun `paused subscription offers resume`() {
        setContent(loaded(SubscriptionStatus.PAUSED))
        compose.onNodeWithTag(DetailTestTags.RESUME).performScrollTo().performClick()
        assertThat(resumed).isTrue()
    }

    @Test
    fun `edit action and gone state`() {
        setContent(loaded())
        compose.onNodeWithTag(DetailTestTags.EDIT).performClick()
        assertThat(edited).isTrue()
    }

    @Test
    fun `gone state closes the screen`() {
        setContent(SubscriptionDetailUiState.Gone)
        compose.waitForIdle()
        assertThat(gone).isTrue()
    }
}
