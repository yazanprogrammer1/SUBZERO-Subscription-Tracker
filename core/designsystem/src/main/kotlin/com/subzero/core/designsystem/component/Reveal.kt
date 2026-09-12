package com.subzero.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme

private const val REVEAL_SLIDE_FRACTION = 6

/**
 * Reveals [content] once it enters the composition and [visible] is true: fade + short rise,
 * delayed by [index] × stagger so lists and insight cards appear progressively
 * (design-system.md §6.1). Pagers pass `visible = isCurrentPage` so pre-rendered neighbours
 * reveal only when they arrive on screen.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val motion = SubzeroTheme.motion
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    AnimatedVisibility(
        visible = visible && started,
        modifier = modifier,
        enter = fadeIn(motion.revealSpec(delayMillis = motion.staggerDelay(index))) +
            slideInVertically(motion.revealSpec(delayMillis = motion.staggerDelay(index))) { it / REVEAL_SLIDE_FRACTION },
        exit = fadeOut(motion.fastSpec()),
    ) {
        content()
    }
}

/** Pager indicator: the current page is a pill, the others are dots. */
@Composable
fun SubzeroPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    Row(
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { page ->
            val selected = page == currentPage
            val width by animateDpAsState(if (selected) 22.dp else 6.dp, motion.standardSpec(), label = "dotWidth")
            val color by animateColorAsState(if (selected) colors.accent else colors.outlineStrong, motion.standardSpec(), label = "dotColor")
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(width)
                    .height(6.dp)
                    .clip(SubzeroTheme.shapes.full)
                    .background(color),
            )
        }
    }
}

@SubzeroPreviews
@Composable
private fun RevealPreview() {
    PreviewTheme {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            repeat(3) { i ->
                StaggeredReveal(index = i) {
                    SubzeroCard(modifier = Modifier.padding(bottom = 8.dp)) {
                        androidx.compose.material3.Text("Card $i", color = SubzeroTheme.colors.textPrimary)
                    }
                }
            }
            SubzeroPageIndicator(pageCount = 5, currentPage = 1, modifier = Modifier.padding(top = 8.dp).size(width = 80.dp, height = 6.dp))
        }
    }
}
