package com.subzero.feature.insights

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.CategoryShare
import com.subzero.core.domain.usecase.SpendSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InsightsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private val spotify = subscription(id = "s", name = "Spotify", price = usd(1199), usage = DeclaredUsage.RARELY)
    private var opened: SubscriptionId? = null
    private var reviewed = false

    private fun ready(insights: List<Insight>) = InsightsUiState.Ready(
        today = today,
        homeCurrency = CurrencyCode.USD,
        summary = SpendSummary(monthly = usd(2748), yearly = usd(32976), includedCount = 2, excludedForeignCurrency = 0),
        insights = insights,
        categories = listOf(CategoryShare(Category.ENTERTAINMENT, usd(2748), 2, 100)),
        ranked = listOf(RankedSubscription(spotify, usd(1199), usd(14388))),
        byUsage = mapOf(DeclaredUsage.RARELY to listOf(RankedSubscription(spotify, usd(1199), usd(14388)))),
    )

    private fun setContent(state: InsightsUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                InsightsScreen(
                    state = state,
                    onOpenSubscription = { opened = it },
                    onReviewSubscriptions = { reviewed = true },
                    onAdd = {},
                )
            }
        }
    }

    @Test
    fun `overview lists insights and taps through to a subscription`() {
        setContent(ready(listOf(Insight.LowUsage(spotify, usd(14388)))))
        compose.onNodeWithText("$329.76").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("You rarely use Spotify.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(InsightsTestTags.insight("low-usage-s")).performClick()
        assertThat(opened).isEqualTo(SubscriptionId("s"))
    }

    @Test
    fun `aggregate insights lead to the subscriptions list`() {
        setContent(ready(listOf(Insight.YearlyTotal(usd(32976), usd(2748), 2))))
        compose.onNodeWithTag(InsightsTestTags.insight("yearly-total")).performScrollTo().performClick()
        assertThat(reviewed).isTrue()
    }

    @Test
    fun `no insights shows the not enough data card`() {
        setContent(ready(emptyList()))
        compose.onNodeWithTag(InsightsTestTags.NO_INSIGHTS).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `spending tab shows categories and top subscriptions`() {
        setContent(ready(emptyList()))
        compose.onNodeWithTag(InsightsTestTags.tab(InsightsTab.Spending)).performClick()
        compose.onNodeWithText("Entertainment").assertIsDisplayed()
        compose.onNodeWithText("2 subscriptions · 100%").assertIsDisplayed()
        compose.onNodeWithText("Spotify").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `usage tab groups by declared usage`() {
        setContent(ready(emptyList()))
        compose.onNodeWithTag(InsightsTestTags.tab(InsightsTab.Usage)).performClick()
        compose.onNodeWithText("Rarely used").assertIsDisplayed()
        compose.onNodeWithText("Spotify").performScrollTo().performClick()
        assertThat(opened).isEqualTo(SubscriptionId("s"))
    }

    @Test
    fun `no subscriptions shows the empty state`() {
        setContent(InsightsUiState.NoSubscriptions)
        compose.onNodeWithTag(InsightsTestTags.EMPTY).assertIsDisplayed()
    }
}
