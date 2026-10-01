package org.battlo.freegrilly.data

import kotlinx.serialization.Serializable
import org.battlo.freegrilly.data.device.model.GrillState

sealed interface GrillyUiState {
    object Loading : GrillyUiState
    data class Connected(
        val status: GrillState,
        val history: Map<Int, List<Float>>,
    ) : GrillyUiState
    object Disconnected : GrillyUiState
    object Demo : GrillyUiState
}

enum class FirmwareVariant { FREE_GRILLY, GRILLY_PLUS, ORIGINAL }

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
    /** API contract version reported by /api/info. */
    val apiVersion: String = "",
    /** Firmware family/name reported by /api/info; IP remains only a last-known address. */
    val firmwareName: String = "",
    val firmwareVariant: FirmwareVariant = FirmwareVariant.FREE_GRILLY,
) {
    val isOriginalFirmware: Boolean get() = capabilities.isEmpty() && firmwareVersion.isNotEmpty()
    val supportsHistory: Boolean get() = capabilities.toSet().supports(Capabilities.HISTORY)
    val supportsAlarmMute: Boolean get() = capabilities.toSet().supports(Capabilities.ALARM_MUTE)
}
