package com.subzero.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroGradients
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme

enum class SubzeroButtonStyle { Primary, Secondary, Ghost, Danger }

private const val DISABLED_ALPHA = 0.4f
private val ButtonHeight = 52.dp
private val CompactButtonHeight = 40.dp

/**
 * The button (design-system.md §7). Primary carries the accent gradient and is the only
 * button that may glow; there is at most one per screen.
 */
@Composable
fun SubzeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SubzeroButtonStyle = SubzeroButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = SubzeroTheme.colors
    val shape = SubzeroTheme.shapes.md
    val interactionSource = remember { MutableInteractionSource() }
    val interactive = enabled && !loading

    val contentColor: Color = when (style) {
        SubzeroButtonStyle.Primary -> colors.onAccent
        SubzeroButtonStyle.Secondary -> colors.textPrimary
        SubzeroButtonStyle.Ghost -> colors.accent
        SubzeroButtonStyle.Danger -> colors.danger
    }
    val background = when (style) {
        SubzeroButtonStyle.Primary -> Modifier.background(SubzeroGradients.accentButton(colors))
        SubzeroButtonStyle.Secondary -> Modifier.background(colors.surfaceSubtle).border(BorderStroke(1.dp, colors.outlineStrong), shape)
        SubzeroButtonStyle.Ghost -> Modifier
        SubzeroButtonStyle.Danger -> Modifier.background(colors.dangerContainer)
    }

    Row(
        modifier = modifier
            .pressScale(interactionSource, SubzeroMotion.PRESS_SCALE_BUTTON, interactive)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .heightIn(min = if (compact) CompactButtonHeight else ButtonHeight)
            .clip(shape)
            .then(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = interactive,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(PaddingValues(horizontal = SubzeroTheme.spacing.lg, vertical = SubzeroTheme.spacing.xs)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = loading) {
            Row {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(SubzeroTheme.spacing.xs))
            }
        }
        if (leadingIcon != null && !loading) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(SubzeroTheme.spacing.xs))
        }
        Text(
            text = text,
            style = SubzeroTheme.typography.label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroButtonPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SubzeroButton("Add subscription", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = SubzeroIcons.Add)
            SubzeroButton("Skip for now", onClick = {}, modifier = Modifier.fillMaxWidth(), style = SubzeroButtonStyle.Secondary)
            SubzeroButton("Review subscriptions", onClick = {}, style = SubzeroButtonStyle.Ghost, compact = true)
            SubzeroButton("Delete", onClick = {}, style = SubzeroButtonStyle.Danger, compact = true, leadingIcon = SubzeroIcons.Delete)
            SubzeroButton("Saving", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
            SubzeroButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
            SubzeroButton("A very long label that should ellipsize rather than wrap onto lines", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
