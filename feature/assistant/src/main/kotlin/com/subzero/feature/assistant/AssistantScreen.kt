package com.subzero.feature.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.ServiceIconSize
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroIconContainer
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroServiceIcon
import com.subzero.core.designsystem.component.SubzeroSkeleton
import com.subzero.core.designsystem.component.SubzeroTag
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.label
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.format.shortDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.designsystem.format.label
import com.subzero.core.domain.assistant.AnswerItem
import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.AssistantSuggestion
import com.subzero.core.domain.assistant.SpendPeriod
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionsKey
import java.time.LocalDate

internal object AssistantTestTags {
    const val INPUT = "assistant_input"
    const val SEND = "assistant_send"
    const val THINKING = "assistant_thinking"
    fun suggestion(index: Int) = "assistant_suggestion_$index"
    fun followUp(index: Int) = "assistant_followup_$index"
    fun answer(id: Long) = "assistant_answer_$id"
}

private val ContentMaxWidth = 640.dp

/** The starter questions; every one maps to an intent the local assistant understands. */
private val starterSuggestions = listOf(
    AssistantSuggestion.AI_CATEGORY,
    AssistantSuggestion.TOP,
    AssistantSuggestion.CANCEL,
    AssistantSuggestion.NEXT,
    AssistantSuggestion.TOTAL,
    AssistantSuggestion.INCREASES,
)

/**
 * The question each follow-up chip asks. The chip's own text is sent verbatim, so a chip tapped
 * in Arabic asks an Arabic question and the assistant answers in Arabic.
 */
private fun AssistantSuggestion.questionRes(): Int = when (this) {
    AssistantSuggestion.TOTAL -> R.string.feature_assistant_suggestion_total
    AssistantSuggestion.YEARLY -> R.string.feature_assistant_suggestion_yearly
    AssistantSuggestion.TOP -> R.string.feature_assistant_suggestion_most
    AssistantSuggestion.CHEAPEST -> R.string.feature_assistant_suggestion_cheapest
    AssistantSuggestion.CANCEL -> R.string.feature_assistant_suggestion_cancel
    AssistantSuggestion.NEXT -> R.string.feature_assistant_suggestion_next
    AssistantSuggestion.UPCOMING_WEEK -> R.string.feature_assistant_suggestion_week
    AssistantSuggestion.INCREASES -> R.string.feature_assistant_suggestion_increase
    AssistantSuggestion.BREAKDOWN -> R.string.feature_assistant_suggestion_breakdown
    AssistantSuggestion.RECORDED -> R.string.feature_assistant_suggestion_recorded
    AssistantSuggestion.INACTIVE -> R.string.feature_assistant_suggestion_inactive
    AssistantSuggestion.AI_CATEGORY -> R.string.feature_assistant_suggestion_ai
    AssistantSuggestion.HELP -> R.string.feature_assistant_suggestion_help
}

/** Kinds whose wording already introduces the chips, so they need no "try next" heading. */
private val suggestionLedKinds = setOf(
    AnswerKind.UNKNOWN,
    AnswerKind.CAPABILITIES,
    AnswerKind.GREETING,
    AnswerKind.NO_SUBSCRIPTIONS,
)

@Composable
fun AssistantRoute(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    AssistantScreen(
        state = state,
        onInput = viewModel::setInput,
        onSend = viewModel::send,
        onAsk = viewModel::ask,
        onBack = navigator::back,
        onOpenSubscription = { navigator.navigate(SubscriptionDetailKey(it.value)) },
        onReviewSubscriptions = { navigator.switchTab(SubscriptionsKey) },
    )
}

@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onAsk: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isThinking) {
        val last = state.messages.size + (if (state.isThinking) 1 else 0)
        if (last > 0) listState.animateScrollToItem(last)
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
                .align(Alignment.TopCenter)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            SubzeroTopBar(
                title = stringResource(R.string.feature_assistant_title),
                onBack = onBack,
                actions = { SubzeroTag(text = stringResource(R.string.feature_assistant_beta), tone = SubzeroTone.Accent) },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                item(key = "intro") { Intro(onAsk = onAsk, showSuggestions = state.messages.isEmpty()) }
                items(state.messages, key = { it.id }) { message ->
                    when (message) {
                        is AssistantMessage.Question -> QuestionBubble(message.text)
                        is AssistantMessage.Answer -> AnswerCard(
                            answer = message.answer,
                            today = message.today,
                            onAsk = onAsk,
                            onOpenSubscription = onOpenSubscription,
                            onReviewSubscriptions = onReviewSubscriptions,
                            modifier = Modifier.testTag(AssistantTestTags.answer(message.id)),
                        )
                    }
                }
                if (state.isThinking) {
                    item(key = "thinking") { ThinkingBubble() }
                }
            }
            InputBar(state = state, onInput = onInput, onSend = onSend)
        }
    }
}

