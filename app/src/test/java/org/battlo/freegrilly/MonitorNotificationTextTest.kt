package org.battlo.freegrilly

import org.battlo.freegrilly.data.device.model.Probe
import org.battlo.freegrilly.service.MonitorNotificationText
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorNotificationTextTest {
    @Test
    fun formatsConnectedProbesWithOptionalTargets() {
        val probes = listOf(
            Probe(1, "Sonde 1", true, 62.6f, 70f),
            Probe(2, "Sonde 2", true, 112f),
            Probe(3, "Sonde 3", false, 20f),
        )
        assertEquals("Sonde 1: 63°C / 70°C · Sonde 2: 112°C", MonitorNotificationText.format(probes, "celcius", "Empty"))
    }

    @Test
    fun convertsCurrentAndTargetToFahrenheit() {
        val probes = listOf(Probe(1, "Probe", true, 0f, 100f))
        assertEquals("Probe: 32°F / 212°F", MonitorNotificationText.format(probes, "fahrenheit", "Empty"))
    }

    @Test
    fun returnsLocalizedLabelWithoutConnectedProbes() {
        assertEquals("Keine Sonde", MonitorNotificationText.format(emptyList(), "celcius", "Keine Sonde"))
        assertEquals("Empty", MonitorNotificationText.format(listOf(Probe(1)), "celcius", "Empty"))
    }

    @Test
    fun missingTemperatureIsNotDisplayedAsZero() {
        assertEquals("Probe: – / 70°C", MonitorNotificationText.format(listOf(Probe(1, "Probe", true, targetTemperatureC = 70f)), "celcius", "Empty"))
    }
}
