package com.subzero.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

@Immutable
data class SubzeroBottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

private val BarMinHeight = 64.dp
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 30.dp
private val HideLabelsBelow = 320.dp

/** Test tags for the bottom bar; the indicator is checked for position in both directions. */
object BottomBarTestTags {
    const val INDICATOR = "bottom_bar_indicator"
}

/**
 * Bottom navigation for the five top-level destinations (design-system.md §6.3, §7).
 * A pill indicator slides between items; icon and label tint crossfade.
 */
@Composable
fun SubzeroBottomBar(
    items: List<SubzeroBottomBarItem>,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    val selectedIndex = items.indexOfFirst { it.selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .heightIn(min = BarMinHeight),
    ) {
        val showLabels = maxWidth >= HideLabelsBelow
        val itemWidth = maxWidth / items.size
        val indicatorTarget = itemWidth * selectedIndex + (itemWidth - IndicatorWidth) / 2
        val indicatorOffset by animateFloatAsState(
            targetValue = indicatorTarget.value,
            animationSpec = motion.standardSpec(),
            label = "navIndicator",
        )

        Box(modifier = Modifier.fillMaxWidth().height(BarMinHeight)) {
            // The sliding pill sits behind the icons, aligned with the icon box of the column
            // (icon 30dp + label 18dp centred in 64dp, or icon alone). The offset is measured
            // from the start edge, which mirrors with the row of items in RTL on its own.
            val indicatorTop = if (showLabels) 8.dp else (BarMinHeight - IndicatorHeight) / 2
            Box(
                modifier = Modifier
                    .padding(top = indicatorTop, start = indicatorOffset.dp)
                    .size(IndicatorWidth, IndicatorHeight)
                    .clip(SubzeroTheme.shapes.full)
                    .background(colors.accentContainer)
                    .testTag(BottomBarTestTags.INDICATOR),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarMinHeight)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val tint by animateColorAsState(
                        targetValue = if (item.selected) colors.accent else colors.textTertiary,
                        animationSpec = motion.standardSpec(),
                        label = "navTint",
                    )
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(BarMinHeight)
                            .selectable(
                                selected = item.selected,
                                onClick = item.onClick,
                                role = Role.Tab,
                            )
                            .semantics { contentDescription = item.label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(modifier = Modifier.size(IndicatorWidth, IndicatorHeight), contentAlignment = Alignment.Center) {
                            Icon(imageVector = item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                        }
                        if (showLabels) {
                            Text(
                                text = item.label,
                                style = SubzeroTheme.typography.caption,
                                color = tint,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroBottomBarPreview() {
    PreviewTheme {
        SubzeroBottomBar(
            items = listOf(
                SubzeroBottomBarItem("Home", SubzeroIcons.Home, selected = true) {},
                SubzeroBottomBarItem("Subscriptions", SubzeroIcons.Subscriptions, selected = false) {},
                SubzeroBottomBarItem("Calendar", SubzeroIcons.Calendar, selected = false) {},
                SubzeroBottomBarItem("Insights", SubzeroIcons.Insights, selected = false) {},
                SubzeroBottomBarItem("Settings", SubzeroIcons.Settings, selected = false) {},
            ),
        )
    }
}
