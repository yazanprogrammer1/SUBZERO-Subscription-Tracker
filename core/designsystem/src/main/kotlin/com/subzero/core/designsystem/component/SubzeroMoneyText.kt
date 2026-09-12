package com.subzero.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money

enum class MoneyTextSize { Hero, Large, Regular, Small }

/**
 * Formatted money that transitions when the value changes: the old value slides up and
 * fades, the new one rises in (design-system.md §6.3). Uses tabular figures so widths hold.
 *
 * The hero size steps down automatically once the formatted value gets long (§10).
 */
@Composable
fun SubzeroMoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    size: MoneyTextSize = MoneyTextSize.Regular,
    color: Color = SubzeroTheme.colors.textPrimary,
    compact: Boolean = false,
    animate: Boolean = true,
) {
    val formatter = SubzeroTheme.moneyFormatter
    val text = if (compact) formatter.formatCompact(money) else formatter.format(money)
    val typography = SubzeroTheme.typography
    val style: TextStyle = when (size) {
        MoneyTextSize.Hero -> if (text.length > HERO_MAX_CHARS) typography.moneyLarge else typography.moneyHero
        MoneyTextSize.Large -> typography.moneyLarge
        MoneyTextSize.Regular -> typography.money
        MoneyTextSize.Small -> typography.moneySmall
    }
    val motion = SubzeroTheme.motion

    if (!animate || motion.reduceMotion) {
        Text(text = text, style = style, color = color, modifier = modifier, maxLines = 1)
        return
    }

    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.length >= initialState.length) 1 else -1
            (slideInVertically(motion.standardSpec()) { direction * it / SLIDE_FRACTION } + fadeIn(motion.standardSpec()))
                .togetherWith(
                    slideOutVertically(motion.standardSpec()) { -direction * it / SLIDE_FRACTION } + fadeOut(motion.fastSpec()),
                )
        },
        label = "money",
    ) { value ->
        Text(text = value, style = style, color = color, maxLines = 1)
    }
}

private const val HERO_MAX_CHARS = 12
private const val SLIDE_FRACTION = 3

@SubzeroPreviews
@Composable
private fun SubzeroMoneyTextPreview() {
    PreviewTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroMoneyText(Money(8748, CurrencyCode.USD), size = MoneyTextSize.Hero)
            SubzeroMoneyText(Money(104976, CurrencyCode.USD), size = MoneyTextSize.Large, color = SubzeroTheme.colors.textSecondary)
            SubzeroMoneyText(Money(1549, CurrencyCode.USD))
            SubzeroMoneyText(Money(2000, CurrencyCode.USD), size = MoneyTextSize.Small, compact = true)
            SubzeroMoneyText(Money(123456789012L, CurrencyCode("JPY")), size = MoneyTextSize.Hero)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
private fun SubzeroMoneyTextAnimatedPreview() {
    PreviewTheme {
        var minor by remember { mutableLongStateOf(7230) }
        Column(modifier = Modifier.padding(16.dp)) {
            SubzeroMoneyText(Money(minor, CurrencyCode.USD), size = MoneyTextSize.Hero)
            SubzeroButton(text = "Change", onClick = { minor = if (minor == 7230L) 8748 else 7230 }, compact = true)
        }
    }
}
