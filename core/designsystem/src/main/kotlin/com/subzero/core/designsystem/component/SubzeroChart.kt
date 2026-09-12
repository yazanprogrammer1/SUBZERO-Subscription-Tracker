package com.subzero.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroGradients
import com.subzero.core.designsystem.theme.SubzeroTheme

/** One data point; [label] is the axis text, [value] the plotted magnitude (already scaled by the caller). */
@Immutable
data class ChartPoint(val label: String, val value: Float, val highlighted: Boolean = false)

private const val GRID_LINES = 3
private const val BAR_WIDTH_FRACTION = 0.55f
private const val MIN_BAR_HEIGHT_PX = 2f

/**
 * Bar chart drawn on a Canvas (design-system.md §7). Bars grow from the baseline on first
 * appearance; the highlighted bar (e.g. the current month) uses the accent, others a muted tone.
 * [contentDescription] should summarize the series for TalkBack.
 */
@Composable
fun SubzeroBarChart(
    points: List<ChartPoint>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val colors = SubzeroTheme.colors
    val progress = rememberRevealProgress(points)
    val max = points.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
    val barColor = colors.accent.copy(alpha = 0.35f)
    val gridColor = colors.outline

    Column(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawGrid(gridColor)
            if (points.isEmpty()) return@Canvas
            val slot = size.width / points.size
            val barWidth = slot * BAR_WIDTH_FRACTION
            points.forEachIndexed { index, point ->
                val fullHeight = (point.value / max) * size.height
                val barHeight = (fullHeight * progress.value).coerceAtLeast(MIN_BAR_HEIGHT_PX)
                val left = slot * index + (slot - barWidth) / 2
                drawRoundRect(
                    color = if (point.highlighted) colors.accent else barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 3),
                )
            }
        }
        AxisLabels(points)
    }
}

/**
 * Line chart with a gradient fill. The line draws left to right on first appearance.
 */
@Composable
fun SubzeroLineChart(
    points: List<ChartPoint>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val colors = SubzeroTheme.colors
    val progress = rememberRevealProgress(points)
    val max = points.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
    val gridColor = colors.outline
    val fill = SubzeroGradients.chartFill(colors)
    val strokeWidth = 2.5.dp

    Column(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawGrid(gridColor)
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1)
            val inset = strokeWidth.toPx()
            val usable = size.height - inset * 2
            val coords = points.mapIndexed { i, p -> Offset(stepX * i, inset + usable - (p.value / max) * usable) }

            val line = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val cur = coords[i]
                    val midX = (prev.x + cur.x) / 2
                    cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }
            clipRect(right = size.width * progress.value) {
                drawPath(area, brush = fill)
                drawPath(line, color = colors.accent, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
                coords.zip(points).filter { it.second.highlighted }.forEach { (c, _) ->
                    drawCircle(colors.background, radius = strokeWidth.toPx() * 2.2f, center = c)
                    drawCircle(colors.accentBright, radius = strokeWidth.toPx() * 1.4f, center = c)
                }
            }
        }
        AxisLabels(points)
    }
}

@Composable
private fun rememberRevealProgress(points: List<ChartPoint>): Animatable<Float, *> {
    val motion = SubzeroTheme.motion
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, motion.revealSpec())
    }
    return progress
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(color: androidx.compose.ui.graphics.Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
    for (i in 0..GRID_LINES) {
        val y = size.height * i / GRID_LINES
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f, pathEffect = if (i == GRID_LINES) null else dash)
    }
}

@Composable
private fun AxisLabels(points: List<ChartPoint>) {
    if (points.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth().padding(top = SubzeroTheme.spacing.xs)) {
        points.forEach { point ->
            Text(
                text = point.label,
                style = SubzeroTheme.typography.caption,
                color = if (point.highlighted) SubzeroTheme.colors.textPrimary else SubzeroTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val previewPoints = listOf(
    ChartPoint("Apr", 72f), ChartPoint("May", 74f), ChartPoint("Jun", 80f),
    ChartPoint("Jul", 80f), ChartPoint("Aug", 81f), ChartPoint("Sep", 87f, highlighted = true),
)

@SubzeroPreviews
@Composable
private fun SubzeroChartPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroBarChart(points = previewPoints, contentDescription = "Monthly spending, April to September")
            SubzeroLineChart(points = previewPoints, contentDescription = "Spending trend", modifier = Modifier.padding(top = 24.dp))
            SubzeroBarChart(points = emptyList(), contentDescription = "No data", modifier = Modifier.padding(top = 24.dp), height = 80.dp)
        }
    }
}
