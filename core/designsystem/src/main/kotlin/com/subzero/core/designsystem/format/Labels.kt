package com.subzero.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.subzero.core.designsystem.R
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus

/*
 * Localized labels for domain enums. Features never switch over these enums for display text;
 * they call these so every screen agrees on the wording.
 */

@Composable
@ReadOnlyComposable
fun Category.label(): String = stringResource(
    when (this) {
        Category.ENTERTAINMENT -> R.string.core_designsystem_category_entertainment
        Category.AI -> R.string.core_designsystem_category_ai
        Category.CLOUD -> R.string.core_designsystem_category_cloud
        Category.SOFTWARE -> R.string.core_designsystem_category_software
        Category.FITNESS -> R.string.core_designsystem_category_fitness
        Category.EDUCATION -> R.string.core_designsystem_category_education
        Category.PRODUCTIVITY -> R.string.core_designsystem_category_productivity
        Category.GAMING -> R.string.core_designsystem_category_gaming
        Category.OTHER -> R.string.core_designsystem_category_other
    },
)

/** "Monthly", "Yearly", "Every 6 months". */
@Composable
@ReadOnlyComposable
fun BillingCycle.label(): String = when (this) {
    BillingCycle.Weekly -> stringResource(R.string.core_designsystem_cycle_weekly)
    BillingCycle.Monthly -> stringResource(R.string.core_designsystem_cycle_monthly)
    BillingCycle.Quarterly -> stringResource(R.string.core_designsystem_cycle_quarterly)
    BillingCycle.Yearly -> stringResource(R.string.core_designsystem_cycle_yearly)
    is BillingCycle.Custom -> pluralStringResource(
        when (unit) {
            CycleUnit.DAY -> R.plurals.core_designsystem_cycle_every_days
            CycleUnit.WEEK -> R.plurals.core_designsystem_cycle_every_weeks
            CycleUnit.MONTH -> R.plurals.core_designsystem_cycle_every_months
            CycleUnit.YEAR -> R.plurals.core_designsystem_cycle_every_years
        },
        every,
        every,
    )
}

/** The suffix after a price: "/ month", "/ year", "/ 6 months". */
@Composable
@ReadOnlyComposable
fun BillingCycle.cadence(): String = when (this) {
    BillingCycle.Weekly -> stringResource(R.string.core_designsystem_cadence_week)
    BillingCycle.Monthly -> stringResource(R.string.core_designsystem_cadence_month)
    BillingCycle.Quarterly -> stringResource(R.string.core_designsystem_cadence_quarter)
    BillingCycle.Yearly -> stringResource(R.string.core_designsystem_cadence_year)
    is BillingCycle.Custom -> pluralStringResource(
        when (unit) {
            CycleUnit.DAY -> R.plurals.core_designsystem_cadence_custom_days
            CycleUnit.WEEK -> R.plurals.core_designsystem_cadence_custom_weeks
            CycleUnit.MONTH -> R.plurals.core_designsystem_cadence_custom_months
            CycleUnit.YEAR -> R.plurals.core_designsystem_cadence_custom_years
        },
        every,
        every,
    )
}

@Composable
@ReadOnlyComposable
fun SubscriptionStatus.label(): String = stringResource(
    when (this) {
        SubscriptionStatus.ACTIVE -> R.string.core_designsystem_status_active
        SubscriptionStatus.PAUSED -> R.string.core_designsystem_status_paused
        SubscriptionStatus.CANCELED -> R.string.core_designsystem_status_canceled
    },
)

@Composable
@ReadOnlyComposable
fun DeclaredUsage.label(): String = stringResource(
    when (this) {
        DeclaredUsage.DAILY -> R.string.core_designsystem_usage_daily
        DeclaredUsage.WEEKLY -> R.string.core_designsystem_usage_weekly
        DeclaredUsage.MONTHLY -> R.string.core_designsystem_usage_monthly
        DeclaredUsage.RARELY -> R.string.core_designsystem_usage_rarely
        DeclaredUsage.UNKNOWN -> R.string.core_designsystem_usage_unknown
    },
)
