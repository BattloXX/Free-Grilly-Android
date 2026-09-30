package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val uuid: String = "",
    @SerialName("unique_id") val uniqueId: String = "",
    val name: String = "",
    /** Firmware family, e.g. "grilly-plus". */
    val firmware: String = "",
    @SerialName("firmware_version") val firmwareVersion: String = "",
    @SerialName("api_version") val apiVersion: String = "",
    @SerialName("mdns_hostname") val mdnsHostname: String = "",
    @SerialName("hostname") val hostname: String = "",
    val capabilities: List<String> = emptyList()
) {
    val resolvedUuid: String get() = uuid.ifBlank { uniqueId }
    val resolvedHostname: String get() = mdnsHostname.ifBlank { hostname }
    /** Grilly+ puts its family in firmware and its actual version in firmware_version. */
    val resolvedFirmwareVersion: String get() = firmwareVersion.ifBlank { firmware }
    val isGrillyPlus: Boolean get() = firmware.equals("grilly-plus", ignoreCase = true)
}
