package org.battlo.freegrilly.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.history.CookSessionEntity
import org.battlo.freegrilly.data.history.Downsample
import org.battlo.freegrilly.data.history.TempSample
import javax.inject.Inject

data class ProbeSeries(val probeId: Int, val points: List<TempSample>)

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val repository: GrillyRepository,
) : ViewModel() {

    val sessions: StateFlow<List<CookSessionEntity>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Load downsampled per-probe series for one session (called when a row is expanded). */
    suspend fun loadCharts(sessionId: Long): List<ProbeSeries> =
        repository.sessionProbeIds(sessionId)
            .map { pid -> ProbeSeries(pid, Downsample.minMax(repository.sessionSamples(sessionId, pid), 300)) }
            .filter { it.points.size > 1 }
}
