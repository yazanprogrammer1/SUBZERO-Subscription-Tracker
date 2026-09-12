package com.subzero.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.HomeKey

/** Registers the Home destination. The dashboard itself is built in Phase 6. */
fun EntryProviderScope<NavKey>.homeEntry(transitions: NavTransitions) {
    entry<HomeKey>(metadata = transitions.tab) {
        SubzeroScreenPlaceholder(title = "Home")
    }
}
