package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

/**
 * Modal bottom sheet for filters, pickers and quick actions (design-system.md §7).
 * Wraps Material's sheet for its gesture and accessibility handling; the visuals are ours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubzeroBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SubzeroTheme.colors
    val sheetState = rememberModalBottomSheetState()
    val shape = SubzeroTheme.shapes.xl.copy(
        bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
        bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = shape,
        containerColor = colors.surfaceElevated,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        dragHandle = { SheetHandle() },
    ) {
        SubzeroSheetContent(title = title, content = content)
    }
}

@Composable
private fun SheetHandle() {
    val description = stringResource(R.string.core_designsystem_drag_handle)
    Box(
        modifier = Modifier
            .padding(top = SubzeroTheme.spacing.sm, bottom = SubzeroTheme.spacing.xs)
            .width(36.dp)
            .height(4.dp)
            .clip(SubzeroTheme.shapes.full)
            .background(SubzeroTheme.colors.outlineStrong)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun SubzeroSheetContent(title: String?, content: @Composable ColumnScope.() -> Unit) {
    val spacing = SubzeroTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screen)
            .padding(bottom = spacing.xl)
            .navigationBarsPadding(),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = SubzeroTheme.typography.title,
                color = SubzeroTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = spacing.md),
            )
        }
        content()
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroBottomSheetPreview() {
    PreviewTheme {
        Column(modifier = Modifier.background(SubzeroTheme.colors.surfaceElevated), horizontalAlignment = Alignment.CenterHorizontally) {
            SheetHandle()
            SubzeroSheetContent(title = "Sort by") {
                SubzeroChip(text = "Highest cost", selected = true, onClick = {})
            }
        }
    }
}
