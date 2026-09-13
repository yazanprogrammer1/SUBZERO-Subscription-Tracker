package com.subzero.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.insight.Insight
import java.time.LocalDate

/** Localized, formatted wording for an [Insight], plus its icon and tone. */
@Immutable
data class InsightPresentation(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tone: SubzeroTone,
)

/**
 * The single place insights become words (design brief §19: observations, never orders).
 * Both Home and the Insights screen use it so the phrasing stays identical.
 */
@Composable
fun Insight.presentation(today: LocalDate): InsightPresentation {
    val money = SubzeroTheme.moneyFormatter
    return when (this) {
        is Insight.YearlyTotal -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_yearly_title, money.formatCompact(yearly)),
            description = pluralStringResource(R.plurals.core_designsystem_insight_yearly_body, activeCount, money.format(monthly), activeCount),
            icon = SubzeroIcons.TrendUp,
            tone = SubzeroTone.Accent,
        )
        is Insight.LargestSubscription -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_largest_title, subscription.name),
            description = stringResource(R.string.core_designsystem_insight_largest_body, money.format(yearlyEquivalent), shareOfTotalPercent),
            icon = SubzeroIcons.Insights,
            tone = SubzeroTone.Accent,
        )
        is Insight.CategoryConcentration -> InsightPresentation(
            title = pluralStringResource(R.plurals.core_designsystem_insight_category_title, subscriptions.size, subscriptions.size, category.label()),
            description = if (rarelyUsedCount > 0) {
                pluralStringResource(R.plurals.core_designsystem_insight_category_body_rarely, rarelyUsedCount, money.format(monthlyTotal), rarelyUsedCount)
            } else {
                stringResource(R.string.core_designsystem_insight_category_body, money.format(monthlyTotal))
            },
            icon = SubzeroIcons.Subscriptions,
            tone = if (rarelyUsedCount > 0) SubzeroTone.Warning else SubzeroTone.Accent,
        )
        is Insight.LowUsage -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_low_usage_title, subscription.name),
            description = stringResource(R.string.core_designsystem_insight_low_usage_body, money.format(yearlyEquivalent)),
            icon = SubzeroIcons.Pause,
            tone = SubzeroTone.Warning,
        )
        is Insight.PotentialSavings -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_savings_title, money.formatCompact(monthly)),
            description = pluralStringResource(R.plurals.core_designsystem_insight_savings_body, subscriptions.size, subscriptions.size, money.format(yearly)),
            icon = SubzeroIcons.Savings,
            tone = SubzeroTone.Positive,
        )
        is Insight.PriceIncrease -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_price_title, subscription.name),
            description = stringResource(R.string.core_designsystem_insight_price_body, money.format(from), money.format(to), changePercent.toPlainString()),
            icon = SubzeroIcons.Warning,
            tone = SubzeroTone.Warning,
        )
        is Insight.UpcomingLargeCharge -> InsightPresentation(
            title = stringResource(R.string.core_designsystem_insight_large_title, money.format(payment.amount)),
            description = stringResource(R.string.core_designsystem_insight_large_body, payment.subscription.name, relativeDate(payment.date, today).lowercaseFirst()),
            icon = SubzeroIcons.Calendar,
            tone = SubzeroTone.Accent,
        )
    }
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase() }
