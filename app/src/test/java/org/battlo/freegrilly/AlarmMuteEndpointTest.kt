package org.battlo.freegrilly

import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.device.AlarmMuteEndpoint
import org.battlo.freegrilly.data.device.alarmMuteEndpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmMuteEndpointTest {
    @Test
    fun `uses per-probe endpoint only when explicitly supported and probe is known`() {
        assertEquals(
            AlarmMuteEndpoint.PROBE,
            alarmMuteEndpoint(setOf(Capabilities.ALARM_PROBE_MUTE), 2),
        )
    }

    @Test
    fun `falls back to global endpoint without capability or probe id`() {
        assertEquals(AlarmMuteEndpoint.GLOBAL, alarmMuteEndpoint(emptySet(), 2))
        assertEquals(AlarmMuteEndpoint.GLOBAL, alarmMuteEndpoint(setOf(Capabilities.ALARM_PROBE_MUTE), null))
    }
}
