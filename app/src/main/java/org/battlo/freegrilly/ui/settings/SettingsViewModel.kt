package org.battlo.freegrilly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.hasFlag
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
    private val repository: GrillyRepository,
) : ViewModel() {

    val unit: StateFlow<String> = deviceStore.temperatureUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "celcius")

    val language: StateFlow<String> = deviceStore.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    val isDemoMode: StateFlow<Boolean> = deviceStore.isDemoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // §8 — capabilities exposure for gating extension-point UI
    val capabilities: StateFlow<Set<String>> = repository.capabilitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** True only when the connected device explicitly declares the power_saving capability. */
    val supportsPowerSaving: StateFlow<Boolean> = capabilities
        .map { it.hasFlag(Capabilities.POWER_SAVING) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // §8 — Power-saving state (null = loading/unknown, requires supportsPowerSaving == true)
    private val _powerSaving = MutableStateFlow<Boolean?>(null)
    val powerSaving: StateFlow<Boolean?> = _powerSaving.asStateFlow()

    fun loadPowerSavingState() {
        viewModelScope.launch {
            val settings = runCatching { repository.getDeviceSettings() }.getOrNull() ?: return@launch
            _powerSaving.value = settings.powerSaving ?: false
        }
    }

    fun setPowerSaving(enabled: Boolean) {
        viewModelScope.launch {
            _powerSaving.value = enabled
            repository.updateSettings(powerSaving = enabled)
        }
    }

    fun setUnit(unit: String) {
        viewModelScope.launch {
            deviceStore.setTemperatureUnit(unit)
            repository.updateSettings(unit = unit)
        }
    }

    fun setLanguage(lang: String) = viewModelScope.launch { deviceStore.setAppLanguage(lang) }

    fun exitDemoMode() = viewModelScope.launch { deviceStore.setDemoMode(false) }
}
