package com.subzero.feature.assistant

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.AssistantKey
import com.subzero.core.navigation.NavTransitions

fun EntryProviderScope<NavKey>.assistantEntry(transitions: NavTransitions) {
    entry<AssistantKey>(metadata = transitions.push) {
        AssistantRoute()
    }
}
