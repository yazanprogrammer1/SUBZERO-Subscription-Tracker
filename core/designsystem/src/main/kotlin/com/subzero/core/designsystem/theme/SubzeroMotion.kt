package com.subzero.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * The motion system (design-system.md §6). Every animation in SUBZERO takes its duration and
 * easing from here. When the user has disabled animations at the system level, all durations
 * collapse to zero so content still appears, instantly.
 */
@Immutable
data class SubzeroMotion(
    val reduceMotion: Boolean,
) {
    val fast: Int get() = scaled(FAST_MS)
    val standard: Int get() = scaled(STANDARD_MS)
    val emphasized: Int get() = scaled(EMPHASIZED_MS)
    val reveal: Int get() = scaled(REVEAL_MS)
    val stagger: Int get() = scaled(STAGGER_MS)

    fun <T> fastSpec(): FiniteAnimationSpec<T> = tween(fast, easing = Standard)
    fun <T> standardSpec(): FiniteAnimationSpec<T> = tween(standard, easing = Standard)
    fun <T> emphasizedEnterSpec(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(emphasized, delayMillis = if (reduceMotion) 0 else delayMillis, easing = EmphasizedDecelerate)
    fun <T> emphasizedExitSpec(): FiniteAnimationSpec<T> = tween(emphasized, easing = EmphasizedAccelerate)
    fun <T> revealSpec(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(reveal, delayMillis = if (reduceMotion) 0 else delayMillis, easing = EmphasizedDecelerate)

    /** The only spring: press feedback, critically damped so it never overshoots. */
    fun pressSpec(): FiniteAnimationSpec<Float> =
        if (reduceMotion) tween(0) else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Delay for the [index]th item of a staggered reveal, capped so long lists never crawl. */
    fun staggerDelay(index: Int): Int = stagger * index.coerceAtMost(STAGGER_CAP)

    private fun scaled(ms: Int): Int = if (reduceMotion) 0 else ms

    companion object {
        const val FAST_MS = 120
        const val STANDARD_MS = 220
        const val EMPHASIZED_MS = 350
        const val REVEAL_MS = 400
        const val STAGGER_MS = 40
        const val STAGGER_CAP = 6

        /** Press scale for buttons. */
        const val PRESS_SCALE_BUTTON = 0.97f

        /** Press scale for cards and rows. */
        const val PRESS_SCALE_CARD = 0.985f

        /** Scale a screen settles from during a tab switch. */
        const val TAB_ENTER_SCALE = 0.98f

        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

internal val LocalSubzeroMotion = staticCompositionLocalOf { SubzeroMotion(reduceMotion = false) }

/** True when the system animator duration scale is 0 ("Remove animations" in accessibility settings). */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    return remember(context, inspection) {
        if (inspection) {
            false
        } else {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
    }
}
