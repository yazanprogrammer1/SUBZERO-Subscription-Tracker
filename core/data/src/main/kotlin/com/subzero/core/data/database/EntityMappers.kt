package com.subzero.core.data.database

import com.subzero.core.data.database.entity.PaymentRecordEntity
import com.subzero.core.data.database.entity.PriceChangeEntity
import com.subzero.core.data.database.entity.SubscriptionEntity
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.CycleUnit
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

internal fun Subscription.toEntity() = SubscriptionEntity(
    id = id.value,
    name = name,
    amountMinor = price.amountMinor,
    currency = price.currency.code,
    cycleEvery = billingCycle.every,
    cycleUnit = billingCycle.unit.name,
    anchorDate = anchorDate,
    nextBillingDate = nextBillingDate,
    category = category.name,
    status = status.name,
    usage = usage.name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    statusChangedAt = statusChangedAt,
)

internal fun SubscriptionEntity.toDomain() = Subscription(
    id = SubscriptionId(id),
    name = name,
    price = Money(amountMinor, CurrencyCode(currency)),
    billingCycle = BillingCycle.of(cycleEvery, CycleUnit.valueOf(cycleUnit)),
    anchorDate = anchorDate,
    nextBillingDate = nextBillingDate,
    category = enumOrDefault(category, Category.OTHER),
    status = enumOrDefault(status, SubscriptionStatus.ACTIVE),
    usage = enumOrDefault(usage, DeclaredUsage.UNKNOWN),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    statusChangedAt = statusChangedAt,
)

internal fun PriceChange.toEntity() = PriceChangeEntity(
    id = id.value,
    subscriptionId = subscriptionId.value,
    amountMinor = price.amountMinor,
    currency = price.currency.code,
    effectiveFrom = effectiveFrom,
)

internal fun PriceChangeEntity.toDomain() = PriceChange(
    id = PriceChangeId(id),
    subscriptionId = SubscriptionId(subscriptionId),
    price = Money(amountMinor, CurrencyCode(currency)),
    effectiveFrom = effectiveFrom,
)

internal fun PaymentRecord.toEntity() = PaymentRecordEntity(
    id = id.value,
    subscriptionId = subscriptionId.value,
    amountMinor = amount.amountMinor,
    currency = amount.currency.code,
    paidOn = paidOn,
    source = source.name,
)

internal fun PaymentRecordEntity.toDomain() = PaymentRecord(
    id = PaymentRecordId(id),
    subscriptionId = SubscriptionId(subscriptionId),
    amount = Money(amountMinor, CurrencyCode(currency)),
    paidOn = paidOn,
    source = enumOrDefault(source, PaymentSource.RECORDED),
)

/** Unknown enum names (e.g. after a downgrade from a newer app) fall back instead of crashing. */
private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
