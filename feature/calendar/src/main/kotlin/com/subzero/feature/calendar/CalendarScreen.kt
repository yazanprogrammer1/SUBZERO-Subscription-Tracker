package com.subzero.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroEmptyState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroSubscriptionRow
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.longDate
import com.subzero.core.designsystem.format.monthYear
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.UpcomingPayment
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import kotlin.math.abs

internal object CalendarTestTags {
    const val PREVIOUS = "calendar_previous"
    const val NEXT = "calendar_next"
    const val TODAY = "calendar_today"
    const val MONTH_TITLE = "calendar_month_title"
    const val EMPTY = "calendar_empty"
    fun day(date: LocalDate) = "calendar_day_$date"
}

private val ContentMaxWidth = 640.dp
private const val SWIPE_THRESHOLD_PX = 80f

@Composable
fun CalendarRoute(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::goToToday,
        onSelectDay = viewModel::selectDay,
        onOpenSubscription = { navigator.navigate(SubscriptionDetailKey(it.value)) },
        onAdd = { navigator.navigate(SubscriptionFormKey()) },
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onOpenSubscription: (SubscriptionId) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
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
                title = stringResource(R.string.feature_calendar_title),
                actions = {
                    if (state.month != YearMonth.from(state.today)) {
                        SubzeroButton(
                            text = stringResource(R.string.feature_calendar_today),
                            onClick = onToday,
                            style = SubzeroButtonStyle.Ghost,
                            compact = true,
                            modifier = Modifier.testTag(CalendarTestTags.TODAY),
                        )
                    }
                },
            )
            when {
                state.isLoading -> SubzeroHeroSkeleton(modifier = Modifier.padding(spacing.screen))
                !state.hasSubscriptions -> SubzeroEmptyState(
                    title = stringResource(R.string.feature_calendar_empty_title),
                    description = stringResource(R.string.feature_calendar_empty_body),
                    icon = SubzeroIcons.Calendar,
                    actionLabel = stringResource(R.string.feature_calendar_add),
                    onAction = onAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(CalendarTestTags.EMPTY),
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screen),
                ) {
                    MonthHeader(state)
                    Spacer(Modifier.height(spacing.md))
                    MonthNavigation(state.month, onPreviousMonth, onNextMonth)
                    Spacer(Modifier.height(spacing.xs))
                    MonthGrid(state, onSelectDay, onPreviousMonth, onNextMonth)
                    Spacer(Modifier.height(spacing.lg))
                    DayList(state, onOpenSubscription)
                    Spacer(Modifier.height(spacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(state: CalendarUiState) {
    val colors = SubzeroTheme.colors
    val isCurrent = state.month == YearMonth.from(state.today)
    Column {
        SubzeroMoneyText(money = state.monthTotal, size = MoneyTextSize.Hero)
        Text(
            text = if (isCurrent) stringResource(R.string.feature_calendar_scheduled_this_month) else stringResource(R.string.feature_calendar_scheduled_in_month, monthYear(state.month)),
            style = SubzeroTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (state.foreignCurrencyCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.feature_calendar_foreign_excluded, state.foreignCurrencyCount, state.foreignCurrencyCount),
                style = SubzeroTheme.typography.caption,
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun MonthNavigation(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = SubzeroTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag(CalendarTestTags.PREVIOUS)) {
            Icon(SubzeroIcons.Back, contentDescription = stringResource(R.string.feature_calendar_previous_month), tint = colors.textPrimary)
        }
        Text(
            text = monthYear(month),
            style = SubzeroTheme.typography.title,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .testTag(CalendarTestTags.MONTH_TITLE),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, modifier = Modifier.testTag(CalendarTestTags.NEXT)) {
            Icon(SubzeroIcons.ChevronRight, contentDescription = stringResource(R.string.feature_calendar_next_month), tint = colors.textPrimary)
        }
    }
}

@Composable
private fun MonthGrid(
    state: CalendarUiState,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val motion = SubzeroTheme.motion
    val locale = LocalConfiguration.current.locales[0]
    val weekFields = remember(locale) { WeekFields.of(locale) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(onPreviousMonth, onNextMonth) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (abs(drag) >= SWIPE_THRESHOLD_PX) {
                            if (drag < 0) onNextMonth() else onPreviousMonth()
                        }
                    },
                    onHorizontalDrag = { _, amount -> drag += amount },
                )
            },
    ) {
        WeekdayHeader(weekFields)
        AnimatedContent(
            targetState = state.month,
            transitionSpec = {
                val forward = targetState > initialState
                val direction = if (forward) 1 else -1
                (slideInHorizontally(motion.standardSpec()) { direction * it / 3 } + fadeIn(motion.standardSpec()))
                    .togetherWith(slideOutHorizontally(motion.standardSpec()) { -direction * it / 3 } + fadeOut(motion.fastSpec()))
            },
            label = "month",
        ) { month ->
            val cells = remember(month, weekFields) { monthCells(month, weekFields) }
            Column {
                cells.chunked(DAYS_PER_WEEK).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                inMonth = YearMonth.from(date) == month,
                                isToday = date == state.today,
                                selected = date == state.selectedDay,
                                payments = state.paymentsByDay[date].orEmpty(),
                                onClick = { onSelectDay(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(weekFields: WeekFields) {
    val locale = LocalConfiguration.current.locales[0]
    val first = weekFields.firstDayOfWeek
    Row(modifier = Modifier.fillMaxWidth()) {
        repeat(DAYS_PER_WEEK) { offset ->
            val day = first.plus(offset.toLong())
            Text(
                text = day.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                style = SubzeroTheme.typography.caption,
                color = SubzeroTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = SubzeroTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun DayList(state: CalendarUiState, onOpenSubscription: (SubscriptionId) -> Unit) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val selected = state.selectedDay
    val payments = if (selected != null) state.selectedPayments else state.paymentsByDay.values.flatten()
    Column {
        SectionHeader(
            title = if (selected != null) longDate(selected) else stringResource(R.string.feature_calendar_whole_month),
        )
        when {
            state.monthIsEmpty -> SubzeroCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.feature_calendar_no_charges_month, monthYear(state.month)),
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            payments.isEmpty() && selected != null -> SubzeroCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.feature_calendar_no_charges_day, relativeDate(selected, state.today).lowercase()),
                    style = SubzeroTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                payments.sortedWith(compareBy({ it.date }, { it.subscription.name.lowercase() })).forEach { payment ->
                    SubzeroSubscriptionRow(
                        name = payment.subscription.name,
                        price = payment.amount,
                        cadence = payment.subscription.billingCycle.cadence(),
                        nextCharge = relativeDate(payment.date, state.today),
                        onClick = { onOpenSubscription(payment.subscription.id) },
                    )
                }
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7
private const val GRID_ROWS = 6

/** 42 dates covering the month, starting on the locale's first day of the week. */
internal fun monthCells(month: YearMonth, weekFields: WeekFields): List<LocalDate> {
    val first = month.atDay(1)
    val offset = ((first.dayOfWeek.value - weekFields.firstDayOfWeek.value) + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val start = first.minusDays(offset.toLong())
    return List(DAYS_PER_WEEK * GRID_ROWS) { start.plusDays(it.toLong()) }
}

/** Sum for a day's cell label; null when the currencies differ. */
internal fun List<UpcomingPayment>.sameCurrencyTotal(): Money? {
    if (isEmpty()) return null
    val currency = first().amount.currency
    if (any { it.amount.currency != currency }) return null
    return fold(Money.zero(currency)) { acc, p -> acc + p.amount }
}
