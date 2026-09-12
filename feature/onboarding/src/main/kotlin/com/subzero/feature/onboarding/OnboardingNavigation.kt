package com.subzero.feature.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.NavTransitions
import com.subzero.core.navigation.OnboardingKey

/**
 * Registers the onboarding destination for hosts that show it inside a NavDisplay.
 * The app shell renders [OnboardingRoute] directly before the main shell exists, so this
 * entry only matters if onboarding is ever re-entered from Settings.
 */
fun EntryProviderScope<NavKey>.onboardingEntry(
    transitions: NavTransitions,
    onFinished: (OnboardingOutcome) -> Unit,
) {
    entry<OnboardingKey>(metadata = transitions.tab) {
        OnboardingRoute(onFinished = onFinished)
    }
}
