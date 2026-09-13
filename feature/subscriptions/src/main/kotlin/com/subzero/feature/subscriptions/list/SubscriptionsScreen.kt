package com.subzero.feature.subscriptions.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.LocalNavEntryAnimatedScope
import com.subzero.core.designsystem.component.SharedElementKeys
import com.subzero.core.designsystem.component.SubzeroBottomSheet
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroEmptyState
import com.subzero.core.designsystem.component.SubzeroSubscriptionRow
import com.subzero.core.designsystem.component.SubzeroSubscriptionRowSkeleton
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.component.sharedBoundsIfAvailable
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.label
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.usecase.SubscriptionFilter
import com.subzero.core.domain.usecase.SubscriptionSort
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.feature.subscriptions.R
import java.time.LocalDate

internal object SubscriptionsTestTags {
    const val LIST = "subscriptions_list"
    const val SEARCH_TOGGLE = "subscriptions_search_toggle"
    const val SEARCH_FIELD = "subscriptions_search_field"
    const val SORT = "subscriptions_sort"
    const val ADD = "subscriptions_add"
    const val EMPTY = "subscriptions_empty"
    const val NO_MATCHES = "subscriptions_no_matches"
    fun row(id: String) = "subscription_row_$id"
}

private val ContentMaxWidth = 640.dp
private const val SKELETON_ROWS = 4

@Composable
fun SubscriptionsRoute(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    SubscriptionsScreen(
        state = state,
        onQueryChange = viewModel::setQuery,
        onSearchActiveChange = viewModel::setSearchActive,
        onFilterChange = viewModel::setFilter,
        onSortChange = viewModel::setSort,
        onAdd = { navigator.navigate(SubscriptionFormKey()) },
        onOpen = { navigator.navigate(SubscriptionDetailKey(it.value)) },
    )
}

@Composable
fun SubscriptionsScreen(
    state: SubscriptionsUiState,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onFilterChange: (SubscriptionFilter) -> Unit,
    onSortChange: (SubscriptionSort) -> Unit,
    onAdd: () -> Unit,
    onOpen: (com.subzero.core.domain.model.SubscriptionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    var showSort by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }

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
            Header(
                state = state,
                onQueryChange = onQueryChange,
                onSearchActiveChange = onSearchActiveChange,
                onSortClick = { showSort = true },
            )
            FilterBar(
                filter = state.filter,
                onFilterChange = onFilterChange,
                onCategoryClick = { showCategories = true },
            )
            Spacer(Modifier.height(spacing.xs))
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingList()
                    state.isEmpty -> SubzeroEmptyState(
                        title = stringResource(R.string.feature_subscriptions_empty_title),
                        description = stringResource(R.string.feature_subscriptions_empty_body),
                        actionLabel = stringResource(R.string.feature_subscriptions_add),
                        onAction = onAdd,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(SubscriptionsTestTags.EMPTY),
                    )
                    state.hasNoMatches -> SubzeroEmptyState(
                        title = stringResource(R.string.feature_subscriptions_no_matches_title),
                        description = stringResource(R.string.feature_subscriptions_no_matches_body),
                        icon = SubzeroIcons.Search,
                        actionLabel = stringResource(R.string.feature_subscriptions_clear_filters),
                        onAction = {
                            onFilterChange(SubscriptionFilter.All)
                            onSearchActiveChange(false)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(SubscriptionsTestTags.NO_MATCHES),
                    )
                    else -> SubscriptionList(items = state.items, today = state.today, onOpen = onOpen)
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isEmpty && !state.isLoading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.screen),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            SubzeroButton(
                text = stringResource(R.string.feature_subscriptions_add),
                onClick = onAdd,
                leadingIcon = SubzeroIcons.Add,
                modifier = Modifier.testTag(SubscriptionsTestTags.ADD),
            )
        }
    }

    if (showSort) {
        SortSheet(current = state.sort, onSelect = { onSortChange(it); showSort = false }, onDismiss = { showSort = false })
    }
    if (showCategories) {
        CategorySheet(
            current = (state.filter as? SubscriptionFilter.InCategory)?.category,
            onSelect = { onFilterChange(SubscriptionFilter.InCategory(it)); showCategories = false },
            onClear = { onFilterChange(SubscriptionFilter.All); showCategories = false },
            onDismiss = { showCategories = false },
        )
    }
}

@Composable
private fun Header(
    state: SubscriptionsUiState,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val motion = SubzeroTheme.motion
    AnimatedContent(
        targetState = state.searchActive,
        transitionSpec = { fadeIn(motion.standardSpec()) togetherWith fadeOut(motion.fastSpec()) },
        label = "header",
    ) { searching ->
        if (searching) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onSearchActiveChange(false) }) {
                    Icon(SubzeroIcons.Back, contentDescription = stringResource(R.string.feature_subscriptions_close_search), tint = colors.textPrimary)
                }
                SubzeroTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.feature_subscriptions_search),
                    placeholder = stringResource(R.string.feature_subscriptions_search_hint),
                    showClearButton = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    inputTag = SubscriptionsTestTags.SEARCH_FIELD,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.sm)
                        .focusRequester(focusRequester),
                )
            }
        } else {
            SubzeroTopBar(
                title = stringResource(R.string.feature_subscriptions_title),
                subtitle = if (state.totalCount > 0) pluralStringResource(R.plurals.feature_subscriptions_count, state.totalCount, state.totalCount) else null,
                actions = {
                    IconButton(onClick = { onSearchActiveChange(true) }, modifier = Modifier.testTag(SubscriptionsTestTags.SEARCH_TOGGLE)) {
                        Icon(SubzeroIcons.Search, contentDescription = stringResource(R.string.feature_subscriptions_search), tint = colors.textPrimary)
                    }
                    IconButton(onClick = onSortClick, modifier = Modifier.testTag(SubscriptionsTestTags.SORT)) {
                        Icon(SubzeroIcons.More, contentDescription = stringResource(R.string.feature_subscriptions_sort), tint = colors.textPrimary)
                    }
                },
            )
        }
    }
}

