package com.subzero.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class SubzeroTypographyTest {

    private val latin = subzeroTypography(FontFamily.Default)
    private val arabic = subzeroTypography(FontFamily.Default, TypeScript.ARABIC)

    private fun SubzeroTypography.all(): List<TextStyle> =
        listOf(display, headline, title, body, bodySmall, label, caption, moneyHero, moneyLarge, money, moneySmall)

    @Test
    fun `arabic headings step down while body text keeps its size`() {
        assertThat(arabic.headline.fontSize.value).isLessThan(latin.headline.fontSize.value)
        assertThat(arabic.title.fontSize.value).isLessThan(latin.title.fontSize.value)
        assertThat(arabic.display.fontSize.value).isLessThan(latin.display.fontSize.value)
        assertThat(arabic.body.fontSize.value).isEqualTo(latin.body.fontSize.value)
        assertThat(arabic.caption.fontSize.value).isEqualTo(latin.caption.fontSize.value)
    }

    @Test
    fun `every arabic line box leaves room for descenders`() {
        arabic.all().forEach { style ->
            assertThat(style.lineHeight.value / style.fontSize.value).isAtLeast(1.5f)
        }
    }

    @Test
    fun `arabic drops the negative tracking that damages a connected script`() {
        arabic.all().forEach { assertThat(it.letterSpacing.value).isEqualTo(0f) }
        assertThat(latin.headline.letterSpacing.value).isLessThan(0f)
    }

    @Test
    fun `latin metrics are unchanged`() {
        assertThat(latin.headline.fontSize.value).isEqualTo(28f)
        assertThat(latin.headline.lineHeight.value).isEqualTo(34f)
        assertThat(latin.body.lineHeight.value).isEqualTo(24f)
    }

    @Test
    fun `script comes from the locale language`() {
        assertThat(TypeScript.of(Locale.forLanguageTag("ar"))).isEqualTo(TypeScript.ARABIC)
        assertThat(TypeScript.of(Locale.forLanguageTag("ar-EG"))).isEqualTo(TypeScript.ARABIC)
        assertThat(TypeScript.of(Locale.forLanguageTag("fa"))).isEqualTo(TypeScript.ARABIC)
        assertThat(TypeScript.of(Locale.ENGLISH)).isEqualTo(TypeScript.LATIN)
    }
}
