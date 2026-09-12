package com.subzero.feature.subscriptions

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.SubscriptionDetailKey
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.navigation.SubscriptionsKey

/** Registers list, detail and add/edit destinations. Screens are built in Phase 5. */
fun EntryProviderScope<NavKey>.subscriptionsEntries(transitions: NavTransitions) {
    entry<SubscriptionsKey>(metadata = transitions.tab) {
        SubzeroScreenPlaceholder(title = "Subscriptions")
    }
    entry<SubscriptionDetailKey>(metadata = transitions.push) { key ->
        SubzeroScreenPlaceholder(title = "Subscription ${key.subscriptionId}")
    }
    entry<SubscriptionFormKey>(metadata = transitions.modal) { key ->
        SubzeroScreenPlaceholder(
            title = if (key.subscriptionId == null) "Add subscription" else "Edit subscription",
        )
    }
}
