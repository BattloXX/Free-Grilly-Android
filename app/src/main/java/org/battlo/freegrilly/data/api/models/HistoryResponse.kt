package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(
    @SerialName("interval_seconds") val intervalSeconds: Int = 10,
    /** Coarse-tier interval (whole-cook view). Adaptive on the device; 0/absent on old firmware. */
    @SerialName("coarse_interval_seconds") val coarseIntervalSeconds: Int = 0,
    val probes: List<ProbeHistory> = emptyList()
)

@Serializable
data class ProbeHistory(
    val id: Int,
    val name: String,
    val history: List<Int> = emptyList(),
    /** Coarse tier (whole cook). Empty on old firmware. */
    @SerialName("history_coarse") val historyCoarse: List<Int> = emptyList()
)
