package org.battlo.freegrilly.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceConnector
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GrillyRepository,
    private val deviceStore: DeviceStore,
    private val deviceConnector: DeviceConnector,
) : ViewModel() {

    val uiState: StateFlow<GrillyUiState> = repository.statusFlow

    val unit: StateFlow<String> = deviceStore.temperatureUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "celcius")

    init {
        viewModelScope.launch {
            deviceStore.isDemoMode.collect { demo ->
                if (demo) {
                    repository.setDemoMode()
                } else {
                    repository.seedHistory()
                    repository.startPolling()
                }
            }
        }
    }

    fun muteAlarm() = viewModelScope.launch { repository.muteAlarm() }

    /**
     * Manual reconnect (the "Erneut verbinden" button on the Disconnected screen).
     *
     * The polling loop already retries the *same* host every second, so it can recover from a
     * brief network blip on its own. But it can never recover when the device's IP changed
     * (DHCP) — it keeps hammering the stale address forever. This re-runs the full connect
     * strategy (saved IP → mDNS hostname → NSD re-discovery by UUID), which updates the host to
     * the new IP and refreshes capabilities, then restarts polling against the right host.
     */
    fun reconnect() = viewModelScope.launch {
        repository.setReconnecting()
        repository.stopPolling()
        val uuid = deviceStore.selectedDeviceUuid.first()
        val device = uuid?.let { id -> deviceStore.knownDevices.first().firstOrNull { it.uuid == id } }
        if (device != null) {
            // Result ignored on purpose: whether connect() found the device or not, we (re)start
            // polling. On success it hits the (possibly new) host → Connected; on failure the
            // poll loop sets Disconnected again so the retry button comes back.
            deviceConnector.connect(device)
        }
        repository.seedHistory()
        repository.startPolling()
    }

    fun enableDemoMode() = viewModelScope.launch {
        deviceStore.setDemoMode(true)
        repository.setDemoMode()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopPolling()
    }
}
