package com.subzero.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var outcome: OnboardingOutcome? = null

    private fun setContent() {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                OnboardingScreen(onComplete = { outcome = it })
            }
        }
    }

    @Test
    fun `first page shows the welcome copy and paging actions`() {
        setContent()
        compose.onNodeWithText("Meet SUBZERO").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.NEXT).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.SKIP).assertIsDisplayed()
    }

    @Test
    fun `next walks through every page to the final actions`() {
        setContent()
        repeat(4) {
            compose.onNodeWithTag(OnboardingTestTags.NEXT).performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Start your financial overview").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.ADD_FIRST).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.SKIP_FOR_NOW).assertIsDisplayed()
    }

    @Test
    fun `skip jumps to the last page`() {
        setContent()
        compose.onNodeWithTag(OnboardingTestTags.SKIP).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(OnboardingTestTags.ADD_FIRST).assertIsDisplayed()
    }

    @Test
    fun `add first subscription reports the outcome`() {
        setContent()
        compose.onNodeWithTag(OnboardingTestTags.SKIP).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(OnboardingTestTags.ADD_FIRST).performClick()
        assertThat(outcome).isEqualTo(OnboardingOutcome.AddFirstSubscription)
    }

    @Test
    fun `skip for now reports the skipped outcome`() {
        setContent()
        compose.onNodeWithTag(OnboardingTestTags.SKIP).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(OnboardingTestTags.SKIP_FOR_NOW).performClick()
        assertThat(outcome).isEqualTo(OnboardingOutcome.Skipped)
    }
}
