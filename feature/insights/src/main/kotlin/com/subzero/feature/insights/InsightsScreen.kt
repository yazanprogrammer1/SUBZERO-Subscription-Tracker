package com.subzero.feature.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.StaggeredReveal
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroEmptyState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroInsightCard
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroSubscriptionRow
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.label
import com.subzero.core.designsystem.format.presentation
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.insight.Insight
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.navigation.SubscriptionsKey

enum class InsightsTab { Overview, Spending, Usage }

internal object InsightsTestTags {
    const val EMPTY = "insights_empty"
    const val NO_INSIGHTS = "insights_none"
    fun tab(tab: InsightsTab) = "insights_tab_${tab.name}"
    fun insight(id: String) = "insight_$id"
}

private val ContentMaxWidth = 640.dp
private const val TOP_SUBSCRIPTIONS = 5

@Composable
fun InsightsRoute(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    InsightsScreen(
        state = state,
        onOpenSubscription = { navigator.navigate(SubscriptionDetailKey(it.value)) },
        onReviewSubscriptions = { navigator.switchTab(SubscriptionsKey) },
        onAdd = { navigator.navigate(SubscriptionFormKey()) },
    )
}

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val motion = SubzeroTheme.motion
    var tab by rememberSaveable { mutableStateOf(InsightsTab.Overview) }

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
            SubzeroTopBar(title = stringResource(R.string.feature_insights_title))
            when (state) {
                InsightsUiState.Loading -> SubzeroHeroSkeleton(modifier = Modifier.padding(spacing.screen))
                InsightsUiState.NoSubscriptions -> SubzeroEmptyState(
                    title = stringResource(R.string.feature_insights_empty_title),
                    description = stringResource(R.string.feature_insights_empty_body),
                    icon = SubzeroIcons.Insights,
                    actionLabel = stringResource(R.string.feature_insights_add),
                    onAction = onAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(InsightsTestTags.EMPTY),
                )
                is InsightsUiState.Ready -> {
                    TabRow(selected = tab, onSelect = { tab = it })
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(motion.standardSpec()) togetherWith fadeOut(motion.fastSpec()) },
                        label = "insightsTab",
                    ) { current ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = spacing.screen),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            when (current) {
                                InsightsTab.Overview -> OverviewTab(state, onOpenSubscription, onReviewSubscriptions)
                                InsightsTab.Spending -> SpendingTab(state, onOpenSubscription)
                                InsightsTab.Usage -> UsageTab(state, onOpenSubscription)
                            }
                            Spacer(Modifier.height(spacing.xxl))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRow(selected: InsightsTab, onSelect: (InsightsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SubzeroTheme.spacing.screen, vertical = SubzeroTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SubzeroTheme.spacing.xs),
    ) {
        InsightsTab.entries.forEach { tab ->
            SubzeroChip(
                text = stringResource(
                    when (tab) {
                        InsightsTab.Overview -> R.string.feature_insights_tab_overview
                        InsightsTab.Spending -> R.string.feature_insights_tab_spending
                        InsightsTab.Usage -> R.string.feature_insights_tab_usage
                    },
                ),
                selected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(InsightsTestTags.tab(tab)),
            )
        }
    }
}

