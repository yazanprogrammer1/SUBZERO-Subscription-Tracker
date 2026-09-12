package com.subzero.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroPageIndicator
import com.subzero.core.designsystem.component.StaggeredReveal
import com.subzero.core.designsystem.theme.SubzeroTheme
import kotlinx.coroutines.launch

private const val GLOW_DRIFT_MS = 9000
private const val GLOW_ALPHA = 0.22f
private val ContentMaxWidth = 560.dp

internal object OnboardingTestTags {
    const val PAGER = "onboarding_pager"
    const val NEXT = "onboarding_next"
    const val SKIP = "onboarding_skip"
    const val ADD_FIRST = "onboarding_add_first"
    const val SKIP_FOR_NOW = "onboarding_skip_for_now"
}

/** Entry point wired to the ViewModel; [onFinished] fires once completion is persisted. */
@Composable
fun OnboardingRoute(
    onFinished: (OnboardingOutcome) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    LaunchedEffect(completed) {
        completed?.let(onFinished)
    }
    OnboardingScreen(onComplete = viewModel::complete)
}

/** The five-page flow. Stateless apart from the pager, so it is previewable and testable. */
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = onboardingPages
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val motion = SubzeroTheme.motion
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .driftingGlow(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .testTag(OnboardingTestTags.PAGER),
                beyondViewportPageCount = 1,
            ) { index ->
                OnboardingPage(page = pages[index], visible = pagerState.currentPage == index)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ContentMaxWidth)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = spacing.screen)
                    .padding(bottom = spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SubzeroPageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    contentDescription = stringResource(
                        R.string.feature_onboarding_page_indicator,
                        pagerState.currentPage + 1,
                        pages.size,
                    ),
                )
                Spacer(Modifier.height(spacing.xl))
                AnimatedContent(
                    targetState = isLast,
                    transitionSpec = { fadeIn(motion.standardSpec()) togetherWith fadeOut(motion.fastSpec()) },
                    label = "onboardingActions",
                ) { last ->
                    if (last) {
                        FinalActions(onComplete = onComplete)
                    } else {
                        PagingActions(
                            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            onSkip = { scope.launch { pagerState.animateScrollToPage(pages.lastIndex) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPageSpec, visible: Boolean) {
    val spacing = SubzeroTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.huge))
        StaggeredReveal(index = 0, visible = visible) {
            Text(
                text = stringResource(page.title),
                style = SubzeroTheme.typography.display,
                color = SubzeroTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = ContentMaxWidth),
            )
        }
        Spacer(Modifier.height(spacing.sm))
        StaggeredReveal(index = 1, visible = visible) {
            Text(
                text = stringResource(page.body),
                style = SubzeroTheme.typography.body,
                color = SubzeroTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = ContentMaxWidth),
            )
        }
        Spacer(Modifier.height(spacing.xxl))
        Column(
            modifier = Modifier.widthIn(max = ContentMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            page.illustration(this, visible)
        }
        Spacer(Modifier.height(spacing.xl))
    }
}

@Composable
private fun PagingActions(onNext: () -> Unit, onSkip: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SubzeroButton(
            text = stringResource(R.string.feature_onboarding_next),
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.NEXT),
        )
        Spacer(Modifier.height(SubzeroTheme.spacing.xs))
        SubzeroButton(
            text = stringResource(R.string.feature_onboarding_skip),
            onClick = onSkip,
            style = SubzeroButtonStyle.Ghost,
            compact = true,
            modifier = Modifier.testTag(OnboardingTestTags.SKIP),
        )
    }
}

@Composable
private fun FinalActions(onComplete: (OnboardingOutcome) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SubzeroButton(
            text = stringResource(R.string.feature_onboarding_add_first),
            onClick = { onComplete(OnboardingOutcome.AddFirstSubscription) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.ADD_FIRST),
        )
        Spacer(Modifier.height(SubzeroTheme.spacing.xs))
        SubzeroButton(
            text = stringResource(R.string.feature_onboarding_skip_for_now),
            onClick = { onComplete(OnboardingOutcome.Skipped) },
            style = SubzeroButtonStyle.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.SKIP_FOR_NOW),
        )
        Spacer(Modifier.height(SubzeroTheme.spacing.sm))
        Text(
            text = stringResource(R.string.feature_onboarding_reminders_hint),
            style = SubzeroTheme.typography.caption,
            color = SubzeroTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A violet-blue glow anchored near the bottom that drifts slowly, giving the pages depth
 * without competing with content. Static under reduced motion.
 */
@Composable
private fun Modifier.driftingGlow(): Modifier {
    val colors = SubzeroTheme.colors
    val reduce = SubzeroTheme.motion.reduceMotion
    val phase: Float = if (reduce) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "glowDrift")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(GLOW_DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
            label = "glowPhase",
        )
        value
    }
    val glow = colors.secondaryAccent.copy(alpha = GLOW_ALPHA)
    return drawBehind {
        val center = Offset(size.width * (0.3f + 0.4f * phase), size.height * (0.9f - 0.1f * phase))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(glow, Color.Transparent),
                center = center,
                radius = size.width * 0.9f,
            ),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF070B14, heightDp = 800)
@Composable
private fun OnboardingScreenPreview() {
    SubzeroTheme(darkTheme = true) {
        OnboardingScreen(onComplete = {})
    }
}
