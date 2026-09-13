package com.subzero.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroInsightCard
import com.subzero.core.designsystem.component.SubzeroSwitch
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone

internal object SettingsTestTags {
    const val UPCOMING = "settings_upcoming"
    const val SUMMARY = "settings_summary"
    const val SAVINGS = "settings_savings"
    const val PERMISSION = "settings_permission"
    fun days(days: Int) = "settings_days_$days"
}

private val ContentMaxWidth = 640.dp
private val daysOptions = listOf(1, 2, 3, 7)

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermission()
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }
    SettingsScreen(
        state = state,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(appNotificationSettingsIntent(context.packageName))
            }
        },
        onOpenSystemSettings = { context.startActivity(appNotificationSettingsIntent(context.packageName)) },
        onUpcoming = viewModel::setUpcomingEnabled,
        onDaysBefore = viewModel::setUpcomingDaysBefore,
        onSummary = viewModel::setMonthlySummaryEnabled,
        onSavings = viewModel::setSavingsInsightsEnabled,
    )
}

private fun appNotificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onUpcoming: (Boolean) -> Unit,
    onDaysBefore: (Int) -> Unit,
    onSummary: (Boolean) -> Unit,
    onSavings: (Boolean) -> Unit,
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
            SubzeroTopBar(title = stringResource(R.string.feature_settings_title))
            when (state) {
                SettingsUiState.Loading -> SubzeroHeroSkeleton(modifier = Modifier.padding(spacing.screen))
                is SettingsUiState.Ready -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    NotificationsSection(state, onRequestPermission, onOpenSystemSettings, onUpcoming, onDaysBefore, onSummary, onSavings)
                    Text(
                        text = stringResource(R.string.feature_settings_more_soon),
                        style = SubzeroTheme.typography.caption,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = spacing.md),
                    )
                    Spacer(Modifier.height(spacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    state: SettingsUiState.Ready,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onUpcoming: (Boolean) -> Unit,
    onDaysBefore: (Int) -> Unit,
    onSummary: (Boolean) -> Unit,
    onSavings: (Boolean) -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val notifications = state.preferences.notifications
    SectionHeader(title = stringResource(R.string.feature_settings_notifications))
    Text(
        text = stringResource(R.string.feature_settings_notifications_intro),
        style = SubzeroTheme.typography.caption,
        color = colors.textTertiary,
    )
    AnimatedVisibility(visible = state.anyNotificationEnabled && !state.notificationsAllowed) {
        SubzeroInsightCard(
            title = stringResource(R.string.feature_settings_notifications_permission_title),
            description = stringResource(R.string.feature_settings_notifications_permission_body),
            icon = SubzeroIcons.Notifications,
            tone = SubzeroTone.Warning,
            actionLabel = stringResource(R.string.feature_settings_notifications_allow),
            onAction = onRequestPermission,
            modifier = Modifier.testTag(SettingsTestTags.PERMISSION),
        )
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = stringResource(R.string.feature_settings_upcoming_title),
            body = stringResource(R.string.feature_settings_upcoming_body),
            checked = notifications.upcomingChargeEnabled,
            onChecked = onUpcoming,
            tag = SettingsTestTags.UPCOMING,
        )
        AnimatedVisibility(visible = notifications.upcomingChargeEnabled) {
            Column {
                Spacer(Modifier.height(spacing.sm))
                Text(text = stringResource(R.string.feature_settings_upcoming_days), style = SubzeroTheme.typography.label, color = colors.textSecondary)
                Spacer(Modifier.height(spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    daysOptions.forEach { days ->
                        SubzeroChip(
                            text = pluralStringResource(R.plurals.feature_settings_days_before, days, days),
                            selected = notifications.upcomingChargeDaysBefore == days,
                            onClick = { onDaysBefore(days) },
                            modifier = Modifier.testTag(SettingsTestTags.days(days)),
                        )
                    }
                }
            }
        }
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = stringResource(R.string.feature_settings_summary_title),
            body = stringResource(R.string.feature_settings_summary_body),
            checked = notifications.monthlySummaryEnabled,
            onChecked = onSummary,
            tag = SettingsTestTags.SUMMARY,
        )
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = stringResource(R.string.feature_settings_savings_title),
            body = stringResource(R.string.feature_settings_savings_body),
            checked = notifications.savingsInsightsEnabled,
            onChecked = onSavings,
            tag = SettingsTestTags.SAVINGS,
        )
    }
    if (state.anyNotificationEnabled && !state.notificationsAllowed) {
        SubzeroButton(
            text = stringResource(R.string.feature_settings_notifications_open_settings),
            onClick = onOpenSystemSettings,
            style = SubzeroButtonStyle.Ghost,
            compact = true,
        )
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChecked: (Boolean) -> Unit, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SubzeroTheme.typography.body, color = SubzeroTheme.colors.textPrimary)
            Text(text = body, style = SubzeroTheme.typography.caption, color = SubzeroTheme.colors.textSecondary)
        }
        Spacer(Modifier.width(SubzeroTheme.spacing.sm))
        SubzeroSwitch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.testTag(tag))
    }
}
