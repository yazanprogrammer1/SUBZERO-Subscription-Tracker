package com.subzero.core.domain.model

enum class Category {
    ENTERTAINMENT,
    AI,
    CLOUD,
    SOFTWARE,
    FITNESS,
    EDUCATION,
    PRODUCTIVITY,
    GAMING,
    OTHER,
}

enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELED,
}

/**
 * How often the user says they use a service. Always user-declared; SUBZERO never infers usage
 * from device data.
 */
enum class DeclaredUsage {
    DAILY,
    WEEKLY,
    MONTHLY,
    RARELY,
    UNKNOWN,
}

/** Whether a payment record was observed by the app or back-filled from the billing schedule. */
enum class PaymentSource {
    RECORDED,
    ESTIMATED,
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}
