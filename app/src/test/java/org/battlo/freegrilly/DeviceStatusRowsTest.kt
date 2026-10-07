package org.battlo.freegrilly

import org.battlo.freegrilly.ui.status.DeviceStatusRow
import org.battlo.freegrilly.ui.status.DeviceStatusRows
import org.battlo.freegrilly.ui.status.DeviceStatusUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceStatusRowsTest {
    @Test
    fun `only known non-empty diagnostic values produce rows`() {
        val rows = DeviceStatusRows.from(DeviceStatusUi(
            firmwareName = "grilly-plus", firmwareVersion = "1.2.3", apiVersion = "1",
            batteryPercent = 86, batteryMillivolts = 3942, batteryCharging = false,
            wifiSignalDbm = -61, lastResetReason = "poweron", lastOffReason = "button",
            uptimeSeconds = 7_500,
            uuid = "device-id", mdnsHostname = "grilly.local",
        ))

        assertEquals(
            listOf(
                DeviceStatusRow.Kind.FIRMWARE_NAME, DeviceStatusRow.Kind.FIRMWARE_VERSION,
                DeviceStatusRow.Kind.API_VERSION, DeviceStatusRow.Kind.UUID, DeviceStatusRow.Kind.MDNS_HOSTNAME,
                DeviceStatusRow.Kind.WIFI_RSSI, DeviceStatusRow.Kind.BATTERY_PERCENT,
                DeviceStatusRow.Kind.BATTERY_VOLTAGE, DeviceStatusRow.Kind.BATTERY_CHARGING,
                DeviceStatusRow.Kind.LAST_RESET_REASON, DeviceStatusRow.Kind.LAST_OFF_REASON,
                DeviceStatusRow.Kind.UPTIME,
            ),
            rows.map { it.kind },
        )
    }

    @Test
    fun `blank and zero unknown values are omitted`() {
        val rows = DeviceStatusRows.from(DeviceStatusUi(
            firmwareName = " ", firmwareVersion = "", apiVersion = " ", uuid = "", mdnsHostname = " ",
            batteryPercent = 0, batteryMillivolts = 0, wifiSignalDbm = -100,
            lastResetReason = "", lastOffReason = " ",
        ))

        assertTrue(rows.isEmpty())
        assertFalse(rows.any { it.kind == DeviceStatusRow.Kind.BATTERY_VOLTAGE })
        assertFalse(rows.any { it.kind == DeviceStatusRow.Kind.UPTIME })
    }

    @Test
    fun `uptime is formatted when present`() {
        val row = DeviceStatusRows.from(DeviceStatusUi(uptimeSeconds = 7_500)).single()
        assertEquals(DeviceStatusRow.Kind.UPTIME, row.kind)
        assertEquals("2 h 05 min", row.value)
    }
}
