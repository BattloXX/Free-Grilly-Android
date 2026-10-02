package org.battlo.freegrilly

import org.battlo.freegrilly.ui.onboarding.OnboardingDiscoveryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDiscoveryPolicyTest {
    @Test fun `retry policy remains bounded to about one minute`() {
        assertEquals(12, OnboardingDiscoveryPolicy.ATTEMPTS)
        assertEquals(59_000L, OnboardingDiscoveryPolicy.totalBudgetMs())
    }

    @Test fun `only attempts before the final one retry`() {
        assertTrue(OnboardingDiscoveryPolicy.shouldRetry(0))
        assertTrue(OnboardingDiscoveryPolicy.shouldRetry(OnboardingDiscoveryPolicy.ATTEMPTS - 2))
        assertFalse(OnboardingDiscoveryPolicy.shouldRetry(OnboardingDiscoveryPolicy.ATTEMPTS - 1))
    }
}
