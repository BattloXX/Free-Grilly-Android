package org.battlo.freegrilly.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.ConnectResult
import org.battlo.freegrilly.data.DeviceConnector
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.DiscoveredDevice
import org.battlo.freegrilly.data.DiscoveryState
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.KnownDevice
import org.battlo.freegrilly.data.NsdDiscovery
import javax.inject.Inject

@HiltViewModel
class DeviceSelectorViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
    private val repository: GrillyRepository,
    private val deviceConnector: DeviceConnector,
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

    /**
     * §8 — Live list of all devices found during the current NSD scan.
     * Updated in real-time as devices are found or lost (multi-device picker).
     */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = nsdDiscovery.discoveredDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Connect to a saved [KnownDevice] (fast-path IP → mDNS fallback via [DeviceConnector]). */
    fun connectDevice(device: KnownDevice, onSuccess: () -> Unit) {
        if (_connectingUuid.value != null) return
        viewModelScope.launch {
            _connectingUuid.value = device.uuid
            _connectError.value = null
            val result = deviceConnector.connect(device)
            _connectingUuid.value = null
            if (result.success) onSuccess() else _connectError.value = result.errorMessage
        }
    }

    /** Connect to a device by raw IP address (manual entry or freshly discovered via mDNS scan). */
    fun connectManualIp(ip: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _connectingUuid.value = "manual"
            _connectError.value = null
            val result = deviceConnector.connectByIp(ip)
            _connectingUuid.value = null
            if (result.success) onSuccess() else _connectError.value = result.errorMessage
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

    /**
     * §8 — In-app device switch: stops current polling, clears the active device,
     * and connects to [device]. Can be called from the Dashboard's device-switcher action.
     */
    fun switchToDevice(device: KnownDevice, onSuccess: () -> Unit) {
        repository.stopPolling()
        viewModelScope.launch {
            deviceStore.clearSelectedDevice()
            connectDevice(device, onSuccess)
        }
    }

    fun dismissError() {
        _connectError.value = null
    }
}