@Composable
private fun FilterBar(
    filter: SubscriptionFilter,
    onFilterChange: (SubscriptionFilter) -> Unit,
    onCategoryClick: () -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    val category = (filter as? SubscriptionFilter.InCategory)?.category
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SubzeroChip(stringResource(R.string.feature_subscriptions_filter_all), filter == SubscriptionFilter.All, { onFilterChange(SubscriptionFilter.All) })
        SubzeroChip(stringResource(R.string.feature_subscriptions_filter_monthly), filter == SubscriptionFilter.Cycle(BillingCycle.Monthly), { onFilterChange(SubscriptionFilter.Cycle(BillingCycle.Monthly)) })
        SubzeroChip(stringResource(R.string.feature_subscriptions_filter_yearly), filter == SubscriptionFilter.Cycle(BillingCycle.Yearly), { onFilterChange(SubscriptionFilter.Cycle(BillingCycle.Yearly)) })
        SubzeroChip(stringResource(R.string.feature_subscriptions_filter_upcoming), filter is SubscriptionFilter.Upcoming, { onFilterChange(SubscriptionFilter.Upcoming()) })
        SubzeroChip(stringResource(R.string.feature_subscriptions_filter_high_cost), filter == SubscriptionFilter.HighCost, { onFilterChange(SubscriptionFilter.HighCost) })
        SubzeroChip(
            text = category?.label() ?: stringResource(R.string.feature_subscriptions_filter_category),
            selected = category != null,
            onClick = onCategoryClick,
            icon = SubzeroIcons.ChevronDown,
        )
    }
}

@Composable
private fun SubscriptionList(
    items: List<Subscription>,
    today: LocalDate,
    onOpen: (com.subzero.core.domain.model.SubscriptionId) -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    val animatedScope = LocalNavEntryAnimatedScope.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SubscriptionsTestTags.LIST),
        contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        items(items, key = { it.id.value }) { subscription ->
            val inactive = subscription.status != SubscriptionStatus.ACTIVE
            SubzeroSubscriptionRow(
                name = subscription.name,
                price = subscription.price,
                cadence = subscription.billingCycle.cadence(),
                nextCharge = if (inactive) subscription.status.label() else stringResource(R.string.feature_subscriptions_next_prefix, relativeDate(subscription.nextBillingDate, today)),
                statusBadge = if (inactive) subscription.status.label() else null,
                muted = inactive,
                onClick = { onOpen(subscription.id) },
                modifier = Modifier
                    .animateItem()
                    .sharedBoundsIfAvailable(SharedElementKeys.subscriptionCard(subscription.id.value), animatedScope)
                    .testTag(SubscriptionsTestTags.row(subscription.id.value)),
            )
        }
    }
}

@Composable
private fun LoadingList() {
    Column(modifier = Modifier.padding(horizontal = SubzeroTheme.spacing.xs)) {
        repeat(SKELETON_ROWS) { SubzeroSubscriptionRowSkeleton() }
    }
}

@Composable
private fun SortSheet(current: SubscriptionSort, onSelect: (SubscriptionSort) -> Unit, onDismiss: () -> Unit) {
    SubzeroBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.feature_subscriptions_sort_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(SubzeroTheme.spacing.xs)) {
            SubscriptionSort.entries.forEach { sort ->
                SubzeroChip(text = sort.label(), selected = sort == current, onClick = { onSelect(sort) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CategorySheet(current: Category?, onSelect: (Category) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    SubzeroBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.feature_subscriptions_filter_category_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(SubzeroTheme.spacing.xs)) {
            Category.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(SubzeroTheme.spacing.xs)) {
                    pair.forEach { category ->
                        SubzeroChip(text = category.label(), selected = category == current, onClick = { onSelect(category) }, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(SubzeroTheme.spacing.xs))
            SubzeroButton(
                text = stringResource(R.string.feature_subscriptions_clear_filters),
                onClick = onClear,
                style = SubzeroButtonStyle.Ghost,
                compact = true,
            )
        }
    }
}

@Composable
private fun SubscriptionSort.label(): String = stringResource(
    when (this) {
        SubscriptionSort.HIGHEST_COST -> R.string.feature_subscriptions_sort_highest_cost
        SubscriptionSort.LOWEST_COST -> R.string.feature_subscriptions_sort_lowest_cost
        SubscriptionSort.NEXT_PAYMENT -> R.string.feature_subscriptions_sort_next_payment
        SubscriptionSort.ALPHABETICAL -> R.string.feature_subscriptions_sort_alphabetical
    },
)
