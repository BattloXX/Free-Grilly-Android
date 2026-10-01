package org.battlo.freegrilly

import org.battlo.freegrilly.data.DiscoveredDevice
import org.battlo.freegrilly.data.DiscoveryDedupe
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryDedupeTest {
    @Test fun `same confirmed UUID is shown once with the latest IP`() {
        val first = DiscoveredDevice("192.168.1.20", "Grill", "device-1", "free_grilly")
        val latest = DiscoveredDevice("192.168.1.21", "Grill+", "device-1", "grilly_plus")

        val devices = DiscoveryDedupe.upsert(listOf(first), latest)

        assertEquals(listOf(latest), devices)
    }
}
