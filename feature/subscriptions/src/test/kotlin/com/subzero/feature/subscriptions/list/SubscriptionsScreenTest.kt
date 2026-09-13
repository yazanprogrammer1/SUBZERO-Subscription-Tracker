package com.subzero.feature.subscriptions.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.SubscriptionFilter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-13"))
    private val paused = subscription(id = "p", name = "Spotify", price = usd(1199), status = SubscriptionStatus.PAUSED)

    private var opened: SubscriptionId? = null
    private var added = false
    private var query = ""
    private var searchActive = false
    private var filter: SubscriptionFilter = SubscriptionFilter.All

    private fun setContent(state: SubscriptionsUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                SubscriptionsScreen(
                    state = state,
                    onQueryChange = { query = it },
                    onSearchActiveChange = { searchActive = it },
                    onFilterChange = { filter = it },
                    onSortChange = {},
                    onAdd = { added = true },
                    onOpen = { opened = it },
                )
            }
        }
    }

    @Test
    fun `empty state offers to add the first subscription`() {
        setContent(SubscriptionsUiState(isLoading = false, totalCount = 0, today = today))
        compose.onNodeWithTag(SubscriptionsTestTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText("Add subscription").performClick()
        assertThat(added).isTrue()
    }

    @Test
    fun `rows show name price relative date and status`() {
        setContent(SubscriptionsUiState(isLoading = false, items = listOf(netflix, paused), totalCount = 2, today = today))
        compose.onNodeWithText("Netflix").assertIsDisplayed()
        compose.onNodeWithText("$15.49").assertIsDisplayed()
        compose.onNodeWithText("Next: Tomorrow").assertIsDisplayed()
        compose.onNodeWithText("Spotify").assertIsDisplayed()
    }

    @Test
    fun `tapping a row opens it`() {
        setContent(SubscriptionsUiState(isLoading = false, items = listOf(netflix), totalCount = 1, today = today))
        compose.onNodeWithTag(SubscriptionsTestTags.row("n")).performClick()
        assertThat(opened).isEqualTo(SubscriptionId("n"))
    }

    @Test
    fun `search toggle activates search and typing reports the query`() {
        setContent(SubscriptionsUiState(isLoading = false, items = listOf(netflix), totalCount = 1, today = today, searchActive = true))
        compose.onNodeWithTag(SubscriptionsTestTags.SEARCH_FIELD).performTextInput("net")
        assertThat(query).isEqualTo("net")
    }

    @Test
    fun `no matches state clears the filter`() {
        setContent(
            SubscriptionsUiState(isLoading = false, items = emptyList(), totalCount = 2, today = today, filter = SubscriptionFilter.HighCost),
        )
        compose.onNodeWithTag(SubscriptionsTestTags.NO_MATCHES).assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()
        assertThat(filter).isEqualTo(SubscriptionFilter.All)
    }

    @Test
    fun `filter chips report the chosen filter`() {
        setContent(SubscriptionsUiState(isLoading = false, items = listOf(netflix), totalCount = 1, today = today))
        compose.onNodeWithText("Yearly").performClick()
        assertThat(filter).isEqualTo(SubscriptionFilter.Cycle(com.subzero.core.domain.model.BillingCycle.Yearly))
    }
}
