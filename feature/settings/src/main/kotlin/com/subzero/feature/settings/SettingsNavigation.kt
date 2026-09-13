package com.subzero.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.SettingsKey

fun EntryProviderScope<NavKey>.settingsEntry(transitions: NavTransitions) {
    entry<SettingsKey>(metadata = transitions.tab) {
        SettingsRoute()
    }
}
