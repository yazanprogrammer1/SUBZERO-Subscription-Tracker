package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money

private const val MUTED_ALPHA = 0.55f
private val RowMinHeight = 68.dp

/**
 * One subscription in a list: monogram, name, cadence, next charge and price.
 * Takes already-formatted strings so the design system stays free of business rules.
 */
@Composable
fun SubzeroSubscriptionRow(
    name: String,
    price: Money,
    cadence: String,
    nextCharge: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    statusBadge: String? = null,
    muted: Boolean = false,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .pressScale(interactionSource, SubzeroMotion.PRESS_SCALE_CARD)
                        .clip(SubzeroTheme.shapes.md)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                } else {
                    Modifier.clip(SubzeroTheme.shapes.md)
                },
            )
            .background(colors.surface)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .alpha(if (muted) MUTED_ALPHA else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubzeroServiceIcon(name = name)
        Spacer(Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = SubzeroTheme.typography.body,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (statusBadge != null) {
                    Spacer(Modifier.width(spacing.xs))
                    SubzeroTag(text = statusBadge, tone = SubzeroTone.Warning)
                }
            }
            Text(
                text = nextCharge,
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            SubzeroMoneyText(money = price, animate = false)
            Text(text = cadence, style = SubzeroTheme.typography.caption, color = colors.textTertiary, maxLines = 1)
        }
    }
}

/** Small pill tag, e.g. a status or category. */
@Composable
fun SubzeroTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: SubzeroTone = SubzeroTone.Neutral,
) {
    val colors = SubzeroTheme.colors
    Text(
        text = text,
        style = SubzeroTheme.typography.caption,
        color = colors.forTone(tone),
        maxLines = 1,
        modifier = modifier
            .clip(SubzeroTheme.shapes.full)
            .background(colors.containerForTone(tone))
            .padding(horizontal = SubzeroTheme.spacing.xs, vertical = 2.dp),
    )
}

@SubzeroPreviews
@Composable
private fun SubzeroSubscriptionRowPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SubzeroSubscriptionRow(
                name = "Netflix",
                price = Money(1549, CurrencyCode.USD),
                cadence = "/ month",
                nextCharge = "Next: Sep 16, 2026",
                onClick = {},
            )
            SubzeroSubscriptionRow(
                name = "Adobe Creative Cloud All Apps Annual Plan",
                price = Money(59988, CurrencyCode.USD),
                cadence = "/ year",
                nextCharge = "Next: Oct 1, 2026",
                onClick = {},
            )
            SubzeroSubscriptionRow(
                name = "Spotify",
                price = Money(1199, CurrencyCode.USD),
                cadence = "/ month",
                nextCharge = "Paused",
                statusBadge = "Paused",
                muted = true,
            )
        }
    }
}
