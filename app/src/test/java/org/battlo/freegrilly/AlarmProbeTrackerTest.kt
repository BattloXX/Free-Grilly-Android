package org.battlo.freegrilly

import org.battlo.freegrilly.domain.AlarmProbe
import org.battlo.freegrilly.domain.AlarmProbeTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmProbeTrackerTest {
    @Test
    fun `tracks each alarming probe and clears only probes that stop`() {
        val tracker = AlarmProbeTracker()
        assertEquals(setOf(1, 2), tracker.update(true, listOf(AlarmProbe(1, true), AlarmProbe(2, true))).started)

        val change = tracker.update(true, listOf(AlarmProbe(1, true), AlarmProbe(2, false)))
        assertEquals(emptySet<Int>(), change.started)
        assertEquals(setOf(2), change.stopped)
    }

    @Test
    fun `probe is rearmed after firmware reports it stopped`() {
        val tracker = AlarmProbeTracker()
        tracker.update(true, listOf(AlarmProbe(7, true)))
        assertEquals(setOf(7), tracker.update(false, listOf(AlarmProbe(7, false))).stopped)
        assertEquals(setOf(7), tracker.update(true, listOf(AlarmProbe(7, true))).started)
    }

    @Test
    fun `does not guess a probe when only alarm sounding is available`() {
        val change = AlarmProbeTracker().update(true, listOf(AlarmProbe(3, false), AlarmProbe(4, false)))
        assertEquals(emptySet<Int>(), change.started)
    }
}
