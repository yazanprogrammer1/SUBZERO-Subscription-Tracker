package com.subzero.core.domain.usecase

import com.subzero.core.domain.billing.BillingSchedule
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.UpcomingPayment
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Every projected charge for ACTIVE subscriptions inside a date window, soonest first.
 * A subscription that charges weekly appears once per week in the window.
 */
class GetUpcomingPaymentsUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(
        subscriptions: List<Subscription>,
        from: LocalDate = LocalDate.now(clock),
        to: LocalDate = from.plusDays(DEFAULT_WINDOW_DAYS),
    ): List<UpcomingPayment> =
        subscriptions.asSequence()
            .filter { it.isActive }
            .flatMap { subscription ->
                BillingSchedule.chargesBetween(subscription.anchorDate, subscription.billingCycle, from, to)
                    .map { date -> UpcomingPayment(subscription, date, subscription.price) }
            }
            .sortedWith(compareBy({ it.date }, { it.subscription.name.lowercase() }))
            .toList()

    /** The single next charge across all subscriptions, or null when nothing is scheduled. */
    fun next(subscriptions: List<Subscription>, from: LocalDate = LocalDate.now(clock)): UpcomingPayment? =
        subscriptions.asSequence()
            .filter { it.isActive }
            .map { subscription ->
                val date = BillingSchedule.nextChargeOnOrAfter(subscription.anchorDate, subscription.billingCycle, from)
                UpcomingPayment(subscription, date, subscription.price)
            }
            .minWithOrNull(compareBy({ it.date }, { it.subscription.name.lowercase() }))

    companion object {
        const val DEFAULT_WINDOW_DAYS = 30L
    }
}
