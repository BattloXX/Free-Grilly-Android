package org.battlo.freegrilly

import org.battlo.freegrilly.data.api.DeviceHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceHostTest {
    @Test fun `normalizes manual host input`() {
        val cases = mapOf(
            "192.168.1.5" to "192.168.1.5",
            " 192.168.1.5 " to "192.168.1.5",
            "http://192.168.1.5/" to "192.168.1.5",
            "https://GRILLY.local/api/info" to "grilly.local",
            "192,168,1,5" to "192.168.1.5",
            "192.168.1.5:80" to "192.168.1.5",
            "grilly.local" to "grilly.local",
            "Grilly.local.:80/" to "grilly.local",
        )
        cases.forEach { (input, expected) -> assertEquals(input, expected, DeviceHost.normalize(input)) }
    }

    @Test fun `rejects invalid hosts and unsupported ports`() {
        listOf(
            "192.168.1.5:8080", "Grilly Garten.local", "", "   ", "fe80::1%wlan0",
            "grilly\t.local", "user@grilly.local", "grilly.local?query", "grilly.local#fragment",
        ).forEach { assertNull(it, DeviceHost.normalize(it)) }
    }

    @Test fun `valid hosts must already be normalized`() {
        listOf("192.168.1.5", "grilly.local").forEach { assertTrue(it, DeviceHost.isValid(it)) }
        listOf(
            " 192.168.1.5 ", "http://192.168.1.5/", "192,168,1,5", "192.168.1.5:80",
            "Grilly.local", "grilly.local.", "Grilly Garten.local", "",
        ).forEach { assertFalse(it, DeviceHost.isValid(it)) }
    }
}
