package com.subzero.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.subzero.app.MainUiState
import com.subzero.app.R
import com.subzero.app.navigation.SubzeroNavigator
import com.subzero.app.navigation.rememberTopLevelBackStack
import com.subzero.core.designsystem.component.LocalSharedTransitionScope
import com.subzero.core.designsystem.component.SubzeroBottomBar
import com.subzero.core.designsystem.component.SubzeroBottomBarItem
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.navigation.CalendarKey
import com.subzero.core.navigation.HomeKey
import com.subzero.core.navigation.InsightsKey
import com.subzero.core.navigation.LocalNavigator
import com.subzero.core.navigation.SettingsKey
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.navigation.SubscriptionsKey
import com.subzero.core.navigation.TopLevelKey
import com.subzero.feature.assistant.assistantEntry
import com.subzero.feature.calendar.calendarEntry
import com.subzero.feature.home.homeEntry
import com.subzero.feature.insights.insightsEntry
import com.subzero.feature.onboarding.OnboardingOutcome
import com.subzero.feature.onboarding.OnboardingRoute
import com.subzero.feature.onboarding.onboardingEntry
import com.subzero.feature.settings.settingsEntry
import com.subzero.feature.subscriptions.subscriptionsEntries

/**
 * Root of the UI. Shows onboarding until it has been completed once, then the main shell
 * (Navigation 3 display + bottom navigation). Switching between them fades through.
 */
@Composable
fun SubzeroApp(
    uiState: MainUiState,
    pendingSubscriptionId: String? = null,
    onPendingSubscriptionOpened: () -> Unit = {},
) {
    // Set when the user chose "Add your first subscription"; consumed by the shell once it exists.
    var pendingAddSubscription by rememberSaveable { mutableStateOf(false) }
    val motion = SubzeroTheme.motion

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SubzeroTheme.colors.background),
    ) {
        AnimatedContent(
            targetState = uiState,
            contentKey = { (it as? MainUiState.Ready)?.onboardingCompleted },
            transitionSpec = { fadeIn(motion.emphasizedEnterSpec()) togetherWith fadeOut(motion.emphasizedExitSpec()) },
            label = "root",
        ) { state ->
            when (state) {
                MainUiState.Loading -> Box(Modifier.fillMaxSize())
                is MainUiState.Ready -> if (state.onboardingCompleted) {
                    MainShell(
                        openAddSubscription = pendingAddSubscription,
                        onAddSubscriptionOpened = { pendingAddSubscription = false },
                        openSubscriptionId = pendingSubscriptionId,
                        onSubscriptionOpened = onPendingSubscriptionOpened,
                    )
                } else {
                    OnboardingRoute(
                        onFinished = { outcome ->
                            pendingAddSubscription = outcome == OnboardingOutcome.AddFirstSubscription
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainShell(
    openAddSubscription: Boolean,
    onAddSubscriptionOpened: () -> Unit,
    openSubscriptionId: String?,
    onSubscriptionOpened: () -> Unit,
) {
    val topLevelBackStack = rememberTopLevelBackStack(startKey = HomeKey)
    val navigator = remember(topLevelBackStack) { SubzeroNavigator(topLevelBackStack) }
    val showBottomBar = topLevelBackStack.currentKey is TopLevelKey
    val transitions = rememberNavTransitions()

    LaunchedEffect(openAddSubscription) {
        if (openAddSubscription) {
            navigator.navigate(SubscriptionFormKey())
            onAddSubscriptionOpened()
        }
    }
    LaunchedEffect(openSubscriptionId) {
        if (openSubscriptionId != null) {
            navigator.switchTab(SubscriptionsKey)
            navigator.navigate(SubscriptionDetailKey(openSubscriptionId))
            onSubscriptionOpened()
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Column(modifier = Modifier.fillMaxSize()) {
            SharedTransitionLayout(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavDisplay(
                        backStack = topLevelBackStack.backStack,
                        onBack = { topLevelBackStack.pop() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        sharedTransitionScope = this,
                        entryProvider = entryProvider {
                            onboardingEntry(transitions, onFinished = { topLevelBackStack.pop() })
                            homeEntry(transitions)
                            subscriptionsEntries(transitions)
                            calendarEntry(transitions)
                            insightsEntry(transitions)
                            settingsEntry(transitions)
                            assistantEntry(transitions)
                        },
                    )
                }
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
