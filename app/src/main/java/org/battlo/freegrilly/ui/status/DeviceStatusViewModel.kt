package org.battlo.freegrilly.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import org.battlo.freegrilly.data.api.models.DeviceInfo
import javax.inject.Inject

/** Snapshot of everything the Grilly status page shows. */
data class DeviceStatusUi(
    val connected: Boolean = false,
    val demo: Boolean = false,
    val name: String = "",
    val firmware: String = "",
    val uuid: String = "",
    val mdnsHostname: String = "",
    val ipAddress: String = "",
    val batteryPercent: Int = 0,
    val batteryCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiSignalDbm: Int = 0,
    val temperatureUnit: String = "celcius",
    val probesTotal: Int = 0,
    val probesConnected: Int = 0,
    val capabilities: List<String> = emptyList(),
)

@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    private val repository: GrillyRepository,
    deviceStore: DeviceStore,
) : ViewModel() {

    // /api/info is not part of the 1-s poll, so fetch it once (and on manual refresh).
    private val deviceInfo = MutableStateFlow<DeviceInfo?>(null)

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
            name = info?.name?.ifBlank { null } ?: status?.name.orEmpty(),
            firmware = info?.firmware?.ifBlank { null } ?: status?.resolvedFirmware.orEmpty(),
            uuid = info?.uuid?.ifBlank { null } ?: status?.resolvedUuid.orEmpty(),
            mdnsHostname = info?.mdnsHostname?.ifBlank { null } ?: status?.mdnsHostname.orEmpty(),
            ipAddress = ip.orEmpty(),
            batteryPercent = status?.batteryPercentage ?: 0,
            batteryCharging = status?.batteryCharging ?: false,
            wifiConnected = status?.wifiConnected ?: false,
            wifiSignalDbm = status?.wifiSignal ?: 0,
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
