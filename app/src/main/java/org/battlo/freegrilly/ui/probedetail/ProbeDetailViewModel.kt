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
import org.battlo.freegrilly.data.history.Downsample
import org.battlo.freegrilly.data.history.TempSample
import javax.inject.Inject

/** Selectable graph time window. `durationMs == null` = whole cook (auto-fit). */
enum class HistoryWindow(val durationMs: Long?) {
    M30(30 * 60_000L),
    H1(60 * 60_000L),
    H6(6 * 60 * 60_000L),
    ALL(null),
}

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

    private val windowState = MutableStateFlow(HistoryWindow.ALL)
    val window: StateFlow<HistoryWindow> = windowState.asStateFlow()
    fun setWindow(w: HistoryWindow) { windowState.value = w }

    /** Time-stamped, window-filtered and downsampled samples for the chart. */
    val samples: StateFlow<List<TempSample>> =
        combine(repository.observeSamples(probeId), windowState) { all, w ->
            val filtered = if (w.durationMs == null || all.isEmpty()) {
                all
            } else {
                val cutoff = all.last().tsMs - w.durationMs
                all.filter { it.tsMs >= cutoff }
            }
            Downsample.minMax(filtered, 300)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
