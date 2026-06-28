package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.Serializable

@Serializable
data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val encryption: String
)
