package org.battlo.freegrilly.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.DiscoveryState
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.KnownDevice
import org.battlo.freegrilly.data.NsdDiscovery
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import javax.inject.Inject

data class ConnectResult(val success: Boolean, val errorMessage: String? = null)

@HiltViewModel
class DeviceSelectorViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
    private val repository: GrillyRepository,
    private val api: GrillyApiService,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val nsdDiscovery: NsdDiscovery,
) : ViewModel() {

    val knownDevices: StateFlow<List<KnownDevice>> = deviceStore.knownDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _connectingUuid = MutableStateFlow<String?>(null)
    val connectingUuid: StateFlow<String?> = _connectingUuid.asStateFlow()

    private val _connectError = MutableStateFlow<String?>(null)
    val connectError: StateFlow<String?> = _connectError.asStateFlow()

    val discoveryState: StateFlow<DiscoveryState> = nsdDiscovery.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoveryState.Idle)

    fun connectDevice(device: KnownDevice, onSuccess: () -> Unit) {
        if (_connectingUuid.value != null) return
        viewModelScope.launch {
            _connectingUuid.value = device.uuid
            _connectError.value = null

            // Fast path: try last-known IP directly (≤3 s timeout).
            val reachable = tryDirectConnect(device.ip)

            val finalDevice = if (reachable) {
                enrichWithInfo(device)
            } else {
                // Slow path: NSD re-discovery filtered to this UUID.
                nsdDiscovery.startDiscovery(includeOriginal = true, targetUuid = device.uuid)
                val found = withTimeoutOrNull(15_000) {
                    nsdDiscovery.state.filterIsInstance<DiscoveryState.Found>().first()
                }
                nsdDiscovery.stopDiscovery()

                if (found == null) {
                    _connectError.value = "Gerät nicht gefunden. Prüfe ob der Grill eingeschaltet und im selben WLAN ist."
                    _connectingUuid.value = null
                    return@launch
                }

                baseUrlInterceptor.currentHost.value = found.ip
                enrichWithInfo(device.copy(ip = found.ip))
            }

            val now = System.currentTimeMillis()
            val saved = finalDevice.copy(lastSeen = now)
            repository.setCapabilities(saved.capabilities)
            deviceStore.saveKnownDevice(saved)
            deviceStore.setSelectedDevice(saved)
            _connectingUuid.value = null
            onSuccess()
        }
    }

    fun connectManualIp(ip: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _connectingUuid.value = "manual"
            _connectError.value = null
            baseUrlInterceptor.currentHost.value = ip
            val reachable = tryDirectConnect(ip)
            if (!reachable) {
                _connectError.value = "Keine Verbindung zu $ip möglich."
                _connectingUuid.value = null
                return@launch
            }
            val info = runCatching { api.getInfo() }.getOrNull()
            val device = KnownDevice(
                uuid = info?.uuid ?: ip,
                name = info?.name ?: "Grilleye",
                ip = ip,
                mdnsHostname = info?.mdnsHostname ?: "",
                lastSeen = System.currentTimeMillis(),
                capabilities = info?.capabilities ?: emptyList(),
                firmwareVersion = info?.firmware ?: "",
            )
            repository.setCapabilities(device.capabilities)
            deviceStore.saveKnownDevice(device)
            deviceStore.setSelectedDevice(device)
            _connectingUuid.value = null
            onSuccess()
        }
    }

    fun startNetworkScan() {
        nsdDiscovery.startDiscovery(includeOriginal = true, targetUuid = null)
    }

    fun stopNetworkScan() {
        nsdDiscovery.stopDiscovery()
    }

    fun removeDevice(uuid: String) {
        viewModelScope.launch { deviceStore.removeDevice(uuid) }
    }

    fun dismissError() {
        _connectError.value = null
    }

    private suspend fun tryDirectConnect(ip: String): Boolean {
        baseUrlInterceptor.currentHost.value = ip
        return withTimeoutOrNull(3_000) {
            runCatching { api.getGrillStatus() }.isSuccess
        } ?: false
    }

    private suspend fun enrichWithInfo(device: KnownDevice): KnownDevice {
        val info = runCatching { api.getInfo() }.getOrNull() ?: return device
        return device.copy(
            name = info.name.ifBlank { device.name },
            capabilities = info.capabilities,
            firmwareVersion = info.firmware,
        )
    }
}
