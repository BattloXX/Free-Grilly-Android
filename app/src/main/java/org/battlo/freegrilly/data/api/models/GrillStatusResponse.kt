package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrillStatusResponse(
    val name: String = "",
    val uuid: String = "",
    @SerialName("temperature_unit") val temperatureUnit: String = "celcius",
    @SerialName("battery_percentage") val batteryPercentage: Int = 0,
    @SerialName("battery_charging") val batteryCharging: Boolean = false,
    @SerialName("wifi_connected") val wifiConnected: Boolean = false,
    @SerialName("wifi_signal") val wifiSignal: Int = -100,
    @SerialName("alarm_active") val alarmActive: Boolean = false,
    @SerialName("mdns_hostname") val mdnsHostname: String = "",
    val probes: List<ProbeStatus> = emptyList()
)

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
