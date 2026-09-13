package com.subzero.feature.subscriptions.form

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.component.ServiceIconSize
import com.subzero.core.designsystem.component.SubzeroBottomSheet
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroIconContainer
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroServiceIcon
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.format.cadence
import com.subzero.core.designsystem.format.longDate
import com.subzero.core.designsystem.format.relativeDate
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.Subscription
import com.subzero.feature.subscriptions.R
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val SAVED_DISPLAY_MS = 1_200L

/** A read-only field that opens a picker; looks like a text field so the form reads as one. */
@Composable
internal fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    Column(modifier = modifier) {
        Text(
            text = label,
            style = SubzeroTheme.typography.label,
            color = if (error != null) colors.danger else colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.xs),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(SubzeroTheme.shapes.sm)
                .background(colors.surfaceSubtle)
                .border(1.dp, if (error != null) colors.danger else colors.outline, SubzeroTheme.shapes.sm)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = SubzeroTheme.typography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Icon(SubzeroIcons.ChevronDown, contentDescription = null, tint = colors.textTertiary)
        }
        if (error != null || helper != null) {
            Text(
                text = error ?: helper.orEmpty(),
                style = SubzeroTheme.typography.caption,
                color = if (error != null) colors.danger else colors.textTertiary,
                modifier = Modifier.padding(top = spacing.xxs, start = spacing.xxs),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubzeroDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            SubzeroButton(
                text = stringResource(R.string.feature_subscriptions_form_ok),
                compact = true,
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    onDismiss()
                },
                modifier = Modifier.padding(end = SubzeroTheme.spacing.xs, bottom = SubzeroTheme.spacing.xs),
            )
        },
        dismissButton = {
            SubzeroButton(
                text = stringResource(com.subzero.core.designsystem.R.string.core_designsystem_cancel),
                style = SubzeroButtonStyle.Ghost,
                compact = true,
                onClick = onDismiss,
            )
        },
        colors = DatePickerDefaults.colors(containerColor = colors.surfaceElevated),
    ) {
        DatePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(
                containerColor = colors.surfaceElevated,
                selectedDayContainerColor = colors.accent,
                selectedDayContentColor = colors.onAccent,
                todayDateBorderColor = colors.accent,
                todayContentColor = colors.accent,
            ),
        )
    }
}

/**
 * Polished confirmation after a save (design-system.md §6.3): the check scales in, the summary
 * fades in, and the screen returns on its own after a moment.
 */
@Composable
internal fun SavedConfirmation(
    subscription: Subscription,
    isEdit: Boolean,
    today: LocalDate,
    onDone: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val motion = SubzeroTheme.motion
    var shown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (shown) 1f else 0.6f, motion.emphasizedEnterSpec(), label = "checkScale")
    LaunchedEffect(Unit) {
        shown = true
        delay(SAVED_DISPLAY_MS)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(FormTestTags.SAVED),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(spacing.screen)) {
            SubzeroIconContainer(
                icon = SubzeroIcons.Check,
                tone = SubzeroTone.Positive,
                size = ServiceIconSize.Header,
                modifier = Modifier.scale(scale),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = stringResource(if (isEdit) R.string.feature_subscriptions_form_saved_updated else R.string.feature_subscriptions_form_saved_added),
                style = SubzeroTheme.typography.title,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.lg))
            SubzeroCard(style = SubzeroCardStyle.Glass, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubzeroServiceIcon(name = subscription.name)
                    Spacer(Modifier.width(spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = subscription.name, style = SubzeroTheme.typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(R.string.feature_subscriptions_form_saved_next, relativeDate(subscription.nextBillingDate, today)),
                            style = SubzeroTheme.typography.caption,
                            color = colors.textTertiary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SubzeroMoneyText(money = subscription.price, animate = false)
                        Text(text = subscription.billingCycle.cadence(), style = SubzeroTheme.typography.caption, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}
