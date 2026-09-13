package com.subzero.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.core.data.export.ExportFormat
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroCurrencySheet
import com.subzero.core.designsystem.component.SubzeroDialog
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroInsightCard
import com.subzero.core.designsystem.component.SubzeroSwitch
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.component.SubzeroWordmark
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.ThemeMode

internal object SettingsTestTags {
    const val UPCOMING = "settings_upcoming"
    const val SUMMARY = "settings_summary"
    const val SAVINGS = "settings_savings"
    const val PERMISSION = "settings_permission"
    const val NAME = "settings_name"
    const val NAME_SAVE = "settings_name_save"
    const val CURRENCY = "settings_currency"
    const val EXPORT_JSON = "settings_export_json"
    const val EXPORT_CSV = "settings_export_csv"
    const val DELETE = "settings_delete"
    const val CONFIRM_DELETE = "settings_confirm_delete"
    fun days(days: Int) = "settings_days_$days"
    fun theme(mode: ThemeMode) = "settings_theme_${mode.name}"
}

private val ContentMaxWidth = 640.dp
private val daysOptions = listOf(1, 2, 3, 7)

/** Every callback the screen needs. */
data class SettingsActions(
    val onRequestPermission: () -> Unit,
    val onOpenSystemSettings: () -> Unit,
    val onUpcoming: (Boolean) -> Unit,
    val onDaysBefore: (Int) -> Unit,
    val onSummary: (Boolean) -> Unit,
    val onSavings: (Boolean) -> Unit,
    val onName: (String?) -> Unit,
    val onCurrency: (CurrencyCode) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onExport: (ExportFormat) -> Unit,
    val onDeleteAll: () -> Unit,
    val onMessageShown: () -> Unit,
)

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingFormat by remember { mutableStateOf(ExportFormat.JSON) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermission()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        if (uri != null) viewModel.exportTo(uri, pendingFormat)
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }
    SettingsScreen(
        state = state,
        actions = SettingsActions(
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
            onName = viewModel::setDisplayName,
            onCurrency = viewModel::setHomeCurrency,
            onTheme = viewModel::setThemeMode,
            onExport = { format ->
                pendingFormat = format
                exportLauncher.launch("subzero-export.${format.extension}")
            },
            onDeleteAll = viewModel::deleteAllData,
            onMessageShown = viewModel::consumeMessage,
        ),
    )
}

private fun appNotificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val snackbar = remember { SnackbarHostState() }
    val message = (state as? SettingsUiState.Ready)?.message
    val messageText = message?.let { stringResource(it.text()) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
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
                    ProfileSection(state, actions)
                    PreferencesSection(state, actions)
                    NotificationsSection(state, actions)
                    PrivacySection()
                    DataSection(state, actions)
                    AboutSection(state)
                    Spacer(Modifier.height(spacing.xxl))
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProfileSection(state: SettingsUiState.Ready, actions: SettingsActions) {
    val spacing = SubzeroTheme.spacing
    var draft by rememberSaveable(state.preferences.displayName) { mutableStateOf(state.preferences.displayName.orEmpty()) }
    SectionHeader(title = stringResource(R.string.feature_settings_profile))
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SubzeroTextField(
                value = draft,
                onValueChange = { draft = it },
                label = stringResource(R.string.feature_settings_name),
                placeholder = stringResource(R.string.feature_settings_name_hint),
                inputTag = SettingsTestTags.NAME,
                modifier = Modifier.weight(1f),
            )
            SubzeroButton(
                text = stringResource(R.string.feature_settings_name_save),
                onClick = { actions.onName(draft.ifBlank { null }) },
                style = SubzeroButtonStyle.Secondary,
                enabled = draft.trim() != state.preferences.displayName.orEmpty(),
                modifier = Modifier.testTag(SettingsTestTags.NAME_SAVE),
            )
        }
    }
}

@Composable
private fun PreferencesSection(state: SettingsUiState.Ready, actions: SettingsActions) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    var showCurrency by remember { mutableStateOf(false) }
    SectionHeader(title = stringResource(R.string.feature_settings_preferences))
    SubzeroCard(onClick = { showCurrency = true }, modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.CURRENCY)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.feature_settings_currency), style = SubzeroTheme.typography.body, color = colors.textPrimary)
                Text(text = stringResource(R.string.feature_settings_currency_body), style = SubzeroTheme.typography.caption, color = colors.textSecondary)
            }
            Spacer(Modifier.width(spacing.sm))
            Text(text = state.preferences.homeCurrency.code, style = SubzeroTheme.typography.money, color = colors.accent)
        }
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.feature_settings_appearance), style = SubzeroTheme.typography.body, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ThemeMode.entries.forEach { mode ->
                SubzeroChip(
                    text = mode.label(),
                    selected = state.preferences.themeMode == mode,
                    onClick = { actions.onTheme(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(SettingsTestTags.theme(mode)),
                )
            }
        }
    }
    if (showCurrency) {
        SubzeroCurrencySheet(
            selected = state.preferences.homeCurrency,
            onSelect = { actions.onCurrency(it); showCurrency = false },
            onDismiss = { showCurrency = false },
        )
    }
}

