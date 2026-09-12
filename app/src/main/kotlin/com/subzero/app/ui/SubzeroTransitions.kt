package com.subzero.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.ui.NavDisplay
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.navigation.NavTransitions

/**
 * Builds the per-destination transition metadata (design-system.md §6.2) from the motion
 * tokens. Tabs fade through; pushed screens slide from the end over a slightly receding
 * underlay; modal forms rise from the bottom. Predictive back mirrors the pop.
 */
@Composable
internal fun rememberNavTransitions(): NavTransitions {
    val motion = SubzeroTheme.motion
    return remember(motion) { buildNavTransitions(motion) }
}

private const val PUSH_SLIDE_FRACTION = 4
private const val UNDERLAY_ALPHA = 0.9f

private fun buildNavTransitions(motion: SubzeroMotion): NavTransitions {
    val tab = (fadeIn(motion.standardSpec()) + scaleIn(motion.standardSpec(), initialScale = SubzeroMotion.TAB_ENTER_SCALE))
        .togetherWith(fadeOut(motion.standardSpec()))

    val push = pushEnter(motion) togetherWith underlayExit(motion)
    val pop = underlayEnter(motion) togetherWith popExit(motion)

    val modalEnter = modalEnter(motion) togetherWith underlayExit(motion)
    val modalExit = underlayEnter(motion) togetherWith modalExit(motion)

    return NavTransitions(
        tab = NavDisplay.transitionSpec { tab } +
            NavDisplay.popTransitionSpec { tab } +
            NavDisplay.predictivePopTransitionSpec { tab },
        push = NavDisplay.transitionSpec { push } +
            NavDisplay.popTransitionSpec { pop } +
            NavDisplay.predictivePopTransitionSpec { pop },
        modal = NavDisplay.transitionSpec { modalEnter } +
            NavDisplay.popTransitionSpec { modalExit } +
            NavDisplay.predictivePopTransitionSpec { modalExit },
    )
}

private fun pushEnter(motion: SubzeroMotion): EnterTransition =
    slideInHorizontally(motion.emphasizedEnterSpec()) { it / PUSH_SLIDE_FRACTION } + fadeIn(motion.emphasizedEnterSpec())

private fun popExit(motion: SubzeroMotion): ExitTransition =
    slideOutHorizontally(motion.emphasizedExitSpec()) { it / PUSH_SLIDE_FRACTION } + fadeOut(motion.emphasizedExitSpec())

private fun underlayExit(motion: SubzeroMotion): ExitTransition =
    fadeOut(motion.emphasizedExitSpec(), targetAlpha = UNDERLAY_ALPHA) +
        scaleOut(motion.emphasizedExitSpec(), targetScale = SubzeroMotion.TAB_ENTER_SCALE)

private fun underlayEnter(motion: SubzeroMotion): EnterTransition =
    fadeIn(motion.emphasizedEnterSpec(), initialAlpha = UNDERLAY_ALPHA) +
        scaleIn(motion.emphasizedEnterSpec(), initialScale = SubzeroMotion.TAB_ENTER_SCALE)

private fun modalEnter(motion: SubzeroMotion): EnterTransition =
    slideInVertically(motion.emphasizedEnterSpec()) { it } + fadeIn(motion.emphasizedEnterSpec())

private fun modalExit(motion: SubzeroMotion): ExitTransition =
    slideOutVertically(motion.emphasizedExitSpec()) { it } + fadeOut(motion.emphasizedExitSpec())
