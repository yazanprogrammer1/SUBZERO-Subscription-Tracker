package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.subzero.core.designsystem.theme.SubzeroTheme

/**
 * TEMPORARY. Stands in for a screen whose feature phase has not been built yet, so the
 * navigation shell can be exercised end-to-end. Deleted as each feature ships.
 */
@Composable
fun SubzeroScreenPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SubzeroTheme.colors.background)
            .safeDrawingPadding()
            .padding(SubzeroTheme.spacing.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = SubzeroTheme.typography.headline,
            color = SubzeroTheme.colors.textPrimary,
        )
    }
}

@Preview
@Composable
private fun SubzeroScreenPlaceholderPreview() {
    SubzeroTheme(darkTheme = true) {
        SubzeroScreenPlaceholder(title = "Home")
    }
}
