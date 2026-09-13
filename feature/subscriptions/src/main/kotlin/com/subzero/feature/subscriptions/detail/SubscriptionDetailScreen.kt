package com.subzero.feature.subscriptions.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.ChartPoint
import com.subzero.core.designsystem.component.LocalNavEntryAnimatedScope
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.ServiceIconSize
import com.subzero.core.designsystem.component.SharedElementKeys
import com.subzero.core.designsystem.component.StaggeredReveal
import com.subzero.core.designsystem.component.SubzeroBarChart
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroDialog
import com.subzero.core.designsystem.component.SubzeroErrorState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroServiceIcon
import com.subzero.core.designsystem.component.SubzeroTag
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.component.sharedBoundsIfAvailable
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.label
import com.subzero.core.designsystem.format.monthAbbreviation
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.format.shortDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.usecase.SpendingHistory
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.feature.subscriptions.R
import java.time.LocalDate

internal object DetailTestTags {
    const val EDIT = "detail_edit"
    const val PAUSE = "detail_pause"
    const val RESUME = "detail_resume"
    const val CANCEL = "detail_cancel"
    const val DELETE = "detail_delete"
    const val CONFIRM_DELETE = "detail_confirm_delete"
    fun usage(usage: DeclaredUsage) = "detail_usage_${usage.name}"
}

private val ContentMaxWidth = 640.dp

