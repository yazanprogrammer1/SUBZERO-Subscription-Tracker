package com.subzero.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroGradients
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme

/** Depth recipes from design-system.md §5. */
enum class SubzeroCardStyle {
    /** Level 1: surface + hairline border + sheen. */
    Standard,

    /** Level 1 glass: translucent surface for cards drawn over a glow. */
    Glass,

    /** Level 2: gradient ground + icy glow. One per screen at most. */
    Hero,
}

/**
 * The card. Depth comes from layered fills and a hairline border, never from shadows on dark.
 * Pass [onClick] to make it tactile (press scale + stronger border).
 */
@Composable
fun SubzeroCard(
    modifier: Modifier = Modifier,
    style: SubzeroCardStyle = SubzeroCardStyle.Standard,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    contentPadding: Dp = if (style == SubzeroCardStyle.Hero) SubzeroTheme.spacing.lg else SubzeroTheme.spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SubzeroTheme.colors
    val shape: Shape = if (style == SubzeroCardStyle.Hero) SubzeroTheme.shapes.lg else SubzeroTheme.shapes.md
    val interactionSource = remember { MutableInteractionSource() }
    val border = when (style) {
        SubzeroCardStyle.Hero -> BorderStroke(1.dp, colors.outlineStrong.copy(alpha = colors.outlineStrong.alpha * 0.6f))
        else -> BorderStroke(1.dp, colors.outline)
    }

    val clickModifier = if (onClick != null) {
        Modifier
            .pressScale(interactionSource, SubzeroMotion.PRESS_SCALE_CARD, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .clip(shape)
            .then(
                when (style) {
                    SubzeroCardStyle.Standard -> Modifier.background(colors.surface)
                    SubzeroCardStyle.Glass -> Modifier.background(colors.surfaceGlass)
                    SubzeroCardStyle.Hero -> Modifier.background(SubzeroGradients.heroSurface(colors))
                },
            )
            .drawWithContent {
                drawContent()
                if (style == SubzeroCardStyle.Hero) drawRect(SubzeroGradients.heroGlow(colors))
                drawRect(SubzeroGradients.cardSheen(colors))
                if (style == SubzeroCardStyle.Glass) {
                    drawRect(colors.outlineHighlight, size = size.copy(height = 1.dp.toPx()))
                }
            }
            .border(border, shape),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroCardPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroCard(modifier = Modifier.fillMaxWidth(), style = SubzeroCardStyle.Hero) {
                Text("Hero", style = SubzeroTheme.typography.title, color = SubzeroTheme.colors.textPrimary)
                Text("Monthly spending", style = SubzeroTheme.typography.bodySmall, color = SubzeroTheme.colors.textSecondary)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            SubzeroCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Standard, clickable", style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textPrimary)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            SubzeroCard(modifier = Modifier.fillMaxWidth(), style = SubzeroCardStyle.Glass) {
                Text("Glass", style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textPrimary)
            }
        }
    }
}

@Preview(name = "Card font scale 2x", fontScale = 2f)
@Composable
private fun SubzeroCardLargeFontPreview() {
    PreviewTheme {
        SubzeroCard(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("A card whose text is scaled twice", style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textPrimary)
        }
    }
}
