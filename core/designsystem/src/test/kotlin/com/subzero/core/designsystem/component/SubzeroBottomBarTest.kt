package com.subzero.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The pill indicator must sit over the selected tab in both directions, not its mirror image. */
@RunWith(RobolectricTestRunner::class)
class SubzeroBottomBarTest {

    @get:Rule
    val compose = createComposeRule()

    private val labels = listOf("Home", "Subscriptions", "Calendar", "Insights", "Settings")

    private fun setContent(selected: Int, direction: LayoutDirection) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    SubzeroBottomBar(
                        items = labels.mapIndexed { index, label ->
                            SubzeroBottomBarItem(
                                label = label,
                                icon = SubzeroIcons.Home,
                                selected = index == selected,
                                onClick = {},
                            )
                        },
                    )
                }
            }
        }
    }

    private fun assertIndicatorCoversSelected(label: String) {
        val indicator = compose.onNodeWithTag(BottomBarTestTags.INDICATOR).getUnclippedBoundsInRoot()
        val item = compose.onNodeWithContentDescription(label).getUnclippedBoundsInRoot()
        val indicatorCenter = (indicator.left + indicator.right) / 2
        val itemCenter = (item.left + item.right) / 2
        assertThat((indicatorCenter - itemCenter).value).isWithin(1f).of(0f)
    }

    @Test
    fun `indicator follows the selected tab left to right`() {
        setContent(selected = 0, direction = LayoutDirection.Ltr)
        assertIndicatorCoversSelected("Home")
    }

    @Test
    fun `indicator follows the selected tab right to left`() {
        setContent(selected = 0, direction = LayoutDirection.Rtl)
        assertIndicatorCoversSelected("Home")
    }

    @Test
    fun `indicator follows a later tab right to left`() {
        setContent(selected = 4, direction = LayoutDirection.Rtl)
        assertIndicatorCoversSelected("Settings")
        // In RTL the last tab is drawn at the left edge, so the pill must be there too.
        val indicator = compose.onNodeWithTag(BottomBarTestTags.INDICATOR).getUnclippedBoundsInRoot()
        assertThat(indicator.left.value).isLessThan(100.dp.value)
    }
}
