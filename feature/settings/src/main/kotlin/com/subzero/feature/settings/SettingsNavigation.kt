package com.subzero.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.SettingsKey

/** Registers the Settings destination. Settings screens are built in Phase 11. */
fun EntryProviderScope<NavKey>.settingsEntry() {
    entry<SettingsKey> {
        SubzeroScreenPlaceholder(title = "Settings")
    }
}