@Composable
private fun OverviewTab(
    state: InsightsUiState.Ready,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    StaggeredReveal(index = 0) {
        SubzeroCard(style = SubzeroCardStyle.Hero, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.feature_insights_yearly_title), style = SubzeroTheme.typography.label, color = colors.textSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                SubzeroMoneyText(money = state.summary.yearly, size = MoneyTextSize.Hero, compact = true)
                Spacer(Modifier.width(spacing.xs))
                Text(
                    text = stringResource(R.string.feature_insights_yearly_suffix),
                    style = SubzeroTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Text(text = stringResource(R.string.feature_insights_yearly_body), style = SubzeroTheme.typography.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.feature_insights_monthly_equivalent, formatter.format(state.summary.monthly)),
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
    }
    if (state.insights.isEmpty()) {
        StaggeredReveal(index = 1) {
            SubzeroCard(modifier = Modifier.fillMaxWidth().testTag(InsightsTestTags.NO_INSIGHTS)) {
                Text(text = stringResource(R.string.feature_insights_no_insights_title), style = SubzeroTheme.typography.body, color = colors.textPrimary)
                Text(text = stringResource(R.string.feature_insights_no_insights_body), style = SubzeroTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
    state.insights.forEachIndexed { index, insight ->
        StaggeredReveal(index = index + 1) {
            val presentation = insight.presentation(state.today)
            val target = insight.subscriptionTarget()
            SubzeroInsightCard(
                title = presentation.title,
                description = presentation.description,
                icon = presentation.icon,
                tone = presentation.tone,
                onClick = if (target != null) ({ onOpenSubscription(target) }) else onReviewSubscriptions,
                modifier = Modifier.testTag(InsightsTestTags.insight(insight.id)),
            )
        }
    }
}

/** Insights about one subscription open it; the rest go to the list. */
private fun Insight.subscriptionTarget(): SubscriptionId? = when (this) {
    is Insight.LargestSubscription -> subscription.id
    is Insight.LowUsage -> subscription.id
    is Insight.PriceIncrease -> subscription.id
    is Insight.UpcomingLargeCharge -> payment.subscription.id
    is Insight.CategoryConcentration, is Insight.PotentialSavings, is Insight.YearlyTotal -> null
}

@Composable
private fun SpendingTab(state: InsightsUiState.Ready, onOpenSubscription: (SubscriptionId) -> Unit) {
    val spacing = SubzeroTheme.spacing
    StaggeredReveal(index = 0) {
        Column {
            SectionHeader(title = stringResource(R.string.feature_insights_categories))
            SubzeroCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    state.categories.forEach { share -> CategoryBar(share) }
                }
            }
        }
    }
    StaggeredReveal(index = 1) {
        Column {
            SectionHeader(title = stringResource(R.string.feature_insights_top_subscriptions))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                state.ranked.take(TOP_SUBSCRIPTIONS).forEach { item ->
                    SubzeroSubscriptionRow(
                        name = item.subscription.name,
                        price = item.subscription.price,
                        cadence = item.subscription.billingCycle.cadence(),
                        nextCharge = stringResource(R.string.feature_insights_per_year, SubzeroTheme.moneyFormatter.format(item.yearly)),
                        onClick = { onOpenSubscription(item.subscription.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(share: com.subzero.core.domain.usecase.CategoryShare) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val fraction by animateFloatAsState(if (shown) share.sharePercent / 100f else 0f, motion.revealSpec(), label = "categoryBar")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = share.category.label(),
                style = SubzeroTheme.typography.bodySmall,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SubzeroMoneyText(money = share.monthly, size = MoneyTextSize.Small, animate = false)
        }
        Text(
            text = pluralStringResource(R.plurals.feature_insights_category_count, share.count, share.count, share.sharePercent),
            style = SubzeroTheme.typography.caption,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(SubzeroTheme.spacing.xxs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(SubzeroTheme.shapes.full)
                .background(colors.surfaceSubtle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(SubzeroTheme.shapes.full)
                    .background(colors.accent),
            )
        }
    }
}

@Composable
private fun UsageTab(state: InsightsUiState.Ready, onOpenSubscription: (SubscriptionId) -> Unit) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    StaggeredReveal(index = 0) {
        Text(
            text = stringResource(R.string.feature_insights_usage_intro),
            style = SubzeroTheme.typography.caption,
            color = colors.textTertiary,
        )
    }
    if (state.unknownUsageCount > 0) {
        StaggeredReveal(index = 1) {
            SubzeroInsightCard(
                title = stringResource(R.string.feature_insights_usage_group_unknown),
                description = pluralStringResource(R.plurals.feature_insights_usage_unknown, state.unknownUsageCount, state.unknownUsageCount),
                icon = SubzeroIcons.Info,
            )
        }
    }
    state.byUsage.entries.forEachIndexed { index, (usage, items) ->
        StaggeredReveal(index = index + 2) {
            Column {
                SectionHeader(title = usage.groupTitle())
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    items.forEach { item ->
                        SubzeroSubscriptionRow(
                            name = item.subscription.name,
                            price = item.subscription.price,
                            cadence = item.subscription.billingCycle.cadence(),
                            nextCharge = stringResource(R.string.feature_insights_per_year, SubzeroTheme.moneyFormatter.format(item.yearly)),
                            onClick = { onOpenSubscription(item.subscription.id) },
                            statusBadge = if (usage == DeclaredUsage.RARELY) usage.label() else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeclaredUsage.groupTitle(): String = stringResource(
    when (this) {
        DeclaredUsage.RARELY -> R.string.feature_insights_usage_group_rarely
        DeclaredUsage.MONTHLY -> R.string.feature_insights_usage_group_monthly
        DeclaredUsage.WEEKLY -> R.string.feature_insights_usage_group_weekly
        DeclaredUsage.DAILY -> R.string.feature_insights_usage_group_daily
        DeclaredUsage.UNKNOWN -> R.string.feature_insights_usage_group_unknown
    },
)
