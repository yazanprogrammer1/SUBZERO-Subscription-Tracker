package com.subzero.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private val TrackWidth = 48.dp
private val TrackHeight = 28.dp
private val ThumbSize = 22.dp
private val ThumbPadding = 3.dp
private const val DISABLED_ALPHA = 0.4f

/** Toggle with a sliding thumb and crossfading track; 48dp touch target. */
@Composable
fun SubzeroSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    val interactionSource = remember { MutableInteractionSource() }
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.surfaceSubtle,
        animationSpec = motion.standardSpec(),
        label = "switchTrack",
    )
    val border by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.outlineStrong,
        animationSpec = motion.standardSpec(),
        label = "switchBorder",
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) colors.onAccent else colors.textSecondary,
        animationSpec = motion.standardSpec(),
        label = "switchThumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbPadding else ThumbPadding,
        animationSpec = motion.standardSpec(),
        label = "switchOffset",
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .size(48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(TrackWidth, TrackHeight)
                .clip(SubzeroTheme.shapes.full)
                .background(track)
                .border(1.dp, border, SubzeroTheme.shapes.full),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                    .size(ThumbSize)
                    .clip(SubzeroTheme.shapes.full)
                    .background(thumb),
            )
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroSwitchPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroSwitch(checked = true, onCheckedChange = {})
            SubzeroSwitch(checked = false, onCheckedChange = {})
            SubzeroSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