@Composable
private fun Intro(onAsk: (String) -> Unit, showSuggestions: Boolean) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubzeroIconContainer(icon = SubzeroIcons.Sparkle, tone = SubzeroTone.Accent, size = ServiceIconSize.Header)
            Spacer(Modifier.width(spacing.sm))
            Column {
                Text(text = stringResource(R.string.feature_assistant_welcome), style = SubzeroTheme.typography.title, color = colors.textPrimary)
                Text(text = stringResource(R.string.feature_assistant_offline_note), style = SubzeroTheme.typography.caption, color = colors.textTertiary)
            }
        }
        AnimatedVisibility(visible = showSuggestions) {
            Suggestions(suggestions = starterSuggestions, onAsk = onAsk, modifier = Modifier.padding(top = spacing.md))
        }
    }
}

@Composable
private fun Suggestions(
    suggestions: List<AssistantSuggestion>,
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: (Int) -> String = AssistantTestTags::suggestion,
) {
    val spacing = SubzeroTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        suggestions.forEachIndexed { index, suggestion ->
            val text = stringResource(suggestion.questionRes())
            SubzeroChip(
                text = text,
                selected = false,
                onClick = { onAsk(text) },
                icon = SubzeroIcons.Sparkle,
                modifier = Modifier.testTag(tag(index)),
            )
        }
    }
}

@Composable
private fun QuestionBubble(text: String) {
    val colors = SubzeroTheme.colors
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = SubzeroTheme.typography.body,
            color = colors.onAccent,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(SubzeroTheme.shapes.md)
                .background(colors.accent)
                .padding(horizontal = SubzeroTheme.spacing.md, vertical = SubzeroTheme.spacing.sm),
        )
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.testTag(AssistantTestTags.THINKING), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { SubzeroSkeleton(modifier = Modifier.width(10.dp).height(10.dp), shape = SubzeroTheme.shapes.full) }
    }
}

