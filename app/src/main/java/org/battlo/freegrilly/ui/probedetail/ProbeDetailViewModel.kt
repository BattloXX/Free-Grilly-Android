package org.battlo.freegrilly.ui.probedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import org.battlo.freegrilly.data.api.models.ProbeConfig
import org.battlo.freegrilly.data.api.models.ProbeStatus
import javax.inject.Inject

@HiltViewModel
class ProbeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GrillyRepository,
    private val deviceStore: DeviceStore,
) : ViewModel() {

    private val probeId: Int = savedStateHandle["probeId"] ?: 1

    val probe: StateFlow<ProbeStatus?> = repository.statusFlow
        .map { state -> (state as? GrillyUiState.Connected)?.status?.probes?.find { it.id == probeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<Float>> = repository.statusFlow
        .map { repository.getHistoryForProbe(probeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unit: StateFlow<String> = deviceStore.temperatureUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "celcius")

    fun muteAlarm() = viewModelScope.launch { repository.muteAlarm() }

    fun setTarget(targetC: Float, minC: Float = 0f) {
        val current = probe.value ?: return
        viewModelScope.launch {
            repository.updateProbeConfig(
                ProbeConfig(
                    id = current.id,
                    name = current.name,
                    targetTemperature = targetC,
                    minimumTemperature = minC,
                )
            )
        }
    }
}
