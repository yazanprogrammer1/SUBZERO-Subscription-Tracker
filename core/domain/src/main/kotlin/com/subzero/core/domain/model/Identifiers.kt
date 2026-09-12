package com.subzero.core.domain.model

import java.util.UUID

@JvmInline
value class SubscriptionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SubscriptionId must not be blank" }
    }

    companion object {
        fun random(): SubscriptionId = SubscriptionId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class PriceChangeId(val value: String) {
    companion object {
        fun random(): PriceChangeId = PriceChangeId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class PaymentRecordId(val value: String) {
    companion object {
        fun random(): PaymentRecordId = PaymentRecordId(UUID.randomUUID().toString())
    }
}
