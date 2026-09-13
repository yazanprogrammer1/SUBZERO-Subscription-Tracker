package com.subzero.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.HomeKey
import com.subzero.core.navigation.NavTransitions

fun EntryProviderScope<NavKey>.homeEntry(transitions: NavTransitions) {
    entry<HomeKey>(metadata = transitions.tab) {
        HomeRoute()
    }
}
