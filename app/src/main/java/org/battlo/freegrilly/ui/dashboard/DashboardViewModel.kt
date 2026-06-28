package org.battlo.freegrilly.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GrillyRepository,
    private val deviceStore: DeviceStore,
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

    fun enableDemoMode() = viewModelScope.launch {
        deviceStore.setDemoMode(true)
        repository.setDemoMode()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopPolling()
    }
}
