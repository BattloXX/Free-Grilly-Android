package org.battlo.freegrilly.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import org.battlo.freegrilly.data.device.model.DeviceIdentity
import javax.inject.Inject

/** Snapshot of everything the Grilly status page shows. */
data class DeviceStatusUi(
    val connected: Boolean = false,
    val demo: Boolean = false,
    val name: String = "",
    val firmwareName: String = "",
    val firmwareVersion: String = "",
    val apiVersion: String = "",
    val uuid: String = "",
    val mdnsHostname: String = "",
    val ipAddress: String = "",
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean? = null,
    val batteryMillivolts: Int? = null,
    val lastOffReason: String = "",
    val lastResetReason: String = "",
    val uptimeSeconds: Long? = null,
    val wifiConnected: Boolean? = null,
    val wifiSignalDbm: Int? = null,
    val temperatureUnit: String = "celcius",
    val probesTotal: Int = 0,
    val probesConnected: Int = 0,
    val capabilities: List<String> = emptyList(),
)

/** A displayable status value. The filtering lives here so it is testable without Compose. */
data class DeviceStatusRow(val kind: Kind, val value: String) {
    enum class Kind {
        NAME, FIRMWARE_NAME, FIRMWARE_VERSION, API_VERSION, UUID, MDNS_HOSTNAME, IP_ADDRESS,
        WIFI_CONNECTED, WIFI_RSSI, BATTERY_PERCENT, BATTERY_VOLTAGE, BATTERY_CHARGING,
        LAST_RESET_REASON, LAST_OFF_REASON, UPTIME,
    }
}

object DeviceStatusRows {
    fun from(ui: DeviceStatusUi): List<DeviceStatusRow> = buildList {
        fun addText(kind: DeviceStatusRow.Kind, text: String) {
            text.trim().takeIf { it.isNotEmpty() }?.let { add(DeviceStatusRow(kind, it)) }
        }
        addText(DeviceStatusRow.Kind.NAME, ui.name)
        addText(DeviceStatusRow.Kind.FIRMWARE_NAME, ui.firmwareName)
        addText(DeviceStatusRow.Kind.FIRMWARE_VERSION, ui.firmwareVersion)
        addText(DeviceStatusRow.Kind.API_VERSION, ui.apiVersion)
        addText(DeviceStatusRow.Kind.UUID, ui.uuid)
        addText(DeviceStatusRow.Kind.MDNS_HOSTNAME, ui.mdnsHostname)
        addText(DeviceStatusRow.Kind.IP_ADDRESS, ui.ipAddress)
        ui.wifiConnected?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.WIFI_CONNECTED, it.toString())) }
        // The legacy DTO uses -100 as its "Wi-Fi unavailable" sentinel.
        ui.wifiSignalDbm?.takeIf { it != 0 && it != -100 }?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.WIFI_RSSI, it.toString())) }
        ui.batteryPercent?.takeIf { it != 0 }?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.BATTERY_PERCENT, it.toString())) }
        ui.batteryMillivolts?.takeIf { it != 0 }?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.BATTERY_VOLTAGE, it.toString())) }
        ui.batteryCharging?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.BATTERY_CHARGING, it.toString())) }
        addText(DeviceStatusRow.Kind.LAST_RESET_REASON, ui.lastResetReason)
        addText(DeviceStatusRow.Kind.LAST_OFF_REASON, ui.lastOffReason)
        ui.uptimeSeconds?.let { add(DeviceStatusRow(DeviceStatusRow.Kind.UPTIME, formatUptime(it))) }
    }

    private fun formatUptime(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val days = safeSeconds / 86_400
        val hours = safeSeconds / 3_600 % 24
        val minutes = safeSeconds / 60 % 60
        val remainingSeconds = safeSeconds % 60
        return when {
            days > 0 -> "$days d $hours h"
            hours > 0 -> "$hours h ${minutes.toString().padStart(2, '0')} min"
            minutes > 0 -> "$minutes min ${remainingSeconds.toString().padStart(2, '0')} s"
            else -> "$remainingSeconds s"
        }
    }
}

@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    private val repository: GrillyRepository,
    deviceStore: DeviceStore,
) : ViewModel() {

    // /api/info is not part of the 1-s poll, so fetch it once (and on manual refresh).
    private val deviceInfo = MutableStateFlow<DeviceIdentity?>(null)

    init {
        refresh()
    }

    val ui: StateFlow<DeviceStatusUi> = combine(
        repository.statusFlow,
        repository.capabilitiesFlow,
        deviceInfo,
        deviceStore.selectedDeviceIp,
    ) { state, caps, info, ip ->
        val status = (state as? GrillyUiState.Connected)?.status
        val demo = state is GrillyUiState.Demo
        DeviceStatusUi(
            connected = status != null || demo,
            demo = demo,
            name = info?.name?.ifBlank { null } ?: status?.identity?.name.orEmpty(),
            firmwareName = info?.firmwareName?.ifBlank { null } ?: status?.identity?.firmwareName.orEmpty(),
            firmwareVersion = info?.firmwareVersion?.ifBlank { null } ?: status?.identity?.firmwareVersion.orEmpty(),
            apiVersion = info?.apiVersion?.ifBlank { null } ?: status?.identity?.apiVersion.orEmpty(),
            uuid = info?.uuid?.ifBlank { null } ?: status?.identity?.uuid.orEmpty(),
            mdnsHostname = info?.hostname?.ifBlank { null } ?: status?.identity?.hostname.orEmpty(),
            ipAddress = ip.orEmpty(),
            batteryPercent = status?.batteryPercentage,
            batteryCharging = status?.batteryCharging,
            batteryMillivolts = status?.diagnostics?.batteryMillivolts,
            lastOffReason = status?.diagnostics?.lastOffReason.orEmpty(),
            lastResetReason = status?.diagnostics?.lastResetReason.orEmpty(),
            uptimeSeconds = status?.diagnostics?.uptimeSeconds,
            wifiConnected = status?.wifiConnected,
            wifiSignalDbm = status?.wifiSignalDbm,
            temperatureUnit = status?.temperatureUnit ?: "celcius",
            probesTotal = status?.probes?.size ?: 0,
            probesConnected = status?.probes?.count { it.connected } ?: 0,
            capabilities = (caps.takeIf { it.isNotEmpty() }?.toList()
                ?: info?.capabilities.orEmpty()).sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceStatusUi())

    fun refresh() {
        viewModelScope.launch {
            repository.getDeviceInfo()?.let { deviceInfo.value = it }
        }
    }
}
