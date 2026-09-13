package com.subzero.feature.subscriptions.form

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.CycleUnit
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.NotificationPreferences
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.domain.model.UserPreferences
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.priceChange
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.AddSubscriptionUseCase
import com.subzero.core.domain.usecase.SubscriptionValidationError
import com.subzero.core.domain.usecase.UpdateSubscriptionUseCase
import com.subzero.core.navigation.SubscriptionFormKey
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SubscriptionFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()
    private val preferences = FakeUserPreferencesRepository(
        UserPreferences(CurrencyCode.EUR, ThemeMode.SYSTEM, NotificationPreferences.Default, true, null),
    )

    private fun viewModel(id: String? = null) = SubscriptionFormViewModel(
        key = SubscriptionFormKey(id),
        repository = repository,
        preferences = preferences,
        addSubscription = AddSubscriptionUseCase(repository, clock),
        updateSubscription = UpdateSubscriptionUseCase(repository, clock),
        clock = clock,
    )

    @Test
    fun `add form starts with the home currency and today as next payment`() = runTest {
        val state = viewModel().state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isEdit).isFalse()
        assertThat(state.currency).isEqualTo(CurrencyCode.EUR)
        assertThat(state.nextPaymentDate).isEqualTo(today)
        assertThat(state.cycle).isEqualTo(BillingCycle.Monthly)
    }

    @Test
    fun `saving a valid draft stores the subscription and reports it`() = runTest {
        val vm = viewModel()
        vm.setName("Netflix")
        vm.setPriceText("15,49")
        vm.setStandardCycle(BillingCycle.Monthly)
        vm.setNextPaymentDate(date("2026-09-16"))
        vm.setCategory(Category.ENTERTAINMENT)
        vm.setUsage(DeclaredUsage.DAILY)

        vm.save()

        val saved = vm.state.value.saved
        assertThat(saved).isNotNull()
        assertThat(saved!!.price.amountMinor).isEqualTo(1549)
        assertThat(saved.price.currency).isEqualTo(CurrencyCode.EUR)
        assertThat(saved.nextBillingDate).isEqualTo(date("2026-09-16"))
        assertThat(repository.subscriptionsSnapshot).hasSize(1)
    }

    @Test
    fun `invalid input surfaces field errors and stores nothing`() = runTest {
        val vm = viewModel()
        vm.save()
        assertThat(vm.state.value.errors).containsAtLeast(
            SubscriptionValidationError.NAME_BLANK,
            SubscriptionValidationError.PRICE_MISSING,
        )
        assertThat(repository.subscriptionsSnapshot).isEmpty()

        vm.setName("Netflix")
        assertThat(vm.state.value.errors).doesNotContain(SubscriptionValidationError.NAME_BLANK)
    }

    @Test
    fun `unparseable price is flagged without a round trip`() = runTest {
        val vm = viewModel()
        vm.setName("Netflix")
        vm.setPriceText("12.345")
        vm.save()
        assertThat(vm.state.value.priceInvalid).isTrue()
        assertThat(vm.state.value.saved).isNull()

        vm.setPriceText("12.34")
        assertThat(vm.state.value.priceInvalid).isFalse()
    }

    @Test
    fun `custom cycle uses the editor values`() = runTest {
        val vm = viewModel()
        vm.setCustomMode(true)
        vm.setCustomEvery(6)
        vm.setCustomUnit(CycleUnit.MONTH)
        assertThat(vm.state.value.cycle).isEqualTo(BillingCycle.Custom(6, CycleUnit.MONTH))

        vm.setCustomEvery(1)
        assertThat(vm.state.value.cycle).isEqualTo(BillingCycle.Monthly)
        assertThat(vm.state.value.customMode).isTrue()

        vm.setStandardCycle(BillingCycle.Yearly)
        assertThat(vm.state.value.cycle).isEqualTo(BillingCycle.Yearly)
        assertThat(vm.state.value.customMode).isFalse()
    }

    @Test
    fun `projection appears when the first payment date changes the schedule`() = runTest {
        val vm = viewModel()
        vm.setNextPaymentDate(date("2026-09-30"))
        assertThat(vm.state.value.showsProjection).isFalse()

        vm.setFirstPaymentDate(date("2024-01-15"))
        assertThat(vm.state.value.projectedNextDate).isEqualTo(date("2026-09-15"))
        assertThat(vm.state.value.showsProjection).isTrue()
    }

    @Test
    fun `edit form loads the existing subscription and updates it`() = runTest {
        val existing = subscription(
            id = "e",
            name = "Spotify",
            price = usd(1199),
            anchorDate = date("2026-01-24"),
            nextBillingDate = date("2026-09-24"),
        )
        repository.seed(existing)
        repository.seedPrices(priceChange(subscriptionId = "e", price = usd(1199), effectiveFrom = date("2026-01-24")))

        val vm = viewModel(id = "e")
        val loaded = vm.state.value
        assertThat(loaded.isEdit).isTrue()
        assertThat(loaded.name).isEqualTo("Spotify")
        assertThat(loaded.priceText).isEqualTo("11.99")
        assertThat(loaded.firstPaymentDate).isEqualTo(date("2026-01-24"))

        vm.setPriceText("12.99")
        vm.save()

        assertThat(vm.state.value.saved?.price).isEqualTo(usd(1299))
        assertThat(repository.priceChangesSnapshot).hasSize(2)
    }

    @Test
    fun `editing a missing subscription reports not found`() = runTest {
        val state = viewModel(id = "ghost").state.value
        assertThat(state.failure).isEqualTo(FormFailure.NOT_FOUND)
        assertThat(state.canSave).isFalse()
    }
}