@Composable
fun SubscriptionDetailRoute(
    key: SubscriptionDetailKey,
    viewModel: SubscriptionDetailViewModel = hiltViewModel<SubscriptionDetailViewModel, SubscriptionDetailViewModel.Factory>(
        creationCallback = { it.create(key) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    SubscriptionDetailScreen(
        state = state,
        onBack = navigator::back,
        onEdit = { navigator.navigate(SubscriptionFormKey(key.subscriptionId)) },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onMarkCanceled = viewModel::markCanceled,
        onDelete = viewModel::delete,
        onUsage = viewModel::setUsage,
        onDismissError = viewModel::dismissActionError,
        onGone = navigator::back,
    )
}

@Composable
fun SubscriptionDetailScreen(
    state: SubscriptionDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onMarkCanceled: () -> Unit,
    onDelete: () -> Unit,
    onUsage: (DeclaredUsage) -> Unit,
    onDismissError: () -> Unit,
    onGone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    LaunchedEffect(state) {
        if (state is SubscriptionDetailUiState.Gone) onGone()
    }
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
            when (state) {
                is SubscriptionDetailUiState.Loaded -> LoadedContent(
                    state = state,
                    onBack = onBack,
                    onEdit = onEdit,
                    onPause = onPause,
                    onResume = onResume,
                    onMarkCanceled = onMarkCanceled,
                    onDelete = onDelete,
                    onUsage = onUsage,
                    onDismissError = onDismissError,
                )
                else -> {
                    SubzeroTopBar(title = "", onBack = onBack)
                    SubzeroHeroSkeleton(modifier = Modifier.padding(SubzeroTheme.spacing.screen))
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: SubscriptionDetailUiState.Loaded,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onMarkCanceled: () -> Unit,
    onDelete: () -> Unit,
    onUsage: (DeclaredUsage) -> Unit,
    onDismissError: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val subscription = state.subscription
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    SubzeroTopBar(
        title = subscription.name,
        subtitle = subscription.category.label(),
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit, modifier = Modifier.testTag(DetailTestTags.EDIT)) {
                Icon(SubzeroIcons.Edit, contentDescription = stringResource(R.string.feature_subscriptions_detail_edit), tint = colors.textPrimary)
            }
        },
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        HeaderCard(state)

        AnimatedVisibility(visible = state.actionFailed) {
            SubzeroErrorState(description = stringResource(R.string.feature_subscriptions_detail_action_failed), onRetry = onDismissError)
        }

        StaggeredReveal(index = 1) { HistoryCard(state.history, state.today) }
        StaggeredReveal(index = 2) { PriceHistoryCard(state.priceHistory, state.today) }
        StaggeredReveal(index = 3) { UsageCard(subscription.usage, onUsage, enabled = !state.isBusy) }
        if (!subscription.notes.isNullOrBlank()) {
            StaggeredReveal(index = 4) { NotesCard(subscription.notes.orEmpty()) }
        }
        StaggeredReveal(index = 5) {
            ActionsSection(
                status = subscription.status,
                busy = state.isBusy,
                onEdit = onEdit,
                onPause = onPause,
                onResume = onResume,
                onMarkCanceled = { confirmCancel = true },
                onDelete = { confirmDelete = true },
            )
        }
        Spacer(Modifier.height(spacing.xl))
    }

    if (confirmDelete) {
        SubzeroDialog(
            title = stringResource(R.string.feature_subscriptions_detail_delete_title, subscription.name),
            message = stringResource(R.string.feature_subscriptions_detail_delete_message),
            confirmLabel = stringResource(R.string.feature_subscriptions_detail_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
            modifier = Modifier.testTag(DetailTestTags.CONFIRM_DELETE),
        )
    }
    if (confirmCancel) {
        SubzeroDialog(
            title = stringResource(R.string.feature_subscriptions_detail_cancel_title, subscription.name),
            message = stringResource(R.string.feature_subscriptions_detail_cancel_message),
            confirmLabel = stringResource(R.string.feature_subscriptions_detail_mark_canceled),
            onConfirm = { confirmCancel = false; onMarkCanceled() },
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
private fun HeaderCard(state: SubscriptionDetailUiState.Loaded) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val subscription = state.subscription
    val formatter = SubzeroTheme.moneyFormatter
    val animatedScope = LocalNavEntryAnimatedScope.current
    SubzeroCard(
        style = SubzeroCardStyle.Hero,
        modifier = Modifier
            .fillMaxWidth()
            .sharedBoundsIfAvailable(SharedElementKeys.subscriptionCard(subscription.id.value), animatedScope),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubzeroServiceIcon(name = subscription.name, size = ServiceIconSize.Header)
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    SubzeroMoneyText(money = subscription.price, size = MoneyTextSize.Large)
                    Spacer(Modifier.width(spacing.xxs))
                    Text(
                        text = subscription.billingCycle.cadence(),
                        style = SubzeroTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.feature_subscriptions_detail_equivalent,
                        formatter.format(state.monthlyEquivalent),
                        formatter.format(state.yearlyEquivalent),
                    ),
                    style = SubzeroTheme.typography.caption,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(spacing.md))
        when (subscription.status) {
            SubscriptionStatus.ACTIVE -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feature_subscriptions_detail_next_payment),
                        style = SubzeroTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    Text(
                        text = relativeDate(subscription.nextBillingDate, state.today),
                        style = SubzeroTheme.typography.body,
                        color = colors.accentBright,
                    )
                }
                Text(
                    text = shortDate(subscription.nextBillingDate, state.today),
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            SubscriptionStatus.PAUSED -> StatusLine(subscription.status.label(), stringResource(R.string.feature_subscriptions_detail_status_paused_help), SubzeroTone.Warning)
            SubscriptionStatus.CANCELED -> StatusLine(subscription.status.label(), stringResource(R.string.feature_subscriptions_detail_status_canceled_help), SubzeroTone.Neutral)
        }
    }
}

@Composable
private fun StatusLine(label: String, help: String, tone: SubzeroTone) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubzeroTag(text = label, tone = tone)
        Spacer(Modifier.width(SubzeroTheme.spacing.xs))
        Text(text = help, style = SubzeroTheme.typography.caption, color = SubzeroTheme.colors.textSecondary)
    }
}

@Composable
private fun HistoryCard(history: SpendingHistory, today: LocalDate) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    val currentMonth = java.time.YearMonth.from(today)
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.feature_subscriptions_detail_history_title))
        Text(
            text = stringResource(R.string.feature_subscriptions_detail_history_subtitle),
            style = SubzeroTheme.typography.caption,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(spacing.md))
        if (history.total.isZero) {
            Text(
                text = stringResource(R.string.feature_subscriptions_detail_history_empty),
                style = SubzeroTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            SubzeroBarChart(
                points = history.months.map { month ->
                    ChartPoint(
                        label = monthAbbreviation(month.month).take(1),
                        value = month.total.amountMinor.toFloat(),
                        highlighted = month.month == currentMonth,
                    )
                },
                contentDescription = stringResource(R.string.feature_subscriptions_detail_history_chart, formatter.format(history.total)),
                height = 120.dp,
            )
        }
        Spacer(Modifier.height(spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.feature_subscriptions_detail_history_total),
                style = SubzeroTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            SubzeroMoneyText(money = history.total, size = MoneyTextSize.Large)
        }
        if (history.hasEstimates) {
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.feature_subscriptions_detail_history_estimated),
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun PriceHistoryCard(priceHistory: List<PriceChange>, today: LocalDate) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.feature_subscriptions_detail_price_title))
        Spacer(Modifier.height(spacing.xs))
        if (priceHistory.size <= 1) {
            Text(
                text = stringResource(R.string.feature_subscriptions_detail_price_no_changes),
                style = SubzeroTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            priceHistory.asReversed().forEachIndexed { index, change ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (index == 0) stringResource(R.string.feature_subscriptions_detail_price_current) else stringResource(R.string.feature_subscriptions_detail_price_since, shortDate(change.effectiveFrom, today)),
                            style = SubzeroTheme.typography.bodySmall,
                            color = if (index == 0) colors.textPrimary else colors.textSecondary,
                        )
                        if (index == 0) {
                            Text(
                                text = stringResource(R.string.feature_subscriptions_detail_price_since, shortDate(change.effectiveFrom, today)),
                                style = SubzeroTheme.typography.caption,
                                color = colors.textTertiary,
                            )
                        }
                    }
                    SubzeroMoneyText(money = change.price, animate = false, color = if (index == 0) colors.textPrimary else colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun UsageCard(usage: DeclaredUsage, onUsage: (DeclaredUsage) -> Unit, enabled: Boolean) {
    val spacing = SubzeroTheme.spacing
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.feature_subscriptions_detail_usage_title))
        Text(
            text = stringResource(R.string.feature_subscriptions_detail_usage_help),
            style = SubzeroTheme.typography.caption,
            color = SubzeroTheme.colors.textTertiary,
        )
        Spacer(Modifier.height(spacing.sm))
        DeclaredUsage.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.padding(bottom = spacing.xs)) {
                row.forEach { option ->
                    SubzeroChip(
                        text = option.label(),
                        selected = option == usage,
                        onClick = { onUsage(option) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(DetailTestTags.usage(option)),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.feature_subscriptions_detail_notes_title))
        Text(text = notes, style = SubzeroTheme.typography.bodySmall, color = SubzeroTheme.colors.textSecondary)
    }
}

