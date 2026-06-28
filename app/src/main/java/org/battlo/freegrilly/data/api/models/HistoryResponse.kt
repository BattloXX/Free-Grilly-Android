package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(
    @SerialName("interval_seconds") val intervalSeconds: Int = 10,
    val probes: List<ProbeHistory> = emptyList()
)

@Serializable
data class ProbeHistory(
    val id: Int,
    val name: String,
    val history: List<Int> = emptyList()
)
