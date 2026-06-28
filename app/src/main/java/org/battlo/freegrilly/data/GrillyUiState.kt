package org.battlo.freegrilly.data

import kotlinx.serialization.Serializable
import org.battlo.freegrilly.data.api.models.GrillStatusResponse

sealed interface GrillyUiState {
    object Loading : GrillyUiState
    data class Connected(
        val status: GrillStatusResponse,
        val history: Map<Int, List<Float>>,
    ) : GrillyUiState
    object Disconnected : GrillyUiState
    object Demo : GrillyUiState
}

@Serializable
data class KnownDevice(
    val uuid: String,
    val name: String,
    val ip: String,
    val mdnsHostname: String,
    val lastSeen: Long = 0L,
    /** Populated from /api/info capabilities array. Empty = unknown/original firmware. */
    val capabilities: List<String> = emptyList(),
    val firmwareVersion: String = "",
) {
    val isOriginalFirmware: Boolean get() = capabilities.isEmpty() && firmwareVersion.isNotEmpty()
    val supportsHistory: Boolean get() = capabilities.isEmpty() || "history" in capabilities
    val supportsAlarmMute: Boolean get() = capabilities.isEmpty() || "alarm_mute" in capabilities
}
