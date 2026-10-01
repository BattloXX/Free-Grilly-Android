package org.battlo.freegrilly.data.device

import java.io.File
import kotlinx.coroutines.flow.Flow
import org.battlo.freegrilly.data.device.model.*

interface GrillyDeviceApi {
    val capabilities: Set<String>
    /** True only when imported history timestamps are stable across reconnects. */
    val supportsHistoryGapFill: Boolean get() = false
    val firmwareUpdateSource: FirmwareUpdateSource get() = FirmwareUpdateSource("BattloXX", "Free-Grilly", false)
    suspend fun info(): DeviceIdentity
    suspend fun status(): GrillState
    suspend fun probes(): List<Probe>
    suspend fun updateProbe(patch: ProbePatch)
    suspend fun settings(): DeviceSettings
    suspend fun updateSettings(settings: DeviceSettings)
    suspend fun history(): List<HistorySeries>
    suspend fun clearHistory(probeId: Int)
    suspend fun muteAlarm(probeId: Int? = null)
    suspend fun uploadFirmware(file: File, adminPassword: String = "")
    suspend fun wifiScan(): List<DeviceWifiNetwork>
    /** null means this firmware does not support events. */
    fun events(): Flow<GrillState>? = null
}

data class FirmwareUpdateSource(val owner: String, val repository: String, val otaOnlyAsset: Boolean)
