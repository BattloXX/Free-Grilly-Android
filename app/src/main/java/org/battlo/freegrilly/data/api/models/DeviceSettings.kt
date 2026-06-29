package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSettings(
    @SerialName("grill_name") val grillName: String? = null,
    @SerialName("wifi_ssid") val wifiSsid: String? = null,
    @SerialName("wifi_password") val wifiPassword: String? = null,
    @SerialName("temperature_unit") val temperatureUnit: String? = null,
    @SerialName("backlight_timeout_minutes") val backlightTimeoutMinutes: Int? = null,
    @SerialName("screen_timeout_minutes") val screenTimeoutMinutes: Int? = null,
    /** §8 — Power-saving mode. null = omit from request (field not sent). */
    @SerialName("power_saving") val powerSaving: Boolean? = null,
)
