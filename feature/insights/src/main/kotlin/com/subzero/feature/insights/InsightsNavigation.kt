package com.subzero.feature.insights

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.InsightsKey
import com.subzero.core.navigation.NavTransitions

fun EntryProviderScope<NavKey>.insightsEntry(transitions: NavTransitions) {
    entry<InsightsKey>(metadata = transitions.tab) {
        InsightsRoute()
    }
}
