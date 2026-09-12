package com.subzero.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * SUBZERO type scale. Money values use the money variants with tabular figures so digits
 * line up and animated numbers do not jitter.
 *
 * The typeface is the system default until Phase 3 introduces the bundled brand font.
 */
@Immutable
data class SubzeroTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val moneyHero: TextStyle,
    val moneyLarge: TextStyle,
    val money: TextStyle,
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private const val TABULAR_FIGURES = "tnum"

internal fun subzeroTypography(fontFamily: FontFamily = FontFamily.Default): SubzeroTypography {
    val base = TextStyle(fontFamily = fontFamily, lineHeightStyle = lineHeightStyle)
    return SubzeroTypography(
        display = base.copy(
            fontSize = 44.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        headline = base.copy(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        title = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        body = base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodySmall = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        label = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        caption = base.copy(
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
        ),
        moneyHero = base.copy(
            fontSize = 44.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        moneyLarge = base.copy(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        money = base.copy(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
    )
}

internal val LocalSubzeroTypography = staticCompositionLocalOf { subzeroTypography() }
