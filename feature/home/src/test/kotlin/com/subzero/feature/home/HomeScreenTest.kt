package com.subzero.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.MonthTotal
import com.subzero.core.domain.usecase.PotentialSavings
import com.subzero.core.domain.usecase.SpendSummary
import com.subzero.core.domain.usecase.SpendTrend
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-13"))
    private var added = false
    private var opened: SubscriptionId? = null
    private var reviewed = false

    private fun dashboard(insight: Insight? = null, savings: PotentialSavings = PotentialSavings(usd(0), emptyList())) = HomeUiState.Dashboard(
        displayName = "Alex",
        today = today,
        homeCurrency = CurrencyCode.USD,
        summary = SpendSummary(monthly = usd(8748), yearly = usd(104976), includedCount = 4, excludedForeignCurrency = 1),
        nextPayment = UpcomingPayment(netflix, date("2026-09-13"), usd(1549)),
        trend = SpendTrend(
            months = listOf(
                MonthTotal(YearMonth.of(2026, 8), usd(8070), usd(0), usd(0)),
                MonthTotal(YearMonth.of(2026, 9), usd(8748), usd(1549), usd(0)),
            ),
            changeVersusPreviousPercent = BigDecimal("8.4"),
        ),
        savings = savings,
        insight = insight,
    )

    private fun setContent(state: HomeUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                HomeScreen(
                    state = state,
                    onAdd = { added = true },
                    onOpenSubscription = { opened = it },
                    onReviewSubscriptions = { reviewed = true },
                    clock = fixedClock(today),
                )
            }
        }
    }

    @Test
    fun `empty state greets and offers to add`() {
        setContent(HomeUiState.Empty("Alex"))
        compose.onNodeWithText("Good afternoon, Alex").assertIsDisplayed()
        compose.onNodeWithTag(HomeTestTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText("Add subscription").performClick()
        assertThat(added).isTrue()
    }

    @Test
    fun `hero shows monthly yearly and the foreign currency note`() {
        setContent(dashboard())
        compose.onAllNodesWithText("$87.48").onFirst().assertIsDisplayed()
        compose.onNodeWithText("$1,049.76 / year").assertIsDisplayed()
        compose.onNodeWithText("1 subscription in another currency is not included.").assertIsDisplayed()
    }

    @Test
    fun `next charge card opens the subscription`() {
        setContent(dashboard())
        compose.onNodeWithText("Tomorrow").assertIsDisplayed()
        compose.onNodeWithTag(HomeTestTags.NEXT_CHARGE).performClick()
        assertThat(opened).isEqualTo(SubscriptionId("n"))
    }

    @Test
    fun `trend shows change and projected note`() {
        setContent(dashboard())
        compose.onNodeWithText("+8.4%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Includes $15.49 still scheduled this month.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `savings card shows the amount and leads to review`() {
        setContent(dashboard(savings = PotentialSavings(usd(2300), listOf(netflix))))
        compose.onNodeWithText("$23.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(HomeTestTags.SAVINGS).performClick()
        assertThat(reviewed).isTrue()
    }

    @Test
    fun `insight card is worded from the insight`() {
        setContent(dashboard(insight = Insight.YearlyTotal(yearly = usd(104976), monthly = usd(8748), activeCount = 4)))
        compose.onNodeWithText("You spend $1,049.76 a year on subscriptions.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Review subscriptions").performScrollTo().performClick()
        assertThat(reviewed).isTrue()
    }
}
