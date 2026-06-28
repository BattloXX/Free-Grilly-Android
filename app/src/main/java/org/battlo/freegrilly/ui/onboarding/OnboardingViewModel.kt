package org.battlo.freegrilly.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.*
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.DeviceSettings
import org.battlo.freegrilly.data.api.models.WifiNetwork
import javax.inject.Inject

sealed interface OnboardingStep {
    object ApConnect : OnboardingStep
    object WifiScan : OnboardingStep
    data class Credentials(val networks: List<WifiNetwork>, val selectedSsid: String = "") : OnboardingStep
    object Provisioning : OnboardingStep
    object Discovery : OnboardingStep
    object Complete : OnboardingStep
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val api: GrillyApiService,
    private val deviceStore: DeviceStore,
    private val nsdDiscovery: NsdDiscovery,
    private val baseUrlInterceptor: BaseUrlInterceptor,
) : ViewModel() {

    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.ApConnect)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onApConnected() {
        viewModelScope.launch {
            _step.value = OnboardingStep.WifiScan
            scanWifi()
        }
    }

    private suspend fun scanWifi() {
        _isLoading.value = true
        baseUrlInterceptor.currentHost.value = "192.168.200.10"
        var networks: List<WifiNetwork> = emptyList()
        var attempts = 0
        while (networks.isEmpty() && attempts < 15) {
            runCatching { networks = api.getWifiNetworks() }
            if (networks.isEmpty()) { delay(1_000); attempts++ }
        }
        _isLoading.value = false
        if (networks.isNotEmpty()) {
            _step.value = OnboardingStep.Credentials(networks)
        } else {
            _error.value = "Keine Netzwerke gefunden. Stelle sicher, dass du mit dem FreeGrilly-AP (FreeGrilly_xxxxxxxx) oder dem Grilleye-AP (Grilleye_xxxxxxxx) verbunden bist."
            _step.value = OnboardingStep.ApConnect
        }
    }

    fun onCredentialsSubmitted(ssid: String, password: String, grillName: String, unit: String) {
        viewModelScope.launch {
            _step.value = OnboardingStep.Provisioning
            _isLoading.value = true
            runCatching {
                api.updateSettings(
                    DeviceSettings(
                        grillName = grillName,
                        wifiSsid = ssid,
                        wifiPassword = password,
                        temperatureUnit = unit,
                    )
                )
                delay(8_000)
                _step.value = OnboardingStep.Discovery
                discoverDevice()
            }.onFailure { e ->
                _error.value = "Fehler: ${e.message}"
                _step.value = OnboardingStep.ApConnect
            }
            _isLoading.value = false
        }
    }

    private fun discoverDevice() {
        nsdDiscovery.startDiscovery(includeOriginal = true)
        viewModelScope.launch {
            nsdDiscovery.state.collect { state ->
                when (state) {
                    is DiscoveryState.Found -> {
                        baseUrlInterceptor.currentHost.value = state.ip
                        // Fetch full device info to capture capabilities + firmware version.
                        val info = runCatching { api.getInfo() }.getOrNull()
                        val device = KnownDevice(
                            uuid = info?.uuid ?: state.uuid.ifEmpty { state.ip },
                            name = info?.name?.ifBlank { state.name } ?: state.name,
                            ip = state.ip,
                            mdnsHostname = info?.mdnsHostname ?: state.name,
                            lastSeen = System.currentTimeMillis(),
                            capabilities = info?.capabilities ?: emptyList(),
                            firmwareVersion = info?.firmware ?: "",
                        )
                        deviceStore.saveKnownDevice(device)
                        deviceStore.setSelectedDevice(device)
                        nsdDiscovery.stopDiscovery()
                        _step.value = OnboardingStep.Complete
                    }
                    is DiscoveryState.Failed ->
                        _error.value = "Gerät nicht gefunden. Bitte versuche es erneut."
                    else -> {}
                }
            }
        }
    }

    fun clearError() { _error.value = null }

    fun skipToDemo() {
        viewModelScope.launch {
            deviceStore.setDemoMode(true)
            deviceStore.setSelectedDevice(
                KnownDevice(uuid = "demo", name = "Demo Griller", ip = "demo", mdnsHostname = "demo")
            )
            _step.value = OnboardingStep.Complete
        }
    }
}
