package com.subzero.feature.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.component.pressScale
import com.subzero.core.designsystem.format.longDate
import com.subzero.core.designsystem.theme.SubzeroMotion
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.UpcomingPayment
import java.time.LocalDate

private const val OUT_OF_MONTH_ALPHA = 0.35f
private const val SELECTED_SCALE = 1.06f
private val MarkerSize = 5.dp

/**
 * One day in the month grid. Payment days carry a dot and a compact amount; the selected day
 * fills with the accent container and scales up slightly; today keeps an accent outline.
 * The whole cell is one accessible node describing the date and its charge count.
 */
@Composable
internal fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    selected: Boolean,
    payments: List<UpcomingPayment>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val motion = SubzeroTheme.motion
    val formatter = SubzeroTheme.moneyFormatter
    val interactionSource = remember { MutableInteractionSource() }
    val hasPayments = payments.isNotEmpty()

    val background by animateColorAsState(
        targetValue = if (selected) colors.accentContainer else Color.Transparent,
        animationSpec = motion.standardSpec(),
        label = "dayBackground",
    )
    val scale by animateFloatAsState(if (selected) SELECTED_SCALE else 1f, motion.standardSpec(), label = "dayScale")
    val dayColor = when {
        !inMonth -> colors.textTertiary.copy(alpha = OUT_OF_MONTH_ALPHA)
        selected || isToday -> colors.accent
        hasPayments -> colors.textPrimary
        else -> colors.textSecondary
    }
    val dateText = longDate(date)
    val description = if (hasPayments) {
        pluralStringResource(R.plurals.feature_calendar_day_description, payments.size, dateText, payments.size)
    } else {
        stringResource(R.string.feature_calendar_day_no_charges, dateText)
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(0.85f)
            .scale(scale)
            .pressScale(interactionSource, SubzeroMotion.PRESS_SCALE_CARD)
            .clip(SubzeroTheme.shapes.sm)
            .background(background)
            .then(if (isToday) Modifier.border(1.dp, colors.accent.copy(alpha = 0.6f), SubzeroTheme.shapes.sm) else Modifier)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .testTag(CalendarTestTags.day(date)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(SubzeroTheme.spacing.xs))
        Text(
            text = date.dayOfMonth.toString(),
            style = SubzeroTheme.typography.moneySmall,
            color = dayColor,
        )
        if (hasPayments && inMonth) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(MarkerSize)
                    .clip(SubzeroTheme.shapes.full)
                    .background(if (selected) colors.accentBright else colors.accent),
            )
            val total = payments.sameCurrencyTotal()
            if (total != null) {
                Text(
                    text = formatter.formatCompact(total),
                    style = SubzeroTheme.typography.caption,
                    color = if (selected) colors.textPrimary else colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}
