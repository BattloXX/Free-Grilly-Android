package org.battlo.freegrilly.service

import org.battlo.freegrilly.data.device.model.Probe
import org.battlo.freegrilly.domain.TempUtils
import kotlin.math.roundToInt

object MonitorNotificationText {
    fun format(probes: List<Probe>, unit: String, noProbes: String): String {
        val connected = probes.filter { it.connected }
        if (connected.isEmpty()) return noProbes
        fun temperature(value: Float) = "${TempUtils.displayTemp(value, unit).roundToInt()}°${TempUtils.unitSymbol(unit)}"
        return connected.joinToString(" · ") { probe ->
            val current = probe.temperatureC?.let(::temperature) ?: "–"
            val target = probe.targetTemperatureC?.let { " / ${temperature(it)}" }.orEmpty()
            "${probe.name}: $current$target"
        }
    }
}
