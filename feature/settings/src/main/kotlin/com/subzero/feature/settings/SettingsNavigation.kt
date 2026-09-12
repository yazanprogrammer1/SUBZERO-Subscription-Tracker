package com.subzero.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.SettingsKey

/** Registers the Settings destination. Settings screens are built in Phase 11. */
fun EntryProviderScope<NavKey>.settingsEntry(transitions: NavTransitions) {
    entry<SettingsKey>(metadata = transitions.tab) {
        SubzeroScreenPlaceholder(title = "Settings")
    }
}
