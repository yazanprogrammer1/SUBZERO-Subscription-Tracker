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
import java.util.Locale

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

/** Scripts the type scale is tuned for. */
enum class TypeScript {
    LATIN,
    ARABIC,
    ;

    companion object {
        private val arabicScriptLanguages = setOf("ar", "fa", "ur", "ps", "ckb", "sd", "ug")

        fun of(locale: Locale): TypeScript =
            if (locale.language in arabicScriptLanguages) ARABIC else LATIN
    }
}

private const val TABULAR_FIGURES = "tnum"

/** Headings step down in Arabic; body sizes stay put because Arabic reads worse when small. */
private const val ARABIC_HEADING_SCALE = 0.86f

/** Minimum leading as a multiple of the font size, so Arabic descenders are never clipped. */
private const val ARABIC_MIN_LEADING = 1.5f

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

/**
 * Builds the scale for [script].
 *
 * Inter carries no Arabic glyphs, so Arabic text is drawn by the system Arabic face: taller
 * letters with real descenders — the final م of "التقويم" hangs below the baseline — while the
 * line box is still reserved from Inter's metrics, which clips them. For Arabic the headings
 * come down one step so long words fit the top bar, every line gets at least
 * [ARABIC_MIN_LEADING] times its font size, and tracking goes to zero because Arabic is a
 * connected script that negative tracking visibly damages.
 */
internal fun subzeroTypography(
    fontFamily: FontFamily = InterFontFamily,
    script: TypeScript = TypeScript.LATIN,
): SubzeroTypography {
    val arabic = script == TypeScript.ARABIC

    fun style(
        size: Float,
        line: Float,
        weight: FontWeight,
        tracking: Float = 0f,
        heading: Boolean = false,
        tabular: Boolean = false,
    ): TextStyle {
        val scale = if (arabic && heading) ARABIC_HEADING_SCALE else 1f
        val fontSize = size * scale
        val minLine = if (arabic) fontSize * ARABIC_MIN_LEADING else 0f
        return TextStyle(
            fontFamily = fontFamily,
            lineHeightStyle = lineHeightStyle,
            fontSize = fontSize.sp,
            lineHeight = maxOf(line * scale, minLine).sp,
            fontWeight = weight,
            letterSpacing = (if (arabic) 0f else tracking).sp,
            fontFeatureSettings = if (tabular) TABULAR_FIGURES else null,
        )
    }

    return SubzeroTypography(
        display = style(44f, 52f, FontWeight.SemiBold, tracking = -0.5f, heading = true),
        headline = style(28f, 34f, FontWeight.SemiBold, tracking = -0.3f, heading = true),
        title = style(20f, 26f, FontWeight.Medium, heading = true),
        body = style(16f, 24f, FontWeight.Normal),
        bodySmall = style(14f, 20f, FontWeight.Normal),
        label = style(13f, 18f, FontWeight.Medium),
        caption = style(11f, 16f, FontWeight.Normal, tracking = 0.2f),
        moneyHero = style(44f, 52f, FontWeight.Bold, tracking = -1f, heading = true, tabular = true),
        moneyLarge = style(28f, 34f, FontWeight.SemiBold, tracking = -0.5f, heading = true, tabular = true),
        money = style(16f, 24f, FontWeight.Medium, tabular = true),
        moneySmall = style(13f, 18f, FontWeight.Medium, tabular = true),
    )
}

internal val LocalSubzeroTypography = staticCompositionLocalOf { subzeroTypography(FontFamily.Default) }
