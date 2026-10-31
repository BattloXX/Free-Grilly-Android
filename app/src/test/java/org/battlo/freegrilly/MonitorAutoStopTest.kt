package org.battlo.freegrilly

import org.battlo.freegrilly.service.MonitorAutoStop
import org.junit.Assert.*
import org.junit.Test

class MonitorAutoStopTest {
    @Test
    fun disconnectedBeforeTimeoutKeepsMonitoring() {
        val state = MonitorAutoStop.update(MonitorAutoStop.State(), 100L, true, false)
        assertFalse(MonitorAutoStop.shouldStop(state, 100L + MonitorAutoStop.TIMEOUT_MS - 1))
    }

    @Test
    fun disconnectedAtTimeoutStopsMonitoring() {
        val state = MonitorAutoStop.update(MonitorAutoStop.State(), 100L, true, false)
        assertTrue(MonitorAutoStop.shouldStop(state, 100L + MonitorAutoStop.TIMEOUT_MS))
        assertTrue(MonitorAutoStop.shouldStop(state, 101L + MonitorAutoStop.TIMEOUT_MS))
    }

    @Test
    fun connectedResetsDisconnectionTimer() {
        val disconnected = MonitorAutoStop.update(MonitorAutoStop.State(), 0L, true, false)
        val connected = MonitorAutoStop.update(disconnected, 500L, false, true)
        assertNull(connected.disconnectedSince)
        assertFalse(MonitorAutoStop.shouldStop(connected, MonitorAutoStop.TIMEOUT_MS))
        val disconnectedAgain = MonitorAutoStop.update(connected, MonitorAutoStop.TIMEOUT_MS, true, false)
        assertFalse(MonitorAutoStop.shouldStop(disconnectedAgain, MonitorAutoStop.TIMEOUT_MS + 1))
    }

    @Test
    fun connectedWithoutProbesStopsAtTimeout() {
        val state = MonitorAutoStop.update(MonitorAutoStop.State(), 0L, false, false)
        assertFalse(MonitorAutoStop.shouldStop(state, MonitorAutoStop.TIMEOUT_MS - 1))
        assertTrue(MonitorAutoStop.shouldStop(state, MonitorAutoStop.TIMEOUT_MS))
    }

    @Test
    fun connectedProbeResetsNoProbeTimer() {
        val empty = MonitorAutoStop.update(MonitorAutoStop.State(), 0L, false, false)
        val withProbe = MonitorAutoStop.update(empty, 500L, false, true)
        assertNull(withProbe.noProbesSince)
        assertFalse(MonitorAutoStop.shouldStop(withProbe, MonitorAutoStop.TIMEOUT_MS))
    }

    @Test
    fun repeatedStatesDoNotRestartTimers() {
        val first = MonitorAutoStop.update(MonitorAutoStop.State(), 0L, true, false)
        val repeated = MonitorAutoStop.update(first, MonitorAutoStop.TIMEOUT_MS, true, false)
        assertTrue(MonitorAutoStop.shouldStop(repeated, MonitorAutoStop.TIMEOUT_MS))
    }

    @Test
    fun noProbeTimerSurvivesConnectionChanges() {
        val empty = MonitorAutoStop.update(MonitorAutoStop.State(), 0L, false, false)
        val disconnected = MonitorAutoStop.update(empty, 500L, true, false)
        val reconnected = MonitorAutoStop.update(disconnected, 1_000L, false, false)
        assertTrue(MonitorAutoStop.shouldStop(reconnected, MonitorAutoStop.TIMEOUT_MS))
    }
}
