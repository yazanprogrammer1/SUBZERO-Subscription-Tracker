package com.subzero.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private const val GLOW_ALPHA = 0.35f

/**
 * The SUBZERO mark: an angular "S" bolt, accent gradient with a soft icy glow behind it.
 * Drawn on a Canvas so it scales crisply at any size.
 */
@Composable
fun SubzeroLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = null,
) {
    val colors = SubzeroTheme.colors
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        val w = this.size.width
        val h = this.size.height
        // Soft glow disc behind the mark.
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.accentBright.copy(alpha = GLOW_ALPHA), colors.accentBright.copy(alpha = 0f))),
            radius = w * 0.55f,
            center = Offset(w / 2, h / 2),
        )
        val path = Path().apply {
            moveTo(w * 0.60f, h * 0.14f)
            lineTo(w * 0.36f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.48f)
            lineTo(w * 0.40f, h * 0.86f)
            lineTo(w * 0.64f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            close()
        }
        val fill = Brush.linearGradient(
            colors = listOf(colors.accentBright, colors.accent),
            start = Offset(w * 0.4f, h * 0.1f),
            end = Offset(w * 0.6f, h * 0.9f),
        )
        drawPath(path, brush = fill)
        drawPath(path, brush = fill, style = Stroke(width = w * 0.02f, join = StrokeJoin.Round))
    }
}

/** Mark + spaced wordmark, for onboarding and about screens. */
@Composable
fun SubzeroWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 72.dp,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SubzeroLogoMark(size = markSize)
        Spacer(Modifier.height(SubzeroTheme.spacing.sm))
        Text(
            text = "SUBZERO",
            style = SubzeroTheme.typography.title.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 6.sp),
            color = SubzeroTheme.colors.textPrimary,
        )
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroLogoPreview() {
    PreviewTheme {
        SubzeroWordmark(modifier = Modifier.padding(24.dp))
    }
}
