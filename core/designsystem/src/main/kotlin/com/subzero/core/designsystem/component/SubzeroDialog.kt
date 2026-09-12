package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private val DialogMaxWidth = 400.dp

/**
 * Confirmation dialog. [destructive] swaps the confirm button to the danger style; the
 * message says exactly what will happen.
 */
@Composable
fun SubzeroDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(R.string.core_designsystem_cancel),
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SubzeroDialogContent(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            dismissLabel = dismissLabel,
            destructive = destructive,
            modifier = modifier,
        )
    }
}

@Composable
private fun SubzeroDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String,
    destructive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val shape = SubzeroTheme.shapes.lg
    Column(
        modifier = modifier
            .padding(spacing.xl)
            .widthIn(max = DialogMaxWidth)
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.outline, shape)
            .padding(spacing.xl),
    ) {
        Text(text = title, style = SubzeroTheme.typography.title, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.xs))
        Text(text = message, style = SubzeroTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.xl))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SubzeroButton(
                text = dismissLabel,
                onClick = onDismiss,
                style = SubzeroButtonStyle.Secondary,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            SubzeroButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) SubzeroButtonStyle.Danger else SubzeroButtonStyle.Primary,
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroDialogPreview() {
    PreviewTheme {
        SubzeroDialogContent(
            title = "Delete Netflix?",
            message = "This removes the subscription and its history. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
            dismissLabel = "Cancel",
            destructive = true,
        )
    }
}
