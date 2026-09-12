package com.subzero.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.subzero.app.R
import com.subzero.app.navigation.SubzeroNavigator
import com.subzero.app.navigation.rememberTopLevelBackStack
import com.subzero.core.designsystem.component.SubzeroBottomBar
import com.subzero.core.designsystem.component.SubzeroBottomBarItem
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.navigation.CalendarKey
import com.subzero.core.navigation.HomeKey
import com.subzero.core.navigation.InsightsKey
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SettingsKey
import com.subzero.core.navigation.SubscriptionsKey
import com.subzero.core.navigation.TopLevelKey
import com.subzero.feature.calendar.calendarEntry
import com.subzero.feature.home.homeEntry
import com.subzero.feature.insights.insightsEntry
import com.subzero.feature.onboarding.onboardingEntry
import com.subzero.feature.settings.settingsEntry
import com.subzero.feature.subscriptions.subscriptionsEntries

/**
 * The app shell: Navigation 3 display + bottom navigation.
 *
 * Onboarding gating (first launch -> OnboardingKey) is wired in Phase 4 once preferences exist.
 */
@Composable
fun SubzeroApp() {
    val topLevelBackStack = rememberTopLevelBackStack(startKey = HomeKey)
    val navigator = remember(topLevelBackStack) { SubzeroNavigator(topLevelBackStack) }
    val showBottomBar = topLevelBackStack.currentKey is TopLevelKey
    val transitions = rememberNavTransitions()

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SubzeroTheme.colors.background),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = topLevelBackStack.backStack,
                    onBack = { topLevelBackStack.pop() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        onboardingEntry(transitions)
                        homeEntry(transitions)
                        subscriptionsEntries(transitions)
                        calendarEntry(transitions)
                        insightsEntry(transitions)
                        settingsEntry(transitions)
                    },
                )
            }
            if (showBottomBar) {
                SubzeroBottomBar(
                    items = listOf(
                        SubzeroBottomBarItem(
                            label = stringResource(R.string.nav_home),
                            icon = SubzeroIcons.Home,
                            selected = topLevelBackStack.topLevelKey == HomeKey,
                            onClick = { navigator.switchTab(HomeKey) },
                        ),
                        SubzeroBottomBarItem(
                            label = stringResource(R.string.nav_subscriptions),
                            icon = SubzeroIcons.Subscriptions,
                            selected = topLevelBackStack.topLevelKey == SubscriptionsKey,
                            onClick = { navigator.switchTab(SubscriptionsKey) },
                        ),
                        SubzeroBottomBarItem(
                            label = stringResource(R.string.nav_calendar),
                            icon = SubzeroIcons.Calendar,
                            selected = topLevelBackStack.topLevelKey == CalendarKey,
                            onClick = { navigator.switchTab(CalendarKey) },
                        ),
                        SubzeroBottomBarItem(
                            label = stringResource(R.string.nav_insights),
                            icon = SubzeroIcons.Insights,
                            selected = topLevelBackStack.topLevelKey == InsightsKey,
                            onClick = { navigator.switchTab(InsightsKey) },
                        ),
                        SubzeroBottomBarItem(
                            label = stringResource(R.string.nav_settings),
                            icon = SubzeroIcons.Settings,
                            selected = topLevelBackStack.topLevelKey == SettingsKey,
                            onClick = { navigator.switchTab(SettingsKey) },
                        ),
                    ),
                )
            }
        }
    }
}
