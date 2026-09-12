package com.subzero.core.domain.testing

import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.Money
import com.subzero.core.domain.model.PaymentRecord
import com.subzero.core.domain.model.PaymentRecordId
import com.subzero.core.domain.model.PaymentSource
import com.subzero.core.domain.model.PriceChange
import com.subzero.core.domain.model.PriceChangeId
import com.subzero.core.domain.model.Subscription
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.model.SubscriptionStatus
import java.time.Instant
import java.time.LocalDate

fun usd(minor: Long): Money = Money(minor, CurrencyCode.USD)
fun eur(minor: Long): Money = Money(minor, CurrencyCode.EUR)
fun jpy(minor: Long): Money = Money(minor, CurrencyCode("JPY"))

/** A realistic subscription with sensible defaults; override only what the test cares about. */
fun subscription(
    id: String = "sub-1",
    name: String = "Netflix",
    price: Money = usd(1549),
    billingCycle: BillingCycle = BillingCycle.Monthly,
    anchorDate: LocalDate = date("2026-09-16"),
    nextBillingDate: LocalDate = anchorDate,
    category: Category = Category.ENTERTAINMENT,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    usage: DeclaredUsage = DeclaredUsage.UNKNOWN,
    notes: String? = null,
    createdAt: Instant = Instant.parse("2026-09-01T10:00:00Z"),
    updatedAt: Instant = createdAt,
    statusChangedAt: Instant? = null,
): Subscription = Subscription(
    id = SubscriptionId(id),
    name = name,
    price = price,
    billingCycle = billingCycle,
    anchorDate = anchorDate,
    nextBillingDate = nextBillingDate,
    category = category,
    status = status,
    usage = usage,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    statusChangedAt = statusChangedAt,
)

fun priceChange(
    subscriptionId: String = "sub-1",
    price: Money = usd(1549),
    effectiveFrom: LocalDate = date("2026-09-16"),
    id: String = "price-$subscriptionId-$effectiveFrom",
): PriceChange = PriceChange(PriceChangeId(id), SubscriptionId(subscriptionId), price, effectiveFrom)

fun paymentRecord(
    subscriptionId: String = "sub-1",
    amount: Money = usd(1549),
    paidOn: LocalDate = date("2026-09-16"),
    source: PaymentSource = PaymentSource.RECORDED,
    id: String = "pay-$subscriptionId-$paidOn",
): PaymentRecord = PaymentRecord(PaymentRecordId(id), SubscriptionId(subscriptionId), amount, paidOn, source)
