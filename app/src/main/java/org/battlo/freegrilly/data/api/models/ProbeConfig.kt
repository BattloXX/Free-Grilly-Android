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
    val beep: Boolean = true
)
