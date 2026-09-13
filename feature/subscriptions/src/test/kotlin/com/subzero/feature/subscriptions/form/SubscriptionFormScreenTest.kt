package com.subzero.feature.subscriptions.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.usecase.SubscriptionValidationError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionFormScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private var name = ""
    private var price = ""
    private var category: Category? = null
    private var cycle: BillingCycle? = null
    private var saved = false
    private var savedDone = false

    private fun setContent(state: SubscriptionFormState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                SubscriptionFormScreen(
                    state = state,
                    actions = FormActions(
                        onName = { name = it },
                        onPrice = { price = it },
                        onCurrency = {},
                        onStandardCycle = { cycle = it },
                        onCustomMode = {},
                        onCustomEvery = {},
                        onCustomUnit = {},
                        onNextDate = {},
                        onFirstDate = {},
                        onCategory = { category = it },
                        onUsage = {},
                        onNotes = {},
                        onSave = { saved = true },
                        onDismissFailure = {},
                    ),
                    onBack = {},
                    onSaved = { savedDone = true },
                )
            }
        }
    }

    private val ready = SubscriptionFormState(isLoading = false, today = today, nextPaymentDate = today, currency = CurrencyCode.USD)

    @Test
    fun `typing into fields reports changes`() {
        // Controlled fields: feed the reported values back so the screen behaves as in the app.
        compose.setContent {
            var state by remember { mutableStateOf(ready) }
            SubzeroTheme(darkTheme = true) {
                SubscriptionFormScreen(
                    state = state,
                    actions = FormActions(
                        onName = { state = state.copy(name = it) },
                        onPrice = { state = state.copy(priceText = it) },
                        onCurrency = {}, onStandardCycle = {}, onCustomMode = {}, onCustomEvery = {}, onCustomUnit = {},
                        onNextDate = {}, onFirstDate = {}, onCategory = {}, onUsage = {}, onNotes = {}, onSave = {}, onDismissFailure = {},
                    ),
                    onBack = {},
                    onSaved = {},
                )
                name = state.name
                price = state.priceText
            }
        }
        compose.onNodeWithTag(FormTestTags.NAME).performTextInput("Netflix")
        compose.onNodeWithTag(FormTestTags.PRICE).performTextInput("15.49")
        compose.waitForIdle()
        assertThat(name).isEqualTo("Netflix")
        assertThat(price).isEqualTo("15.49")
    }

    @Test
    fun `chips report cycle and category`() {
        setContent(ready)
        compose.onNodeWithTag(FormTestTags.cycle("Yearly")).performClick()
        compose.onNodeWithTag(FormTestTags.category(Category.AI)).performScrollTo().performClick()
        assertThat(cycle).isEqualTo(BillingCycle.Yearly)
        assertThat(category).isEqualTo(Category.AI)
    }

    @Test
    fun `validation errors are shown as field messages`() {
        setContent(ready.copy(errors = setOf(SubscriptionValidationError.NAME_BLANK, SubscriptionValidationError.PRICE_MISSING)))
        compose.onNodeWithText("Enter the service name").assertIsDisplayed()
        compose.onNodeWithText("Enter a valid price").assertIsDisplayed()
    }

    @Test
    fun `save button triggers save`() {
        setContent(ready)
        compose.onNodeWithTag(FormTestTags.SAVE).performScrollTo().performClick()
        assertThat(saved).isTrue()
    }

    @Test
    fun `saved state shows the confirmation and then finishes`() {
        setContent(ready.copy(saved = subscription(name = "Netflix")))
        compose.onNodeWithTag(FormTestTags.SAVED).assertIsDisplayed()
        compose.onNodeWithText("Subscription added").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertThat(savedDone).isTrue()
    }

    @Test
    fun `missing subscription shows a human error`() {
        setContent(ready.copy(isEdit = true, failure = FormFailure.NOT_FOUND))
        compose.onNodeWithText("This subscription no longer exists.").assertIsDisplayed()
    }
}
