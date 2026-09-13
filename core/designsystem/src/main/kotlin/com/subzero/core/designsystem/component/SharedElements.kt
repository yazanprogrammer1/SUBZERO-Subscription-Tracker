package com.subzero.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.subzero.core.designsystem.theme.SubzeroTheme

/**
 * Provided by the app shell around its NavDisplay so screens can share elements across
 * destinations. Null in previews, tests and hosts without a SharedTransitionLayout, in which
 * case [sharedBoundsIfAvailable] is a no-op.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * The AnimatedVisibilityScope of the current navigation entry, bridged by each entry from the
 * navigation library. Null outside navigation (previews, tests), where shared bounds are skipped.
 */
val LocalNavEntryAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Stable shared-element keys, so the row and the detail header agree without string literals. */
object SharedElementKeys {
    fun subscriptionCard(id: String) = "subscription-card-$id"
    fun subscriptionIcon(id: String) = "subscription-icon-$id"
    fun subscriptionName(id: String) = "subscription-name-$id"
}

/**
 * Shares this element's bounds with the element carrying the same [key] on another screen,
 * animating with the emphasized spec. Skips silently when no shared transition is in flight
 * or under reduced motion.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsIfAvailable(
    key: String,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    val scope = LocalSharedTransitionScope.current
    val motion = SubzeroTheme.motion
    if (scope == null || animatedVisibilityScope == null || motion.reduceMotion) return this
    return with(scope) {
        this@sharedBoundsIfAvailable.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = { _, _ -> motion.emphasizedEnterSpec() },
        )
    }
}