@Composable
private fun NotificationsSection(state: SettingsUiState.Ready, actions: SettingsActions) {
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
            onAction = actions.onRequestPermission,
            modifier = Modifier.testTag(SettingsTestTags.PERMISSION),
        )
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = stringResource(R.string.feature_settings_upcoming_title),
            body = stringResource(R.string.feature_settings_upcoming_body),
            checked = notifications.upcomingChargeEnabled,
            onChecked = actions.onUpcoming,
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
                            onClick = { actions.onDaysBefore(days) },
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
            onChecked = actions.onSummary,
            tag = SettingsTestTags.SUMMARY,
        )
    }
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = stringResource(R.string.feature_settings_savings_title),
            body = stringResource(R.string.feature_settings_savings_body),
            checked = notifications.savingsInsightsEnabled,
            onChecked = actions.onSavings,
            tag = SettingsTestTags.SAVINGS,
        )
    }
    if (state.anyNotificationEnabled && !state.notificationsAllowed) {
        SubzeroButton(
            text = stringResource(R.string.feature_settings_notifications_open_settings),
            onClick = actions.onOpenSystemSettings,
            style = SubzeroButtonStyle.Ghost,
            compact = true,
        )
    }
}

@Composable
private fun PrivacySection() {
    SectionHeader(title = stringResource(R.string.feature_settings_privacy))
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(SubzeroIcons.Lock, contentDescription = null, tint = SubzeroTheme.colors.positive)
            Spacer(Modifier.width(SubzeroTheme.spacing.sm))
            Text(
                text = stringResource(R.string.feature_settings_privacy_body),
                style = SubzeroTheme.typography.bodySmall,
                color = SubzeroTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DataSection(state: SettingsUiState.Ready, actions: SettingsActions) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    SectionHeader(title = stringResource(R.string.feature_settings_data))
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        val storage = state.storage
        if (storage != null) {
            Text(
                text = pluralStringResource(
                    R.plurals.feature_settings_storage_line,
                    storage.subscriptionCount,
                    storage.subscriptionCount,
                    Formatter.formatShortFileSize(context, storage.databaseBytes),
                ),
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
            )
            Spacer(Modifier.height(spacing.sm))
        }
        Text(text = stringResource(R.string.feature_settings_export), style = SubzeroTheme.typography.body, color = colors.textPrimary)
        Text(text = stringResource(R.string.feature_settings_export_body), style = SubzeroTheme.typography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SubzeroButton(
                text = stringResource(R.string.feature_settings_export_json),
                onClick = { actions.onExport(ExportFormat.JSON) },
                style = SubzeroButtonStyle.Secondary,
                compact = true,
                enabled = !state.isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag(SettingsTestTags.EXPORT_JSON),
            )
            SubzeroButton(
                text = stringResource(R.string.feature_settings_export_csv),
                onClick = { actions.onExport(ExportFormat.CSV) },
                style = SubzeroButtonStyle.Secondary,
                compact = true,
                enabled = !state.isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag(SettingsTestTags.EXPORT_CSV),
            )
        }
    }
    SubzeroButton(
        text = stringResource(R.string.feature_settings_delete_all),
        onClick = { confirmDelete = true },
        style = SubzeroButtonStyle.Danger,
        leadingIcon = SubzeroIcons.Delete,
        enabled = !state.isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsTestTags.DELETE),
    )
    if (confirmDelete) {
        SubzeroDialog(
            title = stringResource(R.string.feature_settings_delete_all_title),
            message = stringResource(R.string.feature_settings_delete_all_message),
            confirmLabel = stringResource(R.string.feature_settings_delete_all),
            destructive = true,
            onConfirm = { confirmDelete = false; actions.onDeleteAll() },
            onDismiss = { confirmDelete = false },
            modifier = Modifier.testTag(SettingsTestTags.CONFIRM_DELETE),
        )
    }
}

@Composable
private fun AboutSection(state: SettingsUiState.Ready) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    SectionHeader(title = stringResource(R.string.feature_settings_about))
    SubzeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            SubzeroWordmark(markSize = 48.dp)
            Spacer(Modifier.height(spacing.xs))
            Text(text = stringResource(R.string.feature_settings_version, state.appVersion), style = SubzeroTheme.typography.caption, color = colors.textTertiary)
            Text(text = stringResource(R.string.feature_settings_tagline), style = SubzeroTheme.typography.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = stringResource(R.string.feature_settings_licenses),
                style = SubzeroTheme.typography.caption,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
        ThemeMode.DARK -> R.string.feature_settings_theme_dark
        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
    },
)

private fun SettingsMessage.text(): Int = when (this) {
    SettingsMessage.EXPORTED -> R.string.feature_settings_message_exported
    SettingsMessage.EXPORT_FAILED -> R.string.feature_settings_message_export_failed
    SettingsMessage.DATA_DELETED -> R.string.feature_settings_message_deleted
    SettingsMessage.DELETE_FAILED -> R.string.feature_settings_message_delete_failed
}
