package com.subzero.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.ChartPoint
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.ServiceIconSize
import com.subzero.core.designsystem.component.StaggeredReveal
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroEmptyState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroIconContainer
import com.subzero.core.designsystem.component.SubzeroInsightCard
import com.subzero.core.designsystem.component.SubzeroLineChart
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroServiceIcon
import com.subzero.core.designsystem.component.SubzeroSubscriptionRowSkeleton
import com.subzero.core.designsystem.component.SubzeroTag
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.monthAbbreviation
import com.subzero.core.designsystem.format.presentation
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.navigation.SubscriptionsKey
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalTime

internal object HomeTestTags {
    const val HERO = "home_hero"
    const val NEXT_CHARGE = "home_next_charge"
    const val TREND = "home_trend"
    const val SAVINGS = "home_savings"
    const val INSIGHT = "home_insight"
    const val EMPTY = "home_empty"
}

private val ContentMaxWidth = 640.dp

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    HomeScreen(
        state = state,
        onAdd = { navigator.navigate(SubscriptionFormKey()) },
        onOpenSubscription = { navigator.navigate(SubscriptionDetailKey(it.value)) },
        onReviewSubscriptions = { navigator.switchTab(SubscriptionsKey) },
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: () -> Unit,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = ContentMaxWidth)
                .align(Alignment.TopCenter),
        ) {
            SubzeroTopBar(
                title = greeting(name = (state as? HomeUiState.Dashboard)?.displayName ?: (state as? HomeUiState.Empty)?.displayName, clock = clock),
            )
            when (state) {
                HomeUiState.Loading -> Column(modifier = Modifier.padding(horizontal = spacing.screen)) {
                    SubzeroHeroSkeleton()
                    Spacer(Modifier.height(spacing.sm))
                    SubzeroSubscriptionRowSkeleton()
                }
                is HomeUiState.Empty -> SubzeroEmptyState(
                    title = stringResource(R.string.feature_home_empty_title),
                    description = stringResource(R.string.feature_home_empty_body),
                    actionLabel = stringResource(R.string.feature_home_add),
                    onAction = onAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(HomeTestTags.EMPTY),
                )
                is HomeUiState.Dashboard -> Dashboard(
                    state = state,
                    onOpenSubscription = onOpenSubscription,
                    onReviewSubscriptions = onReviewSubscriptions,
                )
            }
        }
    }
}

