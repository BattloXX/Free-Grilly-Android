package org.battlo.freegrilly.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.battlo.freegrilly.data.*
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.device.GrillyDeviceApiHolder
import org.battlo.freegrilly.data.device.model.DeviceSettings as NeutralDeviceSettings
import org.battlo.freegrilly.data.device.model.DeviceIdentity
import org.battlo.freegrilly.data.api.models.WifiNetwork
import org.battlo.freegrilly.R
import javax.inject.Inject

sealed interface OnboardingStep {
    object ApConnect : OnboardingStep
    data class WifiScan(val firmware: DeviceIdentity) : OnboardingStep
    data class Credentials(
        val networks: List<WifiNetwork>,
        val firmware: DeviceIdentity,
        val selectedSsid: String = "",
    ) : OnboardingStep
    object Provisioning : OnboardingStep
    object Discovery : OnboardingStep
    object Complete : OnboardingStep
}

sealed interface OnboardingError {
    @get:StringRes val messageRes: Int

    data object NetworksNotFound : OnboardingError {
        override val messageRes = R.string.onboarding_error_networks_not_found
    }
    data object DeviceNotFound : OnboardingError {
        override val messageRes = R.string.onboarding_error_device_not_found
    }
    data class RequestFailed(val detail: String?) : OnboardingError {
        override val messageRes = R.string.onboarding_error_request_failed
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val api: GrillyApiService,
    private val deviceStore: DeviceStore,
    private val nsdDiscovery: NsdDiscovery,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val deviceApiHolder: GrillyDeviceApiHolder,
    private val deviceConnector: DeviceConnector,
) : ViewModel() {

    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.ApConnect)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _error = MutableStateFlow<OnboardingError?>(null)
    val error: StateFlow<OnboardingError?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private var setupDeviceUuid: String = ""
    private var detectedFirmware: DeviceIdentity? = null

    fun onApConnected() {
        viewModelScope.launch {
            baseUrlInterceptor.currentHost.value = SETUP_AP_HOST
            runCatching {
                // Both real firmwares expose /api/info from their open setup AP. Select the
                // adapter once before scanning/provisioning so its settings spelling is used.
                val info = api.getInfo()
                deviceApiHolder.selectForFirmware(info.firmware)
                deviceApiHolder.api.info()
            }.onSuccess { identity ->
                setupDeviceUuid = identity.uuid
                detectedFirmware = identity
                _step.value = OnboardingStep.WifiScan(identity)
                scanWifi()
            }.onFailure {
                _error.value = OnboardingError.RequestFailed(it.message)
                _step.value = OnboardingStep.ApConnect
            }
        }
    }

    private suspend fun scanWifi() {
        _isLoading.value = true
        var networks: List<WifiNetwork> = emptyList()
        var attempts = 0
        while (networks.isEmpty() && attempts < 15) {
            runCatching {
                networks = deviceApiHolder.api.wifiScan().map {
                    WifiNetwork(it.ssid, it.rssi ?: 0, it.encryption.orEmpty())
                }
            }
            if (networks.isEmpty()) { delay(1_000); attempts++ }
        }
        _isLoading.value = false
        if (networks.isNotEmpty()) {
            _step.value = OnboardingStep.Credentials(networks, requireNotNull(detectedFirmware))
        } else {
            _error.value = OnboardingError.NetworksNotFound
            _step.value = OnboardingStep.ApConnect
        }
    }

    fun onCredentialsSubmitted(ssid: String, password: String, grillName: String, unit: String) {
        viewModelScope.launch {
            _step.value = OnboardingStep.Provisioning
            _isLoading.value = true
            runCatching {
                deviceApiHolder.api.updateSettings(
                    NeutralDeviceSettings(
                        grillName = grillName,
                        wifiSsid = ssid,
                        wifiPassword = password,
                        temperatureUnit = unit,
                    )
                )
                _step.value = OnboardingStep.Discovery
                discoverDevice()
            }.onFailure { e ->
                _error.value = OnboardingError.RequestFailed(e.message)
                _step.value = OnboardingStep.ApConnect
            }
            _isLoading.value = false
        }
    }

    private suspend fun discoverDevice() {
        val expectedUuid = setupDeviceUuid
        if (expectedUuid.isBlank()) {
            _error.value = OnboardingError.DeviceNotFound
            _step.value = OnboardingStep.ApConnect
            return
        }

        repeat(OnboardingDiscoveryPolicy.ATTEMPTS) { attempt ->
            nsdDiscovery.startDiscovery(includeOriginal = true, targetUuid = expectedUuid)
            val found = withTimeoutOrNull(OnboardingDiscoveryPolicy.DISCOVERY_WINDOW_MS) {
                nsdDiscovery.state.filterIsInstance<DiscoveryState.Found>().first()
            }
            nsdDiscovery.stopDiscovery()
            if (found != null) {
                baseUrlInterceptor.currentHost.value = found.ip
                // NSD confirms candidates through /api/info; confirm again on the selected host
                // before saving, because only the AP response UUID may complete this wizard.
                val info = runCatching { api.getInfo() }.getOrNull()
                if (info?.resolvedUuid == expectedUuid) {
                    deviceConnector.persistDeviceFromInfo(
                        info = info,
                        ip = found.ip,
                        fallback = KnownDevice(
                            uuid = expectedUuid,
                            name = found.name,
                            ip = found.ip,
                            mdnsHostname = found.name,
                        ),
                    )
                    _step.value = OnboardingStep.Complete
                    return
                }
            }
            if (OnboardingDiscoveryPolicy.shouldRetry(attempt)) delay(OnboardingDiscoveryPolicy.BETWEEN_ATTEMPTS_MS)
        }
        _error.value = OnboardingError.DeviceNotFound
        _step.value = OnboardingStep.ApConnect
    }

    fun clearError() { _error.value = null }

    companion object {
        private const val SETUP_AP_HOST = "192.168.200.10"
    }

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
