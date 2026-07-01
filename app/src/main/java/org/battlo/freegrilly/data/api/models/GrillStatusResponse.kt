package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrillStatusResponse(
    val name: String = "",
    val uuid: String = "",
    /** epiecs firmware returns "unique_id" instead of "uuid" */
    @SerialName("unique_id") val uniqueId: String = "",
    @SerialName("temperature_unit") val temperatureUnit: String = "celcius",
    @SerialName("battery_percentage") val batteryPercentage: Int = 0,
    @SerialName("battery_charging") val batteryCharging: Boolean = false,
    /** Measured cell voltage in mV (firmware ≥26.07.01); 0 if unavailable/older firmware. */
    @SerialName("battery_millivolts") val batteryMillivolts: Int = 0,
    /** Why the device last powered off: "button"/"low_battery"/"boot_gate"/"" (firmware ≥26.07.01). */
    @SerialName("last_off_reason") val lastOffReason: String = "",
    /** ESP32 reset reason at last boot, e.g. "brownout"/"panic"/"deepsleep" (firmware ≥26.07.01). */
    @SerialName("last_reset_reason") val lastResetReason: String = "",
    @SerialName("wifi_connected") val wifiConnected: Boolean = false,
    @SerialName("wifi_signal") val wifiSignal: Int = -100,
    @SerialName("alarm_active") val alarmActive: Boolean = false,
    @SerialName("mdns_hostname") val mdnsHostname: String = "",
    /** epiecs firmware returns "firmware_version" in /api/grill instead of a dedicated /api/info */
    @SerialName("firmware_version") val legacyFirmwareVersion: String = "",
    val probes: List<ProbeStatus> = emptyList()
) {
    /** Resolved UUID: BattloXX firmware → "uuid", epiecs → "unique_id" */
    val resolvedUuid: String get() = uuid.ifBlank { uniqueId }
    /** Resolved firmware version: BattloXX → via /api/info; epiecs fallback → this field */
    val resolvedFirmware: String get() = legacyFirmwareVersion
}

@Serializable
data class ProbeStatus(
    val id: Int = 0,
    val name: String = "",
    val connected: Boolean = false,
    val temperature: Float = 0f,
    @SerialName("target_temperature") val targetTemperature: Float = 0f,
    @SerialName("minimum_temperature") val minimumTemperature: Float = 0f,
    val alarm: Boolean = false,
    @SerialName("eta_seconds") val etaSeconds: Int = -1
)