@Composable
private fun Dashboard(
    state: HomeUiState.Dashboard,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        StaggeredReveal(index = 0) { HeroCard(state) }
        StaggeredReveal(index = 1) { UpcomingSection(state, onOpenSubscription, onReviewSubscriptions) }
        StaggeredReveal(index = 2) { TrendCard(state) }
        StaggeredReveal(index = 3) { SavingsCard(state, onReviewSubscriptions) }
        val insight = state.insight
        if (insight != null) {
            StaggeredReveal(index = 4) {
                Column {
                    SectionHeader(title = stringResource(R.string.feature_home_insight))
                    val presentation = insight.presentation(state.today)
                    SubzeroInsightCard(
                        title = presentation.title,
                        description = presentation.description,
                        icon = presentation.icon,
                        tone = presentation.tone,
                        actionLabel = stringResource(R.string.feature_home_review),
                        onAction = onReviewSubscriptions,
                        modifier = Modifier.testTag(HomeTestTags.INSIGHT),
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.xxl))
    }
}

@Composable
private fun HeroCard(state: HomeUiState.Dashboard) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    SubzeroCard(
        style = SubzeroCardStyle.Hero,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.HERO),
    ) {
        Text(
            text = stringResource(R.string.feature_home_monthly_spending),
            style = SubzeroTheme.typography.label,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.xs))
        SubzeroMoneyText(money = state.summary.monthly, size = MoneyTextSize.Hero)
        Text(
            text = stringResource(R.string.feature_home_per_month),
            style = SubzeroTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.feature_home_per_year, formatter.format(state.summary.yearly)),
                style = SubzeroTheme.typography.money,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.feature_home_active_count, state.summary.includedCount, state.summary.includedCount),
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
        if (state.summary.excludedForeignCurrency > 0) {
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = pluralStringResource(R.plurals.feature_home_foreign_excluded, state.summary.excludedForeignCurrency, state.summary.excludedForeignCurrency),
                style = SubzeroTheme.typography.caption,
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun UpcomingSection(
    state: HomeUiState.Dashboard,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Column {
        SectionHeader(
            title = stringResource(R.string.feature_home_upcoming),
            actionLabel = stringResource(R.string.feature_home_see_all),
            onAction = onReviewSubscriptions,
        )
        val next = state.nextPayment
        if (next == null) {
            SubzeroCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.feature_home_no_upcoming),
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        } else {
            SubzeroCard(
                style = SubzeroCardStyle.Glass,
                onClick = { onOpenSubscription(next.subscription.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeTestTags.NEXT_CHARGE),
            ) {
                Text(
                    text = stringResource(R.string.feature_home_next_charge),
                    style = SubzeroTheme.typography.caption,
                    color = colors.textTertiary,
                )
                Spacer(Modifier.height(spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubzeroServiceIcon(name = next.subscription.name, size = ServiceIconSize.Row)
                    Spacer(Modifier.width(spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = next.subscription.name,
                            style = SubzeroTheme.typography.body,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = relativeDate(next.date, state.today),
                            style = SubzeroTheme.typography.bodySmall,
                            color = colors.accentBright,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SubzeroMoneyText(money = next.amount, animate = false)
                        Text(text = next.subscription.billingCycle.cadence(), style = SubzeroTheme.typography.caption, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCard(state: HomeUiState.Dashboard) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    val trend = state.trend
    Column {
        SectionHeader(title = stringResource(R.string.feature_home_spending_overview))
        SubzeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeTestTags.TREND),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feature_home_this_month),
                        style = SubzeroTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    SubzeroMoneyText(money = trend.current.total, size = MoneyTextSize.Large)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.feature_home_vs_last_month),
                        style = SubzeroTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    ChangeTag(trend.changeVersusPreviousPercent)
                }
            }
            if (!trend.current.projected.isZero) {
                Spacer(Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.feature_home_projected_note, formatter.format(trend.current.projected)),
                    style = SubzeroTheme.typography.caption,
                    color = colors.textTertiary,
                )
            }
            Spacer(Modifier.height(spacing.md))
            SubzeroLineChart(
                points = trend.months.map { month ->
                    ChartPoint(
                        label = monthAbbreviation(month.month),
                        value = month.total.amountMinor.toFloat(),
                        highlighted = month == trend.current,
                    )
                },
                contentDescription = pluralStringResource(R.plurals.feature_home_trend_chart, trend.months.size, trend.months.size, formatter.format(trend.current.total)),
                height = 120.dp,
            )
        }
    }
}

@Composable
private fun ChangeTag(change: BigDecimal?) {
    when {
        change == null -> SubzeroTag(text = stringResource(R.string.feature_home_change_new), tone = SubzeroTone.Neutral)
        change.signum() > 0 -> SubzeroTag(text = stringResource(R.string.feature_home_change_up, change.toPlainString()), tone = SubzeroTone.Warning)
        change.signum() < 0 -> SubzeroTag(text = stringResource(R.string.feature_home_change_down, change.abs().toPlainString()), tone = SubzeroTone.Positive)
        else -> SubzeroTag(text = stringResource(R.string.feature_home_change_none), tone = SubzeroTone.Neutral)
    }
}

@Composable
private fun SavingsCard(state: HomeUiState.Dashboard, onReview: () -> Unit) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val savings = state.savings
    Column {
        SectionHeader(title = stringResource(R.string.feature_home_savings_title))
        SubzeroCard(
            onClick = onReview,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeTestTags.SAVINGS),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubzeroIconContainer(icon = SubzeroIcons.Savings, tone = if (savings.isEmpty) SubzeroTone.Neutral else SubzeroTone.Positive, size = ServiceIconSize.Header)
                Spacer(Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    if (savings.isEmpty) {
                        Text(text = stringResource(R.string.feature_home_savings_none_title), style = SubzeroTheme.typography.body, color = colors.textPrimary)
                        Text(text = stringResource(R.string.feature_home_savings_none_body), style = SubzeroTheme.typography.caption, color = colors.textSecondary)
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            SubzeroMoneyText(money = savings.monthly, size = MoneyTextSize.Large, color = colors.positive)
                            Spacer(Modifier.width(spacing.xxs))
                            Text(
                                text = stringResource(R.string.feature_home_per_month),
                                style = SubzeroTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.feature_home_savings_detail, savings.subscriptions.size, savings.subscriptions.size),
                            style = SubzeroTheme.typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun greeting(name: String?, clock: Clock): String {
    val hour = LocalTime.now(clock).hour
    val base = stringResource(
        when {
            hour < MORNING_END -> R.string.feature_home_greeting_morning
            hour < AFTERNOON_END -> R.string.feature_home_greeting_afternoon
            else -> R.string.feature_home_greeting_evening
        },
    )
    return if (name.isNullOrBlank()) base else stringResource(R.string.feature_home_greeting_named, base, name)
}

private const val MORNING_END = 12
private const val AFTERNOON_END = 18
