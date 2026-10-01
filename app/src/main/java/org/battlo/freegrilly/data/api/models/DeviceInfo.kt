package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/** Accept the string form used by Free-Grilly and the numeric form used by Grilly+. */
object StringOrNumberSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String =
        (decoder as? JsonDecoder)?.decodeJsonElement()?.let { (it as? JsonPrimitive)?.content }
            ?: decoder.decodeString()

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

@Serializable
data class DeviceInfo(
    val uuid: String = "",
    @SerialName("unique_id") val uniqueId: String = "",
    val name: String = "",
    /** Firmware family, e.g. "grilly-plus". */
    val firmware: String = "",
    @SerialName("firmware_version") val firmwareVersion: String = "",
    /** Free-Grilly uses a string; Grilly+ sends the OpenAPI integer form. */
    @SerialName("api_version") @Serializable(with = StringOrNumberSerializer::class)
    val apiVersion: String = "",
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