@Composable
private fun AnswerCard(
    answer: AssistantAnswer,
    today: LocalDate,
    onAsk: (String) -> Unit,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onReviewSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    SubzeroCard(style = SubzeroCardStyle.Glass, modifier = modifier.fillMaxWidth()) {
        when (answer.kind) {
            AnswerKind.TOTAL_SPEND -> {
                Lead(stringResource(R.string.feature_assistant_answer_total_lead))
                HeroAmount(answer)
                Body(pluralStringResource(R.plurals.feature_assistant_answer_total_body, answer.count, answer.count))
            }
            AnswerKind.SPEND_IN_CATEGORY -> {
                Lead(stringResource(R.string.feature_assistant_answer_total_lead))
                HeroAmount(answer)
                Body(pluralStringResource(R.plurals.feature_assistant_answer_category_body, answer.count, answer.count, answer.category?.label().orEmpty()))
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.NOTHING_IN_CATEGORY -> Body(stringResource(R.string.feature_assistant_answer_nothing_in_category, answer.category?.label().orEmpty()))
            AnswerKind.YEARLY_SPEND -> {
                Lead(stringResource(R.string.feature_assistant_answer_total_lead))
                HeroAmount(answer)
                Body(pluralStringResource(R.plurals.feature_assistant_answer_yearly_body, answer.count, answer.count))
                answer.secondaryAmount?.let { Body(stringResource(R.string.feature_assistant_answer_yearly_monthly, formatter.format(it))) }
            }
            AnswerKind.CATEGORY_BREAKDOWN -> {
                Body(stringResource(R.string.feature_assistant_answer_breakdown))
                ItemList(answer.items, today, onOpenSubscription)
                Spacer(Modifier.height(spacing.xs))
                Body(stringResource(R.string.feature_assistant_answer_breakdown_total, formatter.format(checkNotNull(answer.amount))))
            }
            AnswerKind.TOP_SUBSCRIPTIONS -> {
                Body(stringResource(R.string.feature_assistant_answer_top))
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.CHEAPEST_SUBSCRIPTIONS -> {
                Body(stringResource(R.string.feature_assistant_answer_cheapest))
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.SUBSCRIPTION_DETAIL -> {
                val subscription = answer.items.first().subscription
                Body(
                    stringResource(
                        R.string.feature_assistant_answer_detail_price,
                        subscription.name,
                        formatter.format(subscription.price),
                        subscription.billingCycle.cadence(),
                        formatter.format(checkNotNull(answer.secondaryAmount)),
                    ),
                )
                val next = answer.date
                if (next != null) {
                    Body(stringResource(R.string.feature_assistant_answer_detail_next, relativeDate(next, today).lowercase(), shortDate(next, today)))
                } else {
                    Body(stringResource(R.string.feature_assistant_answer_detail_inactive, subscription.status.label().lowercase()))
                }
                Body(
                    if (subscription.usage == DeclaredUsage.UNKNOWN) {
                        stringResource(R.string.feature_assistant_answer_detail_usage_unknown)
                    } else {
                        stringResource(R.string.feature_assistant_answer_detail_usage, subscription.usage.label().lowercase())
                    },
                )
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.CANCEL_CANDIDATES -> {
                Body(stringResource(R.string.feature_assistant_answer_cancel_lead))
                ItemList(answer.items, today, onOpenSubscription)
                Spacer(Modifier.height(spacing.xs))
                Body(stringResource(R.string.feature_assistant_answer_cancel_body, formatter.format(checkNotNull(answer.amount))))
                Spacer(Modifier.height(spacing.sm))
                SubzeroButton(text = stringResource(R.string.feature_assistant_review), onClick = onReviewSubscriptions, style = SubzeroButtonStyle.Secondary, compact = true)
            }
            AnswerKind.NO_CANCEL_CANDIDATES -> {
                Body(stringResource(R.string.feature_assistant_answer_no_cancel))
                if (answer.count > 0) Body(pluralStringResource(R.plurals.feature_assistant_answer_no_cancel_hint, answer.count, answer.count))
            }
            AnswerKind.NO_USAGE_DATA -> Body(stringResource(R.string.feature_assistant_answer_no_usage))
            AnswerKind.NEXT_CHARGE -> {
                val date = checkNotNull(answer.date)
                Body(stringResource(R.string.feature_assistant_answer_next, relativeDate(date, today).lowercase(), shortDate(date, today)))
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.NO_UPCOMING -> Body(stringResource(R.string.feature_assistant_answer_no_upcoming))
            AnswerKind.UPCOMING_THIS_MONTH -> {
                Body(pluralStringResource(R.plurals.feature_assistant_answer_upcoming, answer.count, answer.count))
                ItemList(answer.items, today, onOpenSubscription)
                Spacer(Modifier.height(spacing.xs))
                Body(stringResource(R.string.feature_assistant_answer_upcoming_total, formatter.format(checkNotNull(answer.amount))))
            }
            AnswerKind.PRICE_INCREASES -> {
                Body(stringResource(R.string.feature_assistant_answer_increases))
                ItemList(answer.items, today, onOpenSubscription)
            }
            AnswerKind.NO_PRICE_INCREASES -> Body(stringResource(R.string.feature_assistant_answer_no_increases))
            AnswerKind.COUNT_BY_CATEGORY -> {
                Body(pluralStringResource(R.plurals.feature_assistant_answer_count, answer.count, answer.count))
                ItemList(answer.items, today, onOpenSubscription, showCategory = true)
            }
            AnswerKind.UPCOMING_IN_DAYS -> {
                Body(pluralStringResource(R.plurals.feature_assistant_answer_upcoming_days, answer.count, answer.count, answer.days))
                ItemList(answer.items, today, onOpenSubscription)
                Spacer(Modifier.height(spacing.xs))
                Body(stringResource(R.string.feature_assistant_answer_upcoming_total, formatter.format(checkNotNull(answer.amount))))
            }
            AnswerKind.RECORDED_SPEND -> {
                Lead(stringResource(answer.period.recordedLeadRes()))
                HeroAmount(answer)
                Body(pluralStringResource(R.plurals.feature_assistant_answer_recorded_count, answer.count, answer.count))
            }
            AnswerKind.NO_RECORDED_SPEND -> Body(stringResource(R.string.feature_assistant_answer_no_recorded))
            AnswerKind.INACTIVE -> {
                Body(pluralStringResource(R.plurals.feature_assistant_answer_inactive, answer.count, answer.count))
                ItemList(answer.items, today, onOpenSubscription, showStatus = true)
            }
            AnswerKind.NO_INACTIVE -> Body(stringResource(R.string.feature_assistant_answer_no_inactive))
            AnswerKind.CAPABILITIES -> Body(stringResource(R.string.feature_assistant_answer_capabilities))
            AnswerKind.GREETING -> Body(stringResource(R.string.feature_assistant_answer_greeting))
            AnswerKind.NO_SUBSCRIPTIONS -> Body(stringResource(R.string.feature_assistant_answer_no_subscriptions))
            AnswerKind.TEXT -> Body(answer.text.orEmpty())
            AnswerKind.UNKNOWN -> Body(stringResource(R.string.feature_assistant_answer_unknown))
        }
        if (answer.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(spacing.sm))
            if (answer.kind !in suggestionLedKinds) {
                Text(
                    text = stringResource(R.string.feature_assistant_followups),
                    style = SubzeroTheme.typography.label,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = spacing.xxs),
                )
            }
            Suggestions(
                suggestions = answer.suggestions,
                onAsk = onAsk,
                tag = AssistantTestTags::followUp,
            )
        }
        if (answer.excludedForeignCurrency > 0) {
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = pluralStringResource(R.plurals.feature_assistant_answer_foreign, answer.excludedForeignCurrency, answer.excludedForeignCurrency),
                style = SubzeroTheme.typography.caption,
                color = colors.warning,
            )
        }
        if (answer.isRemote || answer.fellBack) {
            Spacer(Modifier.height(spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubzeroTag(
                    text = stringResource(if (answer.isRemote) R.string.feature_assistant_tag_ai else R.string.feature_assistant_tag_on_device),
                    tone = if (answer.isRemote) SubzeroTone.Accent else SubzeroTone.Neutral,
                )
                if (answer.fellBack) {
                    Spacer(Modifier.width(spacing.xs))
                    Text(
                        text = stringResource(R.string.feature_assistant_fell_back),
                        style = SubzeroTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                }
            }
        }
    }
}

/** Which "you have paid" sentence a recorded-spend answer uses. */
private fun SpendPeriod?.recordedLeadRes(): Int = when (this) {
    SpendPeriod.LAST_MONTH -> R.string.feature_assistant_answer_recorded_last_month
    SpendPeriod.THIS_YEAR -> R.string.feature_assistant_answer_recorded_this_year
    SpendPeriod.ALL_TIME -> R.string.feature_assistant_answer_recorded_all_time
    else -> R.string.feature_assistant_answer_recorded_this_month
}

@Composable
private fun Lead(text: String) {
    Text(text = text, style = SubzeroTheme.typography.label, color = SubzeroTheme.colors.textSecondary)
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = SubzeroTheme.typography.bodySmall, color = SubzeroTheme.colors.textPrimary)
}

@Composable
private fun HeroAmount(answer: AssistantAnswer) {
    SubzeroMoneyText(money = checkNotNull(answer.amount), size = MoneyTextSize.Large, color = SubzeroTheme.colors.accentBright, animate = false)
}

@Composable
private fun ItemList(
    items: List<AnswerItem>,
    today: LocalDate,
    onOpenSubscription: (SubscriptionId) -> Unit,
    showCategory: Boolean = false,
    showStatus: Boolean = false,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val formatter = SubzeroTheme.moneyFormatter
    Column(modifier = Modifier.padding(top = spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        items.forEach { item ->
            SubzeroCard(contentPadding = spacing.sm, onClick = { onOpenSubscription(item.subscription.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubzeroServiceIcon(name = item.subscription.name)
                    Spacer(Modifier.width(spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        val category = item.category
                        Text(
                            text = category?.label() ?: item.subscription.name,
                            style = SubzeroTheme.typography.body,
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                        val detail = when {
                            category != null -> item.subscription.name
                            showStatus -> item.subscription.status.label()
                            item.previousAmount != null && item.date != null ->
                                stringResource(R.string.feature_assistant_answer_increase_item, formatter.format(item.previousAmount!!), shortDate(item.date!!, today))
                            item.date != null -> relativeDate(item.date!!, today)
                            showCategory -> item.subscription.category.label()
                            else -> item.subscription.billingCycle.cadence()
                        }
                        Text(text = detail, style = SubzeroTheme.typography.caption, color = colors.textTertiary, maxLines = 1)
                    }
                    SubzeroMoneyText(money = item.amount, animate = false)
                }
            }
        }
    }
}

@Composable
private fun InputBar(state: AssistantUiState, onInput: (String) -> Unit, onSend: () -> Unit) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screen, vertical = spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        SubzeroTextField(
            value = state.input,
            onValueChange = onInput,
            label = stringResource(R.string.feature_assistant_input_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            inputTag = AssistantTestTags.INPUT,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(spacing.xs))
        IconButton(onClick = onSend, enabled = state.canSend, modifier = Modifier.testTag(AssistantTestTags.SEND)) {
            Icon(
                imageVector = SubzeroIcons.Sparkle,
                contentDescription = stringResource(R.string.feature_assistant_send),
                tint = if (state.canSend) colors.accent else colors.textTertiary,
            )
        }
    }
}
