package com.subzero.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private const val DISABLED_ALPHA = 0.5f
private val FieldMinHeight = 52.dp

/**
 * The text field (design-system.md §7): a labelled, hairline-bordered field whose border
 * brightens on focus and turns to danger with a message on error.
 */
@Composable
fun SubzeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showClearButton: Boolean = false,
    /** Test tag applied to the editable node itself (the outer [modifier] wraps label and helper). */
    inputTag: String? = null,
) {
    val colors = SubzeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val isError = error != null
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            focused -> colors.outlineStrong
            else -> colors.outline
        },
        animationSpec = SubzeroTheme.motion.fastSpec(),
        label = "fieldBorder",
    )

    Column(modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) {
        Text(
            text = label,
            style = SubzeroTheme.typography.label,
            color = if (isError) colors.danger else colors.textSecondary,
            modifier = Modifier.padding(bottom = SubzeroTheme.spacing.xs),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight)
                .clip(SubzeroTheme.shapes.sm)
                .background(colors.surfaceSubtle)
                .border(1.dp, borderColor, SubzeroTheme.shapes.sm)
                .padding(horizontal = SubzeroTheme.spacing.md, vertical = SubzeroTheme.spacing.sm)
                .semantics { if (error != null) this.error(error) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(SubzeroTheme.spacing.xs))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(text = placeholder, style = SubzeroTheme.typography.body, color = colors.textTertiary)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = SubzeroTheme.typography.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (inputTag != null) Modifier.testTag(inputTag) else Modifier),
                )
            }
            if (showClearButton && value.isNotEmpty() && enabled) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = SubzeroIcons.Close,
                        contentDescription = stringResource(R.string.core_designsystem_clear_text),
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(SubzeroTheme.spacing.xs))
                trailing()
            }
        }
        AnimatedVisibility(visible = error != null || helper != null) {
            Text(
                text = error ?: helper.orEmpty(),
                style = SubzeroTheme.typography.caption,
                color = if (isError) colors.danger else colors.textTertiary,
                modifier = Modifier.padding(top = SubzeroTheme.spacing.xxs, start = SubzeroTheme.spacing.xxs),
            )
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroTextFieldPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroTextField(value = "", onValueChange = {}, label = "Service name", placeholder = "e.g. Netflix")
            Spacer(Modifier.padding(8.dp))
            SubzeroTextField(
                value = "15.49",
                onValueChange = {},
                label = "Price",
                leading = { Text("$", style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                showClearButton = true,
            )
            Spacer(Modifier.padding(8.dp))
            SubzeroTextField(value = "", onValueChange = {}, label = "Price", error = "Enter a price")
            Spacer(Modifier.padding(8.dp))
            SubzeroTextField(value = "Family plan", onValueChange = {}, label = "Notes", helper = "Optional", singleLine = false, minLines = 2)
            Spacer(Modifier.padding(8.dp))
            SubzeroTextField(value = "Disabled", onValueChange = {}, label = "Disabled", enabled = false)
        }
    }
}
