package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.Serializable

@Serializable
data class WifiNetwork(
    val ssid: String = "",
    val rssi: Int = 0,
    val encryption: String = "",
    @kotlinx.serialization.SerialName("signal_strength") val signalStrength: Int = 0,
    @kotlinx.serialization.SerialName("auth_method") val authMethod: String = "",
) {
    val resolvedRssi: Int get() = rssi.takeIf { it != 0 } ?: signalStrength
    val resolvedEncryption: String get() = encryption.ifBlank { authMethod }
}
