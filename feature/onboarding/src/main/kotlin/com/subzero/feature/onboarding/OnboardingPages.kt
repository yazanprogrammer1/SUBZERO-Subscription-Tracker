package com.subzero.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/** One onboarding page: copy plus the illustration composed beneath it. */
internal class OnboardingPageSpec(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val illustration: @Composable ColumnScope.(visible: Boolean) -> Unit,
)

internal val onboardingPages: List<OnboardingPageSpec> = listOf(
    OnboardingPageSpec(
        title = R.string.feature_onboarding_page1_title,
        body = R.string.feature_onboarding_page1_body,
        illustration = { visible -> WelcomeIllustration(visible) },
    ),
    OnboardingPageSpec(
        title = R.string.feature_onboarding_page2_title,
        body = R.string.feature_onboarding_page2_body,
        illustration = { visible -> SeeEverythingIllustration(visible) },
    ),
    OnboardingPageSpec(
        title = R.string.feature_onboarding_page3_title,
        body = R.string.feature_onboarding_page3_body,
        illustration = { visible -> PredictIllustration(visible) },
    ),
    OnboardingPageSpec(
        title = R.string.feature_onboarding_page4_title,
        body = R.string.feature_onboarding_page4_body,
        illustration = { visible -> SavingsIllustration(visible) },
    ),
    OnboardingPageSpec(
        title = R.string.feature_onboarding_page5_title,
        body = R.string.feature_onboarding_page5_body,
        illustration = { visible -> StartIllustration(visible) },
    ),
)
