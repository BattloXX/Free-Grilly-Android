package org.battlo.freegrilly.data.device.model

/** Firmware-neutral representation of the device metadata returned by /api/info. */
data class DeviceIdentity(
    val uuid: String = "",
    val name: String = "",
    val hostname: String = "",
    val firmwareName: String = "",
    val firmwareVersion: String = "",
    val apiVersion: String = "",
    val capabilities: Set<String> = emptySet(),
)

data class Diagnostics(
    val batteryMillivolts: Int? = null,
    val lastOffReason: String? = null,
    val lastResetReason: String? = null,
    val uptimeSeconds: Long? = null,
)

data class Probe(
    val id: Int,
    val name: String = "",
    val connected: Boolean = false,
    val temperatureC: Float? = null,
    val targetTemperatureC: Float? = null,
    val minimumTemperatureC: Float? = null,
    val alarm: Boolean = false,
    val etaSeconds: Int? = null,
    val type: String? = null,
    val calibrationOffsetC: Float? = null,
) {
    // Compatibility conveniences for the current Compose screens while they consume the neutral model.
    val resolvedId get() = id
    val resolvedTemperature get() = temperatureC ?: 0f
    val resolvedTargetTemperature get() = targetTemperatureC ?: 0f
    val resolvedMinimumTemperature get() = minimumTemperatureC ?: 0f
}

data class GrillState(
    val identity: DeviceIdentity = DeviceIdentity(),
    val temperatureUnit: String = "celcius",
    val batteryPercentage: Int? = null,
    val batteryCharging: Boolean? = null,
    val wifiConnected: Boolean? = null,
    val wifiSignalDbm: Int? = null,
    val alarmActive: Boolean = false,
    /** Optional firmware cook id; absent on currently released Grilly+ firmware. */
    val cookSessionId: String? = null,
    val probes: List<Probe> = emptyList(),
    val diagnostics: Diagnostics = Diagnostics(),
) {
    val name get() = identity.name
    val batteryMillivolts get() = diagnostics.batteryMillivolts ?: 0
    val lastOffReason get() = diagnostics.lastOffReason.orEmpty()
    val lastResetReason get() = diagnostics.lastResetReason.orEmpty()
    val wifiSignal get() = wifiSignalDbm ?: -100
}

data class ProbePatch(
    val id: Int,
    val name: String? = null,
    val targetTemperatureC: Float? = null,
    val minimumTemperatureC: Float? = null,
    val type: String? = null,
    val calibrationOffsetC: Float? = null,
)

data class DeviceSettings(
    val grillName: String? = null,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val temperatureUnit: String? = null,
    val backlightTimeoutMinutes: Int? = null,
    val screenTimeoutMinutes: Int? = null,
    val powerSaving: Boolean? = null,
)

enum class HistoryTier { FINE, COARSE }

data class HistoryPoint(val timestampMs: Long, val temperatureC: Float)
data class HistorySeries(val probeId: Int, val tier: HistoryTier, val points: List<HistoryPoint>)
data class DeviceWifiNetwork(val ssid: String, val rssi: Int?, val encryption: String?)
