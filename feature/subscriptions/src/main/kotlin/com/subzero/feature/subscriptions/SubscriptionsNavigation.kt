package com.subzero.feature.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.subzero.core.designsystem.component.LocalNavEntryAnimatedScope
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.navigation.SubscriptionsKey
import com.subzero.feature.subscriptions.detail.SubscriptionDetailRoute
import com.subzero.feature.subscriptions.form.SubscriptionFormRoute
import com.subzero.feature.subscriptions.list.SubscriptionsRoute

/** Registers list, detail and add/edit destinations. */
fun EntryProviderScope<NavKey>.subscriptionsEntries(transitions: NavTransitions) {
    entry<SubscriptionsKey>(metadata = transitions.tab) {
        WithNavAnimatedScope { SubscriptionsRoute() }
    }
    entry<SubscriptionDetailKey>(metadata = transitions.push) { key ->
        WithNavAnimatedScope { SubscriptionDetailRoute(key = key) }
    }
    entry<SubscriptionFormKey>(metadata = transitions.modal) { key ->
        SubscriptionFormRoute(key = key)
    }
}

/** Bridges the navigation animated scope into the null-safe local screens read for shared elements. */
@Composable
private fun WithNavAnimatedScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavEntryAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}
