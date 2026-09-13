package com.subzero.core.domain.model

data class UserPreferences(
    /** Currency the dashboard aggregates in. Subscriptions in other currencies are counted, not summed. */
    val homeCurrency: CurrencyCode,
    val themeMode: ThemeMode,
    val notifications: NotificationPreferences,
    val onboardingCompleted: Boolean,
    val displayName: String?,
    /** Opt-in: send an anonymized subscription summary to a hosted model for richer answers. Off by default. */
    val aiEnhancedEnabled: Boolean = false,
)

data class NotificationPreferences(
    val upcomingChargeEnabled: Boolean,
    /** How many days before a charge to remind the user (1 = the day before). */
    val upcomingChargeDaysBefore: Int,
    val monthlySummaryEnabled: Boolean,
    val savingsInsightsEnabled: Boolean,
) {
    companion object {
        /** Everything off until the user opts in; SUBZERO never notifies without consent. */
        val Default = NotificationPreferences(
            upcomingChargeEnabled = false,
            upcomingChargeDaysBefore = 1,
            monthlySummaryEnabled = false,
            savingsInsightsEnabled = false,
        )
    }
}
