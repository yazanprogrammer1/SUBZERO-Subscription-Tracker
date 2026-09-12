package com.subzero.feature.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.subzero.core.designsystem.component.SubzeroScreenPlaceholder
import com.subzero.core.navigation.OnboardingKey

/** Registers the Onboarding flow. The five-screen experience is built in Phase 4. */
fun EntryProviderScope<NavKey>.onboardingEntry() {
    entry<OnboardingKey> {
        SubzeroScreenPlaceholder(title = "Onboarding")
    }
}
