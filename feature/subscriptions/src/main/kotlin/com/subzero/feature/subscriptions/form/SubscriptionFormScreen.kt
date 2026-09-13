package com.subzero.feature.subscriptions.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroCurrencySheet
import com.subzero.core.designsystem.component.SubzeroErrorState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroSwitch
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.format.label
import com.subzero.core.designsystem.format.longDate
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.usecase.SubscriptionValidationError
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.feature.subscriptions.R

internal object FormTestTags {
    const val NAME = "form_name"
    const val PRICE = "form_price"
    const val CURRENCY = "form_currency"
    const val SAVE = "form_save"
    const val SAVED = "form_saved"
    const val NEXT_DATE = "form_next_date"
    const val FIRST_DATE_TOGGLE = "form_first_date_toggle"
    const val CUSTOM_EVERY = "form_custom_every"
    fun cycle(cycle: String) = "form_cycle_$cycle"
    fun category(category: Category) = "form_category_${category.name}"
    fun usage(usage: DeclaredUsage) = "form_usage_${usage.name}"
}

private val ContentMaxWidth = 640.dp

@Composable
fun SubscriptionFormRoute(
    key: SubscriptionFormKey,
    viewModel: SubscriptionFormViewModel = hiltViewModel<SubscriptionFormViewModel, SubscriptionFormViewModel.Factory>(
        creationCallback = { it.create(key) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    SubscriptionFormScreen(
        state = state,
        actions = FormActions(
            onName = viewModel::setName,
            onPrice = viewModel::setPriceText,
            onCurrency = viewModel::setCurrency,
            onStandardCycle = viewModel::setStandardCycle,
            onCustomMode = viewModel::setCustomMode,
            onCustomEvery = viewModel::setCustomEvery,
            onCustomUnit = viewModel::setCustomUnit,
            onNextDate = viewModel::setNextPaymentDate,
            onFirstDate = viewModel::setFirstPaymentDate,
            onCategory = viewModel::setCategory,
            onUsage = viewModel::setUsage,
            onNotes = viewModel::setNotes,
            onSave = viewModel::save,
            onDismissFailure = viewModel::dismissFailure,
        ),
        onBack = navigator::back,
        onSaved = navigator::back,
    )
}

/** Every callback the form needs, grouped so the screen signature stays readable. */
data class FormActions(
    val onName: (String) -> Unit,
    val onPrice: (String) -> Unit,
    val onCurrency: (com.subzero.core.domain.model.CurrencyCode) -> Unit,
    val onStandardCycle: (BillingCycle) -> Unit,
    val onCustomMode: (Boolean) -> Unit,
    val onCustomEvery: (Int) -> Unit,
    val onCustomUnit: (CycleUnit) -> Unit,
    val onNextDate: (java.time.LocalDate) -> Unit,
    val onFirstDate: (java.time.LocalDate?) -> Unit,
    val onCategory: (Category) -> Unit,
    val onUsage: (DeclaredUsage) -> Unit,
    val onNotes: (String) -> Unit,
    val onSave: () -> Unit,
    val onDismissFailure: () -> Unit,
)

@Composable
fun SubscriptionFormScreen(
    state: SubscriptionFormState,
    actions: FormActions,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Crossfade(targetState = state.saved, label = "formSaved") { saved ->
            if (saved != null) {
                SavedConfirmation(subscription = saved, isEdit = state.isEdit, today = state.today, onDone = onSaved)
            } else {
                FormContent(state = state, actions = actions, onBack = onBack)
            }
        }
    }
}

@Composable
private fun FormContent(state: SubscriptionFormState, actions: FormActions, onBack: () -> Unit) {
    val spacing = SubzeroTheme.spacing
    val colors = SubzeroTheme.colors
    var showCurrency by remember { mutableStateOf(false) }
    var showNextDatePicker by remember { mutableStateOf(false) }
    var showFirstDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = ContentMaxWidth),
    ) {
        SubzeroTopBar(
            title = stringResource(if (state.isEdit) R.string.feature_subscriptions_form_edit_title else R.string.feature_subscriptions_form_add_title),
            onBack = onBack,
        )
        when {
            state.isLoading -> SubzeroHeroSkeleton(modifier = Modifier.padding(spacing.screen))
            state.failure == FormFailure.NOT_FOUND -> SubzeroErrorState(
                description = stringResource(R.string.feature_subscriptions_form_error_not_found),
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screen)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SubzeroTextField(
                    value = state.name,
                    onValueChange = actions.onName,
                    label = stringResource(R.string.feature_subscriptions_form_name),
                    placeholder = stringResource(R.string.feature_subscriptions_form_name_hint),
                    error = state.errors.nameError(),
                    inputTag = FormTestTags.NAME,
                )

                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    SubzeroTextField(
                        value = state.priceText,
                        onValueChange = actions.onPrice,
                        label = stringResource(R.string.feature_subscriptions_form_price),
                        placeholder = stringResource(R.string.feature_subscriptions_form_price_hint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        leading = {
                            Text(
                                text = SubzeroTheme.moneyFormatter.symbol(state.currency),
                                style = SubzeroTheme.typography.body,
                                color = colors.textSecondary,
                            )
                        },
                        error = if (state.priceInvalid || SubscriptionValidationError.PRICE_MISSING in state.errors) stringResource(R.string.feature_subscriptions_form_error_price) else null,
                        inputTag = FormTestTags.PRICE,
                        modifier = Modifier.weight(1f),
                    )
                    SubzeroButton(
                        text = state.currency.code,
                        onClick = { showCurrency = true },
                        style = SubzeroButtonStyle.Secondary,
                        modifier = Modifier
                            .padding(bottom = if (state.priceInvalid || SubscriptionValidationError.PRICE_MISSING in state.errors) 20.dp else 0.dp)
                            .testTag(FormTestTags.CURRENCY),
                    )
                }

                CycleSection(state = state, actions = actions)

                DatesSection(
                    state = state,
                    actions = actions,
                    onPickNext = { showNextDatePicker = true },
                    onPickFirst = { showFirstDatePicker = true },
                )

                ChoiceSection(
                    title = stringResource(R.string.feature_subscriptions_form_category),
                    options = Category.entries,
                    selected = state.category,
                    label = { it.label() },
                    tag = { FormTestTags.category(it) },
                    onSelect = actions.onCategory,
                )

                ChoiceSection(
                    title = stringResource(R.string.feature_subscriptions_form_usage),
                    options = DeclaredUsage.entries,
                    selected = state.usage,
                    label = { it.label() },
                    tag = { FormTestTags.usage(it) },
                    onSelect = actions.onUsage,
                )

                SubzeroTextField(
                    value = state.notes,
                    onValueChange = actions.onNotes,
                    label = stringResource(R.string.feature_subscriptions_form_notes),
                    placeholder = stringResource(R.string.feature_subscriptions_form_notes_hint),
                    helper = stringResource(R.string.feature_subscriptions_form_optional),
                    error = if (SubscriptionValidationError.NOTES_TOO_LONG in state.errors) pluralStringResource(R.plurals.feature_subscriptions_form_error_notes_long, Subscription.MAX_NOTES_LENGTH, Subscription.MAX_NOTES_LENGTH) else null,
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                AnimatedVisibility(visible = state.failure == FormFailure.SAVE_FAILED) {
                    SubzeroErrorState(
                        description = stringResource(R.string.feature_subscriptions_form_error_save),
                        onRetry = actions.onSave,
                    )
                }

                SubzeroButton(
                    text = stringResource(R.string.feature_subscriptions_form_save),
                    onClick = actions.onSave,
                    loading = state.isSaving,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FormTestTags.SAVE),
                )
                Spacer(Modifier.height(spacing.xl))
            }
        }
    }

    if (showCurrency) {
        SubzeroCurrencySheet(selected = state.currency, onSelect = { actions.onCurrency(it); showCurrency = false }, onDismiss = { showCurrency = false })
    }
    if (showNextDatePicker) {
        SubzeroDatePickerDialog(initial = state.nextPaymentDate, onConfirm = actions.onNextDate, onDismiss = { showNextDatePicker = false })
    }
    if (showFirstDatePicker) {
        SubzeroDatePickerDialog(
            initial = state.firstPaymentDate ?: state.nextPaymentDate,
            onConfirm = { actions.onFirstDate(it) },
            onDismiss = { showFirstDatePicker = false },
        )
    }
}

