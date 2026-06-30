package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProbeConfig(
    val id: Int,
    val name: String,
    val type: String = "meat",
    @SerialName("target_temperature") val targetTemperature: Float = 0f,
    @SerialName("minimum_temperature") val minimumTemperature: Float = 0f,
    val beep: Boolean = true,
    // Thermistor calibration — round-tripped so a partial edit (target/name) does not reset
    // the probe's type/calibration on the device. POST /api/probes is a full replace: any
    // omitted field is defaulted (type → "custom", references → 0). Always read-modify-write.
    @SerialName("reference_kohm") val referenceKohm: Int = 0,
    @SerialName("reference_celcius") val referenceCelcius: Int = 0,
    @SerialName("reference_beta") val referenceBeta: Int = 0,
)
