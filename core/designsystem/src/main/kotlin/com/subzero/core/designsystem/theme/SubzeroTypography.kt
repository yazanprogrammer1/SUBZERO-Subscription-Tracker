package com.subzero.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.subzero.core.designsystem.R

/**
 * SUBZERO type scale (design-system.md §2). Inter variable font, bundled.
 * Money styles enable tabular figures so digits keep constant width while animating.
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
    val moneySmall: TextStyle,
)

private const val TABULAR_FIGURES = "tnum"

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** One variable font file provides every weight; each entry pins the `wght` axis. */
internal val InterFontFamily: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resId = R.font.inter_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

internal fun subzeroTypography(fontFamily: FontFamily = InterFontFamily): SubzeroTypography {
    val base = TextStyle(fontFamily = fontFamily, lineHeightStyle = lineHeightStyle)
    val money = base.copy(fontFeatureSettings = TABULAR_FIGURES)
    return SubzeroTypography(
        display = base.copy(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headline = base.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        title = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        body = base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodySmall = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        label = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        caption = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
        moneyHero = money.copy(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        moneyLarge = money.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        money = money.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        moneySmall = money.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    )
}

internal val LocalSubzeroTypography = staticCompositionLocalOf { subzeroTypography(FontFamily.Default) }
