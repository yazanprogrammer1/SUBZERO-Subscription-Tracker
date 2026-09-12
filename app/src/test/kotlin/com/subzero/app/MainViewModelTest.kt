package com.subzero.app

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.usecase.RollForwardBillingDatesUseCase
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()
    private val subscriptions = FakeSubscriptionRepository()
    private val clock = fixedClock(date("2026-09-12"))

    private fun viewModel() = MainViewModel(
        preferences = preferences,
        rollForwardBillingDates = RollForwardBillingDatesUseCase(subscriptions, clock),
        applicationScope = kotlinx.coroutines.CoroutineScope(mainDispatcherRule.testDispatcher),
    )

    @Test
    fun `reflects onboarding and theme preferences`() = runTest {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value).isEqualTo(MainUiState.Loading)
        viewModel.uiState.test {
            // The unconfined test dispatcher collects eagerly, so Loading is conflated away here.
            assertThat(awaitItem()).isEqualTo(MainUiState.Ready(onboardingCompleted = false, themeMode = ThemeMode.SYSTEM))

            preferences.setOnboardingCompleted(true)
            assertThat(awaitItem()).isEqualTo(MainUiState.Ready(onboardingCompleted = true, themeMode = ThemeMode.SYSTEM))

            preferences.setThemeMode(ThemeMode.DARK)
            assertThat(awaitItem()).isEqualTo(MainUiState.Ready(onboardingCompleted = true, themeMode = ThemeMode.DARK))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rolls billing dates forward on launch`() = runTest {
        subscriptions.seed(subscription(anchorDate = date("2026-08-12"), nextBillingDate = date("2026-09-12")))

        viewModel()

        assertThat(subscriptions.paymentsSnapshot).hasSize(1)
        assertThat(subscriptions.subscriptionsSnapshot.single().nextBillingDate).isEqualTo(date("2026-10-12"))
    }
}
