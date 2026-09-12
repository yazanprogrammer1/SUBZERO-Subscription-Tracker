package com.subzero.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ManageSubscriptionUseCasesTest {

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()

    private val validDraft = SubscriptionDraft(
        name = "  Netflix ",
        price = usd(1549),
        billingCycle = BillingCycle.Monthly,
        nextPaymentDate = date("2026-09-16"),
        category = Category.ENTERTAINMENT,
        usage = DeclaredUsage.DAILY,
        notes = "  Family plan ",
    )

    @Test
    fun `add stores a trimmed subscription with its initial price change`() = runTest {
        val result = AddSubscriptionUseCase(repository, clock)(validDraft)

        val saved = (result as SaveSubscriptionResult.Saved).subscription
        assertThat(saved.name).isEqualTo("Netflix")
        assertThat(saved.notes).isEqualTo("Family plan")
        assertThat(saved.anchorDate).isEqualTo(date("2026-09-16"))
        assertThat(saved.nextBillingDate).isEqualTo(date("2026-09-16"))
        assertThat(saved.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(saved.createdAt).isEqualTo(clock.instant())
        assertThat(repository.subscriptionsSnapshot).containsExactly(saved)
        val price = repository.priceChangesSnapshot.single()
        assertThat(price.subscriptionId).isEqualTo(saved.id)
        assertThat(price.price).isEqualTo(usd(1549))
        assertThat(price.effectiveFrom).isEqualTo(date("2026-09-16"))
    }

    @Test
    fun `add with a first payment date anchors there and projects the next charge`() = runTest {
        val draft = validDraft.copy(firstPaymentDate = date("2024-01-31"), nextPaymentDate = date("2026-09-30"))
        val saved = (AddSubscriptionUseCase(repository, clock)(draft) as SaveSubscriptionResult.Saved).subscription
        assertThat(saved.anchorDate).isEqualTo(date("2024-01-31"))
        assertThat(saved.nextBillingDate).isEqualTo(date("2026-09-30"))
        assertThat(repository.priceChangesSnapshot.single().effectiveFrom).isEqualTo(date("2024-01-31"))
    }

    @Test
    fun `add with a past next payment date rolls the projection to the future`() = runTest {
        val draft = validDraft.copy(nextPaymentDate = date("2026-08-05"))
        val saved = (AddSubscriptionUseCase(repository, clock)(draft) as SaveSubscriptionResult.Saved).subscription
        assertThat(saved.anchorDate).isEqualTo(date("2026-08-05"))
        assertThat(saved.nextBillingDate).isEqualTo(date("2026-10-05"))
    }

    @Test
    fun `add rejects invalid drafts and stores nothing`() = runTest {
        val result = AddSubscriptionUseCase(repository, clock)(
            validDraft.copy(name = "   ", price = null, notes = "x".repeat(501), firstPaymentDate = date("2026-12-01")),
        )
        assertThat(result).isEqualTo(
            SaveSubscriptionResult.Invalid(
                setOf(
                    SubscriptionValidationError.NAME_BLANK,
                    SubscriptionValidationError.PRICE_MISSING,
                    SubscriptionValidationError.NOTES_TOO_LONG,
                    SubscriptionValidationError.FIRST_PAYMENT_AFTER_NEXT,
                ),
            ),
        )
        assertThat(repository.subscriptionsSnapshot).isEmpty()
    }

    @Test
    fun `name longer than the limit is rejected`() {
        val errors = SubscriptionValidator.validate(validDraft.copy(name = "n".repeat(81)))
        assertThat(errors).containsExactly(SubscriptionValidationError.NAME_TOO_LONG)
        assertThat(SubscriptionValidator.validate(validDraft.copy(name = "n".repeat(80)))).isEmpty()
    }

    @Test
    fun `zero price is a valid subscription`() = runTest {
        val result = AddSubscriptionUseCase(repository, clock)(validDraft.copy(price = usd(0)))
        assertThat(result).isInstanceOf(SaveSubscriptionResult.Saved::class.java)
    }

    @Test
    fun `update with a new price appends a price change effective today`() = runTest {
        val existing = subscription(price = usd(1399), anchorDate = date("2026-09-16"))
        repository.seed(existing)

        val result = UpdateSubscriptionUseCase(repository, clock)(existing.id, validDraft.copy(price = usd(1549)))

        val updated = (result as SaveSubscriptionResult.Saved).subscription
        assertThat(updated.price).isEqualTo(usd(1549))
        assertThat(updated.nextBillingDate).isEqualTo(existing.nextBillingDate)
        assertThat(updated.createdAt).isEqualTo(existing.createdAt)
        val change = repository.priceChangesSnapshot.single()
        assertThat(change.price).isEqualTo(usd(1549))
        assertThat(change.effectiveFrom).isEqualTo(today)
    }

    @Test
    fun `update without a price change records nothing in price history`() = runTest {
        val existing = subscription(price = usd(1549))
        repository.seed(existing)
        UpdateSubscriptionUseCase(repository, clock)(existing.id, validDraft.copy(name = "Netflix Premium"))
        assertThat(repository.priceChangesSnapshot).isEmpty()
        assertThat(repository.subscriptionsSnapshot.single().name).isEqualTo("Netflix Premium")
    }

    @Test
    fun `update with a new schedule re-projects the next billing date`() = runTest {
        val existing = subscription(anchorDate = date("2026-09-16"), nextBillingDate = date("2026-09-16"))
        repository.seed(existing)
        val result = UpdateSubscriptionUseCase(repository, clock)(
            existing.id,
            validDraft.copy(billingCycle = BillingCycle.Yearly, nextPaymentDate = date("2026-01-31")),
        )
        val updated = (result as SaveSubscriptionResult.Saved).subscription
        assertThat(updated.anchorDate).isEqualTo(date("2026-01-31"))
        assertThat(updated.nextBillingDate).isEqualTo(date("2027-01-31"))
    }

    @Test
    fun `update of an unknown subscription reports not found`() = runTest {
        val result = UpdateSubscriptionUseCase(repository, clock)(subscription(id = "ghost").id, validDraft)
        assertThat(result).isEqualTo(SaveSubscriptionResult.NotFound)
    }

    @Test
    fun `delete removes the subscription and its history`() = runTest {
        val existing = subscription()
        repository.seed(existing)
        repository.seedPrices(com.subzero.core.domain.testing.priceChange())
        repository.seedPayments(com.subzero.core.domain.testing.paymentRecord())

        DeleteSubscriptionUseCase(repository)(existing.id)

        assertThat(repository.subscriptionsSnapshot).isEmpty()
        assertThat(repository.priceChangesSnapshot).isEmpty()
        assertThat(repository.paymentsSnapshot).isEmpty()
    }

    @Test
    fun `pausing keeps the next billing date and stamps the status change`() = runTest {
        val existing = subscription(nextBillingDate = date("2026-09-16"))
        repository.seed(existing)

        val ok = SetSubscriptionStatusUseCase(repository, clock)(existing.id, SubscriptionStatus.PAUSED)

        assertThat(ok).isTrue()
        val paused = repository.subscriptionsSnapshot.single()
        assertThat(paused.status).isEqualTo(SubscriptionStatus.PAUSED)
        assertThat(paused.nextBillingDate).isEqualTo(date("2026-09-16"))
        assertThat(paused.statusChangedAt).isEqualTo(clock.instant())
    }

    @Test
    fun `resuming re-projects a stale next billing date from the anchor`() = runTest {
        val stale = subscription(
            anchorDate = date("2026-01-31"),
            nextBillingDate = date("2026-05-31"),
            status = SubscriptionStatus.PAUSED,
        )
        repository.seed(stale)

        SetSubscriptionStatusUseCase(repository, clock)(stale.id, SubscriptionStatus.ACTIVE)

        val resumed = repository.subscriptionsSnapshot.single()
        assertThat(resumed.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(resumed.nextBillingDate).isEqualTo(date("2026-09-30"))
    }

    @Test
    fun `setting the same status is a no-op that still succeeds`() = runTest {
        val existing = subscription()
        repository.seed(existing)
        assertThat(SetSubscriptionStatusUseCase(repository, clock)(existing.id, SubscriptionStatus.ACTIVE)).isTrue()
        assertThat(repository.subscriptionsSnapshot.single()).isEqualTo(existing)
    }

    @Test
    fun `status change on an unknown subscription returns false`() = runTest {
        assertThat(SetSubscriptionStatusUseCase(repository, clock)(subscription(id = "ghost").id, SubscriptionStatus.CANCELED)).isFalse()
    }

    @Test
    fun `declared usage can be changed`() = runTest {
        val existing = subscription(usage = DeclaredUsage.UNKNOWN)
        repository.seed(existing)
        SetDeclaredUsageUseCase(repository, clock)(existing.id, DeclaredUsage.RARELY)
        assertThat(repository.subscriptionsSnapshot.single().usage).isEqualTo(DeclaredUsage.RARELY)
    }
}
