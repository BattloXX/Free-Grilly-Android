package org.battlo.freegrilly.ui.onboarding

/**
 * Bounded NSD retry schedule used after the device leaves its setup access point.
 * Keeping it pure makes the timing contract easy to verify without Android services.
 */
object OnboardingDiscoveryPolicy {
    const val ATTEMPTS = 12
    const val DISCOVERY_WINDOW_MS = 4_000L
    const val BETWEEN_ATTEMPTS_MS = 1_000L

    fun shouldRetry(attempt: Int): Boolean = attempt < ATTEMPTS - 1

    fun totalBudgetMs(): Long = ATTEMPTS * DISCOVERY_WINDOW_MS +
        (ATTEMPTS - 1) * BETWEEN_ATTEMPTS_MS
}
