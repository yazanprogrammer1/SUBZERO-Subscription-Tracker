package com.subzero.feature.calendar

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.CalendarKey

/** Registers the Calendar destination. The calendar is built in Phase 7. */
fun EntryProviderScope<NavKey>.calendarEntry(transitions: NavTransitions) {
    entry<CalendarKey>(metadata = transitions.tab) {
        SubzeroScreenPlaceholder(title = "Calendar")
    }
}
