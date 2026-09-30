package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProbeConfig(
    val id: Int = 0,
    @SerialName("probe_id") val probeId: Int = 0,
    val name: String = "",
    val type: String = "meat",
    @SerialName("probe_type") val probeType: String = "",
    @SerialName("target_temperature") val targetTemperature: Float = 0f,
    @SerialName("minimum_temperature") val minimumTemperature: Float = 0f,
    val beep: Boolean = true,
    // Thermistor calibration — round-tripped so a partial edit (target/name) does not reset
    // the probe's type/calibration on the device. POST /api/probes is a full replace: any
    // omitted field is defaulted (type → "custom", references → 0). Always read-modify-write.
    @SerialName("reference_kohm") val referenceKohm: Float = 0f,
    @SerialName("reference_celcius") val referenceCelcius: Float = 0f,
    @SerialName("reference_beta") val referenceBeta: Float = 0f,
    @SerialName("offset_celcius") val offsetCelcius: Float? = null,
) {
    val resolvedId: Int get() = id.takeIf { it != 0 } ?: probeId
}

/** Partial Grilly+ update. Its API deliberately keeps omitted probe fields unchanged. */
@Serializable
data class GrillyPlusProbePatch(
    @SerialName("probe_id") val probeId: Int,
    val name: String? = null,
    @SerialName("target_temperature") val targetTemperature: Float? = null,
    @SerialName("minimum_temperature") val minimumTemperature: Float? = null,
    @SerialName("probe_type") val probeType: String? = null,
    @SerialName("offset_celcius") val offsetCelcius: Float? = null,
)
