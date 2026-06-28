package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val uuid: String = "",
    val name: String = "",
    val firmware: String = "",
    @SerialName("mdns_hostname") val mdnsHostname: String = "",
    val capabilities: List<String> = emptyList()
)
