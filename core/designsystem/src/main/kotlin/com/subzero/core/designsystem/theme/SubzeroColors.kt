package com.subzero.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for SUBZERO.
 *
 * Components read colors from [SubzeroTheme.colors], never from raw hex values.
 * Dark is the primary experience; the light palette maps the same semantics.
 */
@Immutable
data class SubzeroColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val outlineHighlight: Color,
    val accent: Color,
    val accentBright: Color,
    val secondaryAccent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean,
)

internal val DarkColors = SubzeroColors(
    background = Color(0xFF070B14),
    surface = Color(0xFF0E1526),
    surfaceElevated = Color(0xFF141D33),
    outline = Color(0x1F7AA2FF),
    outlineHighlight = Color(0x0FFFFFFF),
    accent = Color(0xFF4DA3FF),
    accentBright = Color(0xFF5EE7FF),
    secondaryAccent = Color(0xFF6F7CFF),
    onAccent = Color(0xFF05101F),
    textPrimary = Color(0xFFF2F6FF),
    textSecondary = Color(0xFF9AA8C7),
    textTertiary = Color(0xFF5F6D8C),
    positive = Color(0xFF3DDC97),
    warning = Color(0xFFF5B84B),
    danger = Color(0xFFFF5C6C),
    isDark = true,
)

internal val LightColors = SubzeroColors(
    background = Color(0xFFF4F7FC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    outline = Color(0x1F1E3A8A),
    outlineHighlight = Color(0x00FFFFFF),
    accent = Color(0xFF1F6FEB),
    accentBright = Color(0xFF0EA5E9),
    secondaryAccent = Color(0xFF5B5FE0),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0B1220),
    textSecondary = Color(0xFF4B5876),
    textTertiary = Color(0xFF8A96B3),
    positive = Color(0xFF1B9E6A),
    warning = Color(0xFFB7791F),
    danger = Color(0xFFD64550),
    isDark = false,
)

internal val LocalSubzeroColors = staticCompositionLocalOf { DarkColors }
