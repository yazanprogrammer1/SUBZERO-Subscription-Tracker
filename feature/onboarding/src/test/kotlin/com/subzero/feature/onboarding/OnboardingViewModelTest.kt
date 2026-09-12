package com.subzero.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()
    private val viewModel = OnboardingViewModel(preferences)

    @Test
    fun `completing persists the flag before reporting the outcome`() = runTest {
        viewModel.completed.test {
            assertThat(awaitItem()).isNull()

            viewModel.complete(OnboardingOutcome.AddFirstSubscription)

            assertThat(awaitItem()).isEqualTo(OnboardingOutcome.AddFirstSubscription)
            assertThat(preferences.preferences.first().onboardingCompleted).isTrue()
        }
    }

    @Test
    fun `skipping also completes onboarding`() = runTest {
        viewModel.complete(OnboardingOutcome.Skipped)
        assertThat(viewModel.completed.value).isEqualTo(OnboardingOutcome.Skipped)
        assertThat(preferences.preferences.first().onboardingCompleted).isTrue()
    }
}
