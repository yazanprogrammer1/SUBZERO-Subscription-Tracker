package com.subzero.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone

private val StateMaxWidth = 360.dp

/**
 * Designed empty state (design-system.md §7): a toned glyph, a headline that speaks to the
 * user, one sentence of guidance, and the single action that resolves the emptiness.
 */
@Composable
fun SubzeroEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = SubzeroIcons.Sparkle,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateLayout(
        modifier = modifier,
        icon = icon,
        tone = SubzeroTone.Accent,
        title = title,
        description = description,
        actionLabel = actionLabel,
        onAction = onAction,
        actionStyle = SubzeroButtonStyle.Primary,
    )
}

/**
 * Human error state. Never shows an exception; [description] is a sentence about what the
 * user was doing, and [onRetry] is the way out.
 */
@Composable
fun SubzeroErrorState(
    description: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.core_designsystem_something_went_wrong),
    onRetry: (() -> Unit)? = null,
) {
    StateLayout(
        modifier = modifier,
        icon = SubzeroIcons.Warning,
        tone = SubzeroTone.Danger,
        title = title,
        description = description,
        actionLabel = if (onRetry != null) stringResource(R.string.core_designsystem_retry) else null,
        onAction = onRetry,
        actionStyle = SubzeroButtonStyle.Secondary,
    )
}

@Composable
private fun StateLayout(
    modifier: Modifier,
    icon: ImageVector,
    tone: SubzeroTone,
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    actionStyle: SubzeroButtonStyle,
) {
    val spacing = SubzeroTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SubzeroIconContainer(icon = icon, tone = tone, size = ServiceIconSize.Header)
        Spacer(Modifier.height(spacing.lg))
        Text(
            text = title,
            style = SubzeroTheme.typography.title,
            color = SubzeroTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = StateMaxWidth),
        )
        Spacer(Modifier.height(spacing.xs))
        Text(
            text = description,
            style = SubzeroTheme.typography.bodySmall,
            color = SubzeroTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = StateMaxWidth),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.xl))
            SubzeroButton(text = actionLabel, onClick = onAction, style = actionStyle, leadingIcon = if (actionStyle == SubzeroButtonStyle.Primary) SubzeroIcons.Add else null)
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroStatesPreview() {
    PreviewTheme {
        Column {
            SubzeroEmptyState(
                title = "Your recurring spending starts here.",
                description = "Add your first subscription and SUBZERO will build your financial overview.",
                actionLabel = "Add subscription",
                onAction = {},
            )
            SubzeroErrorState(description = "Your subscription could not be saved.", onRetry = {})
        }
    }
}
