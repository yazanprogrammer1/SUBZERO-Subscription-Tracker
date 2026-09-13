package com.subzero.feature.calendar

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
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class CalendarScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private val netflix = subscription(id = "n", name = "Netflix", price = usd(1549), anchorDate = date("2026-09-16"))
    private var previous = false
    private var next = false
    private var selected: LocalDate? = null
    private var opened: SubscriptionId? = null

    private fun state(selectedDay: LocalDate? = today) = CalendarUiState(
        isLoading = false,
        today = today,
        month = YearMonth.of(2026, 9),
        paymentsByDay = mapOf(date("2026-09-16") to listOf(UpcomingPayment(netflix, date("2026-09-16"), usd(1549)))),
        monthTotal = usd(1549),
        foreignCurrencyCount = 0,
        selectedDay = selectedDay,
        hasSubscriptions = true,
    )

    private fun setContent(state: CalendarUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                CalendarScreen(
                    state = state,
                    onPreviousMonth = { previous = true },
                    onNextMonth = { next = true },
                    onToday = {},
                    onSelectDay = { selected = it },
                    onOpenSubscription = { opened = it },
                    onAdd = {},
                )
            }
        }
    }

    @Test
    fun `header shows the month total and title`() {
        setContent(state())
        compose.onAllNodesWithText("$15.49").onFirst().assertIsDisplayed()
        compose.onNodeWithText("scheduled this month").assertIsDisplayed()
        compose.onNodeWithTag(CalendarTestTags.MONTH_TITLE).assertIsDisplayed()
        compose.onNodeWithText("September 2026").assertIsDisplayed()
    }

    @Test
    fun `arrows navigate months`() {
        setContent(state())
        compose.onNodeWithTag(CalendarTestTags.NEXT).performClick()
        compose.onNodeWithTag(CalendarTestTags.PREVIOUS).performClick()
        assertThat(next).isTrue()
        assertThat(previous).isTrue()
    }

    @Test
    fun `tapping a day reports the selection`() {
        setContent(state(selectedDay = null))
        compose.onNodeWithTag(CalendarTestTags.day(date("2026-09-16"))).performClick()
        assertThat(selected).isEqualTo(date("2026-09-16"))
    }

    @Test
    fun `a selected day lists its charges and opens them`() {
        setContent(state(selectedDay = date("2026-09-16")))
        compose.onNodeWithText("September 16, 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Netflix").performScrollTo().performClick()
        assertThat(opened).isEqualTo(SubscriptionId("n"))
    }

    @Test
    fun `a day without charges says so`() {
        setContent(state(selectedDay = today))
        compose.onNodeWithText("Nothing scheduled on today.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `no subscriptions shows the empty state`() {
        setContent(CalendarUiState(isLoading = false, today = today, month = YearMonth.of(2026, 9), hasSubscriptions = false))
        compose.onNodeWithTag(CalendarTestTags.EMPTY).assertIsDisplayed()
    }
}
