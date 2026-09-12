package com.subzero.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader

/**
 * Gradient recipes (design-system.md §1.2). Built from [SubzeroColors] so they follow the theme;
 * size-dependent ones are [ShaderBrush]es that resolve against the drawn area.
 */
object SubzeroGradients {

    private const val HERO_GLOW_ALPHA = 0.18f
    private const val ONBOARDING_GLOW_ALPHA = 0.22f
    private const val SHEEN_ALPHA = 0.04f
    private const val CHART_FILL_ALPHA = 0.28f

    /** Radial icy glow anchored top-right; hero card only. */
    fun heroGlow(colors: SubzeroColors): Brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = RadialGradientShader(
            center = Offset(size.width * 0.85f, size.height * 0.1f),
            radius = size.width * 1.2f,
            colors = listOf(colors.accentBright.copy(alpha = HERO_GLOW_ALPHA), Color.Transparent),
        )
    }

    /** Vertical white sheen over the top 40% of a card. */
    fun cardSheen(colors: SubzeroColors): Brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(0f, size.height * 0.4f),
            colors = listOf(colors.outlineHighlight.copy(alpha = SHEEN_ALPHA), Color.Transparent),
            tileMode = TileMode.Clamp,
        )
    }

    /** Primary button fill. */
    fun accentButton(colors: SubzeroColors): Brush = Brush.linearGradient(
        colors = listOf(colors.accent, colors.accentBright),
    )

    /** Hero card ground: elevated surface fading into surface. */
    fun heroSurface(colors: SubzeroColors): Brush = Brush.verticalGradient(
        colors = listOf(colors.surfaceElevated, colors.surface),
    )

    /** Violet-blue radial glow anchored at the bottom; onboarding pages. */
    fun onboardingGlow(colors: SubzeroColors): Brush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = RadialGradientShader(
            center = Offset(size.width * 0.5f, size.height * 0.95f),
            radius = size.width * 0.9f,
            colors = listOf(colors.secondaryAccent.copy(alpha = ONBOARDING_GLOW_ALPHA), Color.Transparent),
        )
    }

    /** Area fill under a line chart. */
    fun chartFill(colors: SubzeroColors): Brush = Brush.verticalGradient(
        colors = listOf(colors.accent.copy(alpha = CHART_FILL_ALPHA), Color.Transparent),
    )
}
