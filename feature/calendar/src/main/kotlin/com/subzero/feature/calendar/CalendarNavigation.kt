package com.subzero.feature.calendar

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.CalendarKey
import com.subzero.core.navigation.NavTransitions

fun EntryProviderScope<NavKey>.calendarEntry(transitions: NavTransitions) {
    entry<CalendarKey>(metadata = transitions.tab) {
        CalendarRoute()
    }
}
