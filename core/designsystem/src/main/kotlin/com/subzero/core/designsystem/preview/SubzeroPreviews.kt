package com.subzero.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.subzero.core.designsystem.theme.SubzeroTheme

/** Every reusable component renders in dark, light and at 200% font scale. */
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0xFF070B14)
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, backgroundColor = 0xFFF4F7FC)
@Preview(name = "Dark 2x font", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2f, showBackground = true, backgroundColor = 0xFF070B14)
annotation class SubzeroPreviews

/** Theme wrapper for previews: picks dark/light from the preview uiMode and paints the ground. */
@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    SubzeroTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SubzeroTheme.colors.background),
        ) {
            content()
        }
    }
}
