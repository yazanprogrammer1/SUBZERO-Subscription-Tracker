package com.subzero.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private const val SHIMMER_PERIOD_MS = 1400
private const val SHIMMER_WIDTH_PX = 600f

/**
 * A shimmering placeholder block. Under reduced motion the shimmer is static.
 * Skeletons mirror the layout they replace so content does not jump when it arrives.
 */
@Composable
fun SubzeroSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = SubzeroTheme.shapes.sm,
) {
    val colors = SubzeroTheme.colors
    val base = colors.surfaceSubtle
    val highlight = colors.outlineStrong
    val brush = if (SubzeroTheme.motion.reduceMotion) {
        Brush.linearGradient(listOf(base, base))
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val offset by transition.animateFloat(
            initialValue = -SHIMMER_WIDTH_PX,
            targetValue = SHIMMER_WIDTH_PX * 3,
            animationSpec = infiniteRepeatable(tween(SHIMMER_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmerOffset",
        )
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(offset, 0f),
            end = Offset(offset + SHIMMER_WIDTH_PX, SHIMMER_WIDTH_PX / 3),
        )
    }
    Box(modifier = modifier.clip(shape).background(brush))
}

/** Skeleton for [SubzeroSubscriptionRow]. */
@Composable
fun SubzeroSubscriptionRowSkeleton(modifier: Modifier = Modifier) {
    val spacing = SubzeroTheme.spacing
    val loading = stringResource(R.string.core_designsystem_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .semantics { contentDescription = loading },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubzeroSkeleton(modifier = Modifier.size(44.dp))
        Spacer(Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(width = 120.dp)
            Spacer(Modifier.height(spacing.xs))
            SkeletonLine(width = 90.dp, height = 10.dp)
        }
        Column(horizontalAlignment = Alignment.End) {
            SkeletonLine(width = 56.dp)
            Spacer(Modifier.height(spacing.xs))
            SkeletonLine(width = 40.dp, height = 10.dp)
        }
    }
}

/** Skeleton for the dashboard hero card. */
@Composable
fun SubzeroHeroSkeleton(modifier: Modifier = Modifier) {
    val spacing = SubzeroTheme.spacing
    val loading = stringResource(R.string.core_designsystem_loading)
    SubzeroCard(modifier = modifier.fillMaxWidth().semantics { contentDescription = loading }, style = SubzeroCardStyle.Hero) {
        SkeletonLine(width = 100.dp, height = 12.dp)
        Spacer(Modifier.height(spacing.sm))
        SkeletonLine(width = 180.dp, height = 40.dp)
        Spacer(Modifier.height(spacing.sm))
        SkeletonLine(width = 140.dp, height = 12.dp)
    }
}

@Composable
private fun SkeletonLine(width: Dp, height: Dp = 14.dp) {
    SubzeroSkeleton(modifier = Modifier.width(width).height(height), shape = SubzeroTheme.shapes.full)
}

@SubzeroPreviews
@Composable
private fun SubzeroSkeletonPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroHeroSkeleton()
            Spacer(Modifier.height(12.dp))
            SubzeroSubscriptionRowSkeleton()
            SubzeroSubscriptionRowSkeleton()
        }
    }
}
