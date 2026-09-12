package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.theme.SubzeroTheme

@Immutable
data class SubzeroBottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Bottom navigation for the five top-level destinations.
 *
 * Phase 1 baseline: correct semantics, sizing and theming. The animated active indicator
 * and refined visuals land with the full design system in Phase 3.
 */
@Composable
fun SubzeroBottomBar(
    items: List<SubzeroBottomBarItem>,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .heightIn(min = 64.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val tint = if (item.selected) colors.accent else colors.textTertiary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = item.selected,
                        onClick = item.onClick,
                        role = Role.Tab,
                    )
                    .padding(vertical = SubzeroTheme.spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = item.icon, contentDescription = null, tint = tint)
                Text(
                    text = item.label,
                    style = SubzeroTheme.typography.caption,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
private fun SubzeroBottomBarPreview() {
    SubzeroTheme(darkTheme = true) {
        SubzeroBottomBar(
            items = listOf(
                SubzeroBottomBarItem("Home", Icons.Outlined.Home, selected = true) {},
                SubzeroBottomBarItem("Settings", Icons.Outlined.Settings, selected = false) {},
            ),
        )
    }
}
