package org.battlo.freegrilly.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceConnector
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.DeviceHost
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val deviceConnector: DeviceConnector,
) : ViewModel() {

    /**
     * Start destination:
     * - A selected device UUID exists → dashboard (device is known and was previously connected).
     * - Otherwise → device_selector, which auto-starts an mDNS scan and shows any grills found
     *   on the local network as well as all saved (but not selected) devices.
     *   "Add new device" in the selector routes to onboarding for a factory-fresh grill.
     *
     * Null while DataStore loads; the splash screen stays up until it is known. Resolved once:
     * a start destination that changes later (e.g. clearing the selected device on a device
     * switch) would make NavHost swap its graph under the running screens.
     */
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val uuid = runCatching { deviceStore.selectedDeviceUuid.first() }.getOrNull()
            _startDestination.value = if (uuid != null) "dashboard" else "device_selector"
        }
        viewModelScope.launch {
            // Restore the saved IP immediately so the dashboard polling hits the right host
            // from the very first request (before the background verify completes).
            val uuid = deviceStore.selectedDeviceUuid.first() ?: return@launch
            val ip = deviceStore.selectedDeviceIp.first()
            if (ip != null && DeviceHost.isValid(ip)) baseUrlInterceptor.currentHost.value = ip

            // Background: verify the saved IP, auto-correct via mDNS if stale, restore
            // capabilities (required for SSE / §8 opt-in features to activate on relaunch).
            val device = deviceStore.knownDevices.first().firstOrNull { it.uuid == uuid }
                ?: return@launch
            Log.d("MainViewModel", "Background reconnect for ${device.name} at ${device.ip}")
            deviceConnector.connect(device)
            // Failure is silent — the dashboard will show "Disconnected" and let the user retry.
        }
    }
}
