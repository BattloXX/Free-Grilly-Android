package org.battlo.freegrilly

import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.DeviceFeatures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFeaturesTest {
    @Test
    fun `empty capabilities keeps legacy Free-Grilly opt-in features hidden`() {
        val features = DeviceFeatures.from(emptySet())

        assertFalse(features.calibration)
        assertFalse(features.longTermHistory)
        assertFalse(features.ota)
        assertFalse(features.events)
        assertFalse(features.clearHistory)
        assertFalse(features.diagnostics)
        assertFalse(features.alarmProbes)
    }

    @Test
    fun `normalizes capability aliases before deriving features`() {
        val features = DeviceFeatures.from(setOf("probe_calibration", "ota_upload", "sse", Capabilities.DIAGNOSTICS))

        assertTrue(features.calibration)
        assertTrue(features.ota)
        assertTrue(features.events)
        assertTrue(features.diagnostics)
    }

    @Test
    fun `only explicit feature flags are enabled`() {
        val features = DeviceFeatures.from(setOf(
            Capabilities.HISTORY, Capabilities.CLEAR_HISTORY, Capabilities.ALARM_PROBES,
        ))

        assertTrue(features.longTermHistory)
        assertTrue(features.clearHistory)
        assertTrue(features.alarmProbes)
        assertFalse(features.calibration)
    }
}
