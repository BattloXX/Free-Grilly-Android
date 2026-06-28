package org.battlo.freegrilly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
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

    fun setUnit(unit: String) {
        viewModelScope.launch {
            deviceStore.setTemperatureUnit(unit)
            repository.updateSettings(unit = unit)
        }
    }

    fun setLanguage(lang: String) = viewModelScope.launch { deviceStore.setAppLanguage(lang) }

    fun exitDemoMode() = viewModelScope.launch { deviceStore.setDemoMode(false) }
}
