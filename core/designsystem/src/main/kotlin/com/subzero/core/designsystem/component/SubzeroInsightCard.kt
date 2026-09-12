package com.subzero.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone

/**
 * An insight: toned icon, title, explanation and an optional action. The whole card is the
 * tap target when [onClick] is given; a chevron signals it (design-system.md §7).
 */
@Composable
fun SubzeroInsightCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: SubzeroTone = SubzeroTone.Accent,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    SubzeroCard(modifier = modifier.fillMaxWidth(), onClick = onClick, onClickLabel = actionLabel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubzeroIconContainer(icon = icon, tone = tone)
            Spacer(Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SubzeroTheme.typography.body,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null) {
                Spacer(Modifier.width(spacing.xs))
                Icon(imageVector = SubzeroIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary)
            }
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.sm))
            SubzeroButton(text = actionLabel, onClick = onAction, style = SubzeroButtonStyle.Secondary, compact = true)
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroInsightCardPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SubzeroInsightCard(
                title = "You spend \$1,049 / year on subscriptions.",
                description = "Across 6 active subscriptions.",
                icon = SubzeroIcons.TrendUp,
                onClick = {},
            )
            SubzeroInsightCard(
                title = "Potential savings",
                description = "You told us you rarely use Spotify. That is \$143.88 a year.",
                icon = SubzeroIcons.Savings,
                tone = SubzeroTone.Positive,
                actionLabel = "Review subscriptions",
                onAction = {},
            )
            SubzeroInsightCard(
                title = "Netflix increased its price",
                description = "From \$13.99 to \$15.49 per month, a 10.7% increase.",
                icon = SubzeroIcons.Warning,
                tone = SubzeroTone.Warning,
                onClick = {},
            )
        }
    }
}
