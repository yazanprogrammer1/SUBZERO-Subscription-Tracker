package com.subzero.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for SUBZERO (docs/design/design-system.md §1).
 *
 * Components read colors from [SubzeroTheme.colors], never from raw hex values.
 * Dark is the primary experience; the light palette maps the same semantics.
 */
@Immutable
data class SubzeroColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val surfaceSubtle: Color,
    val outline: Color,
    val outlineStrong: Color,
    val outlineHighlight: Color,
    val accent: Color,
    val accentBright: Color,
    val accentContainer: Color,
    val onAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    /** Text color for a given tone, so callers never pick a hue for status text. */
    fun forTone(tone: SubzeroTone): Color = when (tone) {
        SubzeroTone.Neutral -> textSecondary
        SubzeroTone.Accent -> accent
        SubzeroTone.Positive -> positive
        SubzeroTone.Warning -> warning
        SubzeroTone.Danger -> danger
    }

    fun containerForTone(tone: SubzeroTone): Color = when (tone) {
        SubzeroTone.Neutral -> surfaceSubtle
        SubzeroTone.Accent -> accentContainer
        SubzeroTone.Positive -> positiveContainer
        SubzeroTone.Warning -> warningContainer
        SubzeroTone.Danger -> dangerContainer
    }
}

/** Semantic emphasis used by icons, chips and insight cards. */
enum class SubzeroTone { Neutral, Accent, Positive, Warning, Danger }

private const val CONTAINER_ALPHA_DARK = 0.14f
private const val CONTAINER_ALPHA_LIGHT = 0.12f

internal val DarkColors: SubzeroColors = run {
    val accent = Color(0xFF4DA3FF)
    val positive = Color(0xFF3DDC97)
    val warning = Color(0xFFF5B84B)
    val danger = Color(0xFFFF5C6C)
    val surface = Color(0xFF0E1526)
    SubzeroColors(
        background = Color(0xFF070B14),
        surface = surface,
        surfaceElevated = Color(0xFF141D33),
        surfaceGlass = surface.copy(alpha = 0.72f),
        surfaceSubtle = Color(0x0AFFFFFF),
        outline = Color(0x1F7AA2FF),
        outlineStrong = Color(0x477AA2FF),
        outlineHighlight = Color(0x0FFFFFFF),
        accent = accent,
        accentBright = Color(0xFF5EE7FF),
        accentContainer = accent.copy(alpha = CONTAINER_ALPHA_DARK),
        onAccent = Color(0xFF05101F),
        secondaryAccent = Color(0xFF6F7CFF),
        textPrimary = Color(0xFFF2F6FF),
        textSecondary = Color(0xFF9AA8C7),
        textTertiary = Color(0xFF5F6D8C),
        positive = positive,
        positiveContainer = positive.copy(alpha = CONTAINER_ALPHA_DARK),
        warning = warning,
        warningContainer = warning.copy(alpha = CONTAINER_ALPHA_DARK),
        danger = danger,
        dangerContainer = danger.copy(alpha = CONTAINER_ALPHA_DARK),
        scrim = Color(0x99000000),
        isDark = true,
    )
}

internal val LightColors: SubzeroColors = run {
    val accent = Color(0xFF1F6FEB)
    val positive = Color(0xFF1B9E6A)
    val warning = Color(0xFFB7791F)
    val danger = Color(0xFFD64550)
    val surface = Color(0xFFFFFFFF)
    SubzeroColors(
        background = Color(0xFFF4F7FC),
        surface = surface,
        surfaceElevated = surface,
        surfaceGlass = surface.copy(alpha = 0.85f),
        surfaceSubtle = Color(0x0A0B1220),
        outline = Color(0x1F1E3A8A),
        outlineStrong = Color(0x471E3A8A),
        outlineHighlight = Color.Transparent,
        accent = accent,
        accentBright = Color(0xFF0EA5E9),
        accentContainer = accent.copy(alpha = CONTAINER_ALPHA_LIGHT),
        onAccent = Color(0xFFFFFFFF),
        secondaryAccent = Color(0xFF5B5FE0),
        textPrimary = Color(0xFF0B1220),
        textSecondary = Color(0xFF4B5876),
        textTertiary = Color(0xFF8A96B3),
        positive = positive,
        positiveContainer = positive.copy(alpha = CONTAINER_ALPHA_LIGHT),
        warning = warning,
        warningContainer = warning.copy(alpha = CONTAINER_ALPHA_LIGHT),
        danger = danger,
        dangerContainer = danger.copy(alpha = CONTAINER_ALPHA_LIGHT),
        scrim = Color(0x660B1220),
        isDark = false,
    )
}

internal val LocalSubzeroColors = staticCompositionLocalOf { DarkColors }
