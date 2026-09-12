package com.subzero.feature.insights

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.InsightsKey

/** Registers the Insights destination. The insights engine and screen are built in Phase 8. */
fun EntryProviderScope<NavKey>.insightsEntry() {
    entry<InsightsKey> {
        SubzeroScreenPlaceholder(title = "Insights")
    }
}
