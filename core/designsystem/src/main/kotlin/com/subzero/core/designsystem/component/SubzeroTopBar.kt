package com.subzero.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private val TopBarMinHeight = 56.dp

/**
 * Screen header. Large title for top-level tabs, compact title with back for pushed screens.
 * Sits on the page ground (no fill) so content scrolls under the status bar naturally.
 */
@Composable
fun SubzeroTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    large: Boolean = onBack == null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = TopBarMinHeight)
            .padding(horizontal = if (onBack != null) spacing.xs else spacing.screen, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = SubzeroIcons.Back,
                    contentDescription = stringResource(R.string.core_designsystem_back),
                    tint = colors.textPrimary,
                )
            }
            Spacer(Modifier.width(spacing.xxs))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (large) SubzeroTheme.typography.headline else SubzeroTheme.typography.title,
                color = colors.textPrimary,
                maxLines = if (large) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Section title inside a screen, with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SubzeroTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SubzeroTheme.typography.title,
            color = SubzeroTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            SubzeroButton(text = actionLabel, onClick = onAction, style = SubzeroButtonStyle.Ghost, compact = true)
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroTopBarPreview() {
    PreviewTheme {
        Column {
            SubzeroTopBar(title = "Subscriptions", actions = {
                IconButton(onClick = {}) { Icon(SubzeroIcons.Search, contentDescription = "Search", tint = SubzeroTheme.colors.textPrimary) }
            })
            SubzeroTopBar(title = "Netflix", subtitle = "Entertainment", onBack = {})
            SectionHeader(title = "Upcoming", actionLabel = "See all", onAction = {})
        }
    }
}