@Composable
private fun ActionsSection(
    status: SubscriptionStatus,
    busy: Boolean,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onMarkCanceled: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SubzeroButton(
                text = stringResource(R.string.feature_subscriptions_detail_edit),
                onClick = onEdit,
                style = SubzeroButtonStyle.Secondary,
                leadingIcon = SubzeroIcons.Edit,
                modifier = Modifier.weight(1f),
            )
            when (status) {
                SubscriptionStatus.ACTIVE -> SubzeroButton(
                    text = stringResource(R.string.feature_subscriptions_detail_pause),
                    onClick = onPause,
                    style = SubzeroButtonStyle.Secondary,
                    leadingIcon = SubzeroIcons.Pause,
                    enabled = !busy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DetailTestTags.PAUSE),
                )
                else -> SubzeroButton(
                    text = stringResource(R.string.feature_subscriptions_detail_resume),
                    onClick = onResume,
                    style = SubzeroButtonStyle.Primary,
                    leadingIcon = SubzeroIcons.Resume,
                    enabled = !busy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DetailTestTags.RESUME),
                )
            }
        }
        if (status != SubscriptionStatus.CANCELED) {
            SubzeroButton(
                text = stringResource(R.string.feature_subscriptions_detail_mark_canceled),
                onClick = onMarkCanceled,
                style = SubzeroButtonStyle.Ghost,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.CANCEL),
            )
        }
        SubzeroButton(
            text = stringResource(R.string.feature_subscriptions_detail_delete),
            onClick = onDelete,
            style = SubzeroButtonStyle.Danger,
            leadingIcon = SubzeroIcons.Delete,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DetailTestTags.DELETE),
        )
    }
}
