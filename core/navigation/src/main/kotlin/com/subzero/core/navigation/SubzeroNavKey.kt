package com.subzero.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every navigable destination in SUBZERO.
 *
 * Sealed so the back stack can be serialized with closed polymorphism (process-death safe)
 * and so `when` over keys is exhaustive.
 */
@Serializable
sealed interface SubzeroNavKey : NavKey

/** Destinations reachable from the bottom navigation bar. Each owns its own back stack. */
@Serializable
sealed interface TopLevelKey : SubzeroNavKey

@Serializable
data object OnboardingKey : SubzeroNavKey

@Serializable
data object HomeKey : TopLevelKey

@Serializable
data object SubscriptionsKey : TopLevelKey

@Serializable
data object CalendarKey : TopLevelKey

@Serializable
data object InsightsKey : TopLevelKey

@Serializable
data object SettingsKey : TopLevelKey

@Serializable
data class SubscriptionDetailKey(val subscriptionId: String) : SubzeroNavKey

/** `subscriptionId == null` means "add new", otherwise "edit existing". */
@Serializable
data class SubscriptionFormKey(val subscriptionId: String? = null) : SubzeroNavKey

/** The conversational assistant, pushed over any tab. */
@Serializable
data object AssistantKey : SubzeroNavKey
