package org.battlo.freegrilly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
    private val baseUrlInterceptor: BaseUrlInterceptor,
) : ViewModel() {

    val startDestination = combine(
        deviceStore.selectedDeviceUuid,
        deviceStore.knownDevices,
    ) { uuid, devices ->
        when {
            uuid != null -> "dashboard"
            devices.isNotEmpty() -> "device_selector"
            else -> "onboarding"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "onboarding")

    init {
        // Apply the saved device IP to the interceptor immediately on startup,
        // so the dashboard polling hits the correct device from the first request.
        viewModelScope.launch {
            val ip = deviceStore.selectedDeviceIp.first()
            if (ip != null) baseUrlInterceptor.currentHost.value = ip
        }
    }
}
