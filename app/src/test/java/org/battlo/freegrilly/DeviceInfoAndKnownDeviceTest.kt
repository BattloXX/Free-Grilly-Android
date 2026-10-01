package org.battlo.freegrilly

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.battlo.freegrilly.data.KnownDevice
import org.battlo.freegrilly.data.api.models.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoAndKnownDeviceTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun `info fixtures decode string and numeric API versions as strings`() {
        val free = json.decodeFromString<DeviceInfo>(fixture("free-grilly-info.json"))
        val plusString = json.decodeFromString<DeviceInfo>(fixture("grilly-plus-info.json"))
        val plusInteger = json.decodeFromString<DeviceInfo>(fixture("grilly-plus-info-v2.json"))

        assertEquals("", free.apiVersion)
        assertEquals("1", plusString.apiVersion)
        assertEquals("1", plusInteger.apiVersion)
    }

    @Test fun `known device JSON from before info metadata remains decodable`() {
        val old = """{"uuid":"free-1","name":"Free","ip":"192.168.1.20","mdnsHostname":"free.local","firmwareVersion":"26.09.1"}"""

        val device = json.decodeFromString<KnownDevice>(old)

        assertEquals("", device.apiVersion)
        assertEquals("", device.firmwareName)
    }

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")).readText()
}