@Composable
private fun CycleSection(state: SubscriptionFormState, actions: FormActions) {
    val spacing = SubzeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = stringResource(R.string.feature_subscriptions_form_cycle),
            style = SubzeroTheme.typography.label,
            color = SubzeroTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            BillingCycle.standard.forEach { cycle ->
                SubzeroChip(
                    text = cycle.label(),
                    selected = !state.customMode && state.standardCycle == cycle,
                    onClick = { actions.onStandardCycle(cycle) },
                    modifier = Modifier.testTag(FormTestTags.cycle(cycle.toString())),
                )
            }
        }
        SubzeroChip(
            text = stringResource(com.subzero.core.designsystem.R.string.core_designsystem_cycle_custom),
            selected = state.customMode,
            onClick = { actions.onCustomMode(!state.customMode) },
            modifier = Modifier.testTag(FormTestTags.cycle("Custom")),
        )
        AnimatedVisibility(visible = state.customMode) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                SubzeroTextField(
                    value = state.customEvery.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let(actions.onCustomEvery) },
                    label = stringResource(R.string.feature_subscriptions_form_custom_every),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    inputTag = FormTestTags.CUSTOM_EVERY,
                    modifier = Modifier.width(96.dp),
                )
                Row(
                    modifier = Modifier.padding(bottom = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                ) {
                    CycleUnit.entries.forEach { unit ->
                        SubzeroChip(text = unit.label(), selected = state.customUnit == unit, onClick = { actions.onCustomUnit(unit) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DatesSection(
    state: SubscriptionFormState,
    actions: FormActions,
    onPickNext: () -> Unit,
    onPickFirst: () -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PickerField(
            label = stringResource(R.string.feature_subscriptions_form_next_payment),
            value = longDate(state.nextPaymentDate),
            onClick = onPickNext,
            helper = if (state.showsProjection) {
                stringResource(R.string.feature_subscriptions_form_projected_next, relativeDate(state.projectedNextDate, state.today))
            } else {
                null
            },
            error = if (SubscriptionValidationError.FIRST_PAYMENT_AFTER_NEXT in state.errors) stringResource(R.string.feature_subscriptions_form_error_first_after_next) else null,
            modifier = Modifier.testTag(FormTestTags.NEXT_DATE),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.feature_subscriptions_form_first_payment_toggle),
                    style = SubzeroTheme.typography.body,
                    color = SubzeroTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.feature_subscriptions_form_first_payment_help),
                    style = SubzeroTheme.typography.caption,
                    color = SubzeroTheme.colors.textTertiary,
                )
            }
            SubzeroSwitch(
                checked = state.firstPaymentDate != null,
                onCheckedChange = { on -> actions.onFirstDate(if (on) state.nextPaymentDate else null) },
                modifier = Modifier.testTag(FormTestTags.FIRST_DATE_TOGGLE),
            )
        }
        AnimatedVisibility(visible = state.firstPaymentDate != null) {
            PickerField(
                label = stringResource(R.string.feature_subscriptions_form_first_payment),
                value = state.firstPaymentDate?.let { longDate(it) }.orEmpty(),
                onClick = onPickFirst,
            )
        }
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val spacing = SubzeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(text = title, style = SubzeroTheme.typography.label, color = SubzeroTheme.colors.textSecondary)
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                row.forEach { option ->
                    SubzeroChip(
                        text = label(option),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tag(option)),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CycleUnit.label(): String = stringResource(
    when (this) {
        CycleUnit.DAY -> R.string.feature_subscriptions_form_unit_days
        CycleUnit.WEEK -> R.string.feature_subscriptions_form_unit_weeks
        CycleUnit.MONTH -> R.string.feature_subscriptions_form_unit_months
        CycleUnit.YEAR -> R.string.feature_subscriptions_form_unit_years
    },
)

@Composable
private fun Set<SubscriptionValidationError>.nameError(): String? = when {
    SubscriptionValidationError.NAME_BLANK in this -> stringResource(R.string.feature_subscriptions_form_error_name_blank)
    SubscriptionValidationError.NAME_TOO_LONG in this -> pluralStringResource(R.plurals.feature_subscriptions_form_error_name_long, Subscription.MAX_NAME_LENGTH, Subscription.MAX_NAME_LENGTH)
    else -> null
}
