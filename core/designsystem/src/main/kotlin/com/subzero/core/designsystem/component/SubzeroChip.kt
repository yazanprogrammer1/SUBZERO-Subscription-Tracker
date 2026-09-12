package com.subzero.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme

private val ChipHeight = 36.dp

/** Filter / choice chip. Selection is shown by fill, border and a check icon, not color alone. */
@Composable
fun SubzeroChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) colors.accentContainer else colors.surfaceSubtle,
        animationSpec = motion.fastSpec(),
        label = "chipBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.outline,
        animationSpec = motion.fastSpec(),
        label = "chipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textSecondary,
        animationSpec = motion.fastSpec(),
        label = "chipContent",
    )

    Row(
        modifier = modifier
            .pressScale(interactionSource, SubzeroMotion.PRESS_SCALE_BUTTON, enabled)
            .heightIn(min = ChipHeight)
            .clip(SubzeroTheme.shapes.full)
            .background(background)
            .border(1.dp, border, SubzeroTheme.shapes.full)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = SubzeroTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val leading = if (selected) SubzeroIcons.Check else icon
        if (leading != null) {
            Icon(imageVector = leading, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(SubzeroTheme.spacing.xxs))
        }
        Text(text = text, style = SubzeroTheme.typography.label, color = content, maxLines = 1)
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroChipPreview() {
    PreviewTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubzeroChip(text = "All", selected = true, onClick = {})
            SubzeroChip(text = "Monthly", selected = false, onClick = {})
            SubzeroChip(text = "Yearly", selected = false, onClick = {}, icon = SubzeroIcons.Calendar)
        }
    }
}
