package com.subzero.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.ServiceIconSize
import com.subzero.core.designsystem.component.StaggeredReveal
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroIconContainer
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroServiceIcon
import com.subzero.core.designsystem.component.SubzeroSubscriptionRow
import com.subzero.core.designsystem.component.SubzeroTag
import com.subzero.core.designsystem.component.SubzeroWordmark
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money

/*
 * Illustrations are built from the real components the user will meet in the app, with
 * example values. They are examples, not the user's data; nothing here is persisted.
 */

private val exampleRows = listOf(
    Triple("Netflix", 1549L, "Sep 16"),
    Triple("ChatGPT", 2000L, "Sep 20"),
    Triple("Spotify", 1199L, "Sep 24"),
    Triple("Google One", 299L, "Sep 22"),
)

@Composable
internal fun WelcomeIllustration(visible: Boolean) {
    StaggeredReveal(index = 0, visible = visible) {
        SubzeroWordmark(markSize = 96.dp)
    }
}

@Composable
internal fun SeeEverythingIllustration(visible: Boolean) {
    val perMonth = stringResource(R.string.feature_onboarding_example_per_month)
    Column(verticalArrangement = Arrangement.spacedBy(SubzeroTheme.spacing.xs)) {
        exampleRows.forEachIndexed { index, (name, minor, next) ->
            StaggeredReveal(index = index, visible = visible) {
                SubzeroCard(contentPadding = SubzeroTheme.spacing.xxs) {
                    SubzeroSubscriptionRow(
                        name = name,
                        price = Money(minor, CurrencyCode.USD),
                        cadence = perMonth,
                        nextCharge = stringResource(R.string.feature_onboarding_example_next_prefix, next),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PredictIllustration(visible: Boolean) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    StaggeredReveal(index = 0, visible = visible) {
        SubzeroCard(style = SubzeroCardStyle.Hero, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.feature_onboarding_example_next_charge),
                style = SubzeroTheme.typography.label,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubzeroServiceIcon(name = "Netflix", size = ServiceIconSize.Header)
                Spacer(Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Netflix", style = SubzeroTheme.typography.title, color = colors.textPrimary)
                    Text(
                        text = stringResource(R.string.feature_onboarding_example_tomorrow),
                        style = SubzeroTheme.typography.bodySmall,
                        color = colors.accentBright,
                    )
                }
                SubzeroMoneyText(money = Money(1549, CurrencyCode.USD), size = MoneyTextSize.Large, animate = false)
            }
        }
    }
    Spacer(Modifier.height(spacing.sm))
    StaggeredReveal(index = 1, visible = visible) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            listOf("15", "16", "17", "18", "19").forEachIndexed { i, day ->
                val highlighted = i == 1
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SubzeroCard(
                        style = if (highlighted) SubzeroCardStyle.Glass else SubzeroCardStyle.Standard,
                        contentPadding = spacing.sm,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = day,
                            style = SubzeroTheme.typography.money,
                            color = if (highlighted) colors.accent else colors.textSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SavingsIllustration(visible: Boolean) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val perMonth = stringResource(R.string.feature_onboarding_example_per_month)
    StaggeredReveal(index = 0, visible = visible) {
        SubzeroCard(style = SubzeroCardStyle.Hero, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubzeroIconContainer(icon = SubzeroIcons.Savings, tone = SubzeroTone.Positive, size = ServiceIconSize.Header)
                Spacer(Modifier.width(spacing.sm))
                Column {
                    Text(
                        text = stringResource(R.string.feature_onboarding_example_could_save),
                        style = SubzeroTheme.typography.label,
                        color = colors.textSecondary,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        SubzeroMoneyText(money = Money(2300, CurrencyCode.USD), size = MoneyTextSize.Large, animate = false)
                        Spacer(Modifier.width(spacing.xxs))
                        Text(
                            text = perMonth,
                            style = SubzeroTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(spacing.sm))
    StaggeredReveal(index = 1, visible = visible) {
        SubzeroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.feature_onboarding_example_low_usage),
                style = SubzeroTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                SubzeroServiceIcon(name = "Spotify")
                SubzeroServiceIcon(name = "Canva")
                Spacer(Modifier.weight(1f))
                SubzeroTag(text = stringResource(R.string.feature_onboarding_example_rarely_used), tone = SubzeroTone.Warning)
            }
        }
    }
}

@Composable
internal fun StartIllustration(visible: Boolean) {
    val spacing = SubzeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        exampleRows.take(2).forEachIndexed { index, (name, minor, _) ->
            StaggeredReveal(index = index, visible = visible) {
                SubzeroCard(style = SubzeroCardStyle.Glass, contentPadding = spacing.sm) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubzeroServiceIcon(name = name)
                        Spacer(Modifier.width(spacing.sm))
                        Text(text = name, style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                        SubzeroMoneyText(money = Money(minor, CurrencyCode.USD), animate = false)
                    }
                }
            }
        }
    }
}
