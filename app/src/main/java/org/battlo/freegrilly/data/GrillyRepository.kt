package org.battlo.freegrilly.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import org.battlo.freegrilly.data.api.models.ProbeConfig
import org.battlo.freegrilly.data.stream.GrillEventSource
import org.battlo.freegrilly.di.ApplicationScope
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrillyRepository @Inject constructor(
    private val api: GrillyApiService,
    private val alarmController: AlarmController,
    private val eventSource: GrillEventSource,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val TAG = "GrillyRepository"
    private val historyBuffers = mutableMapOf<Int, ArrayDeque<Float>>()
    private val bufferCapacity = 600

    // Populated from /api/info after connecting. Empty = unknown (original firmware).
    private val _capabilitiesFlow = MutableStateFlow<Set<String>>(emptySet())
    val capabilitiesFlow: StateFlow<Set<String>> = _capabilitiesFlow.asStateFlow()

    var activeCapabilities: Set<String> = emptySet()
        private set

    fun setCapabilities(caps: List<String>) {
        activeCapabilities = caps.toSet()
        _capabilitiesFlow.value = activeCapabilities
    }

    private val _statusFlow = MutableStateFlow<GrillyUiState>(GrillyUiState.Loading)
    val statusFlow: StateFlow<GrillyUiState> = _statusFlow.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            // §8 — SSE path: use server-push instead of 1-second polling when the device
            // explicitly declares the `events` capability. Falls through to the polling loop
            // if the stream ends or the flag is absent (original firmware, inert default).
            if (activeCapabilities.hasFlag(Capabilities.EVENTS)) {
                Log.d(TAG, "Starting SSE stream (events capability present)")
                runCatching {
                    eventSource.stream().collect { status ->
                        appendToHistory(status)
                        _statusFlow.value = GrillyUiState.Connected(
                            status = status,
                            history = historyBuffers.mapValues { it.value.toList() },
                        )
                        if (status.alarmActive) {
                            alarmController.onAlarmActive(status.probes.filter { it.alarm })
                        } else {
                            alarmController.onAlarmCleared()
                        }
                    }
                }.onFailure {
                    Log.w(TAG, "SSE stream failed, falling back to polling", it)
                    if (_statusFlow.value !is GrillyUiState.Disconnected) {
                        _statusFlow.value = GrillyUiState.Disconnected
                    }
                }
                // SSE stream ended (device closed or all retries exhausted) — fall through
                // to the polling loop below so the app recovers automatically.
            }

            // Default: 1-second polling (original firmware or SSE stream ended).
            Log.d(TAG, "Starting 1-second polling loop")
            while (isActive) {
                try {
                    val status = api.getGrillStatus()
                    appendToHistory(status)
                    _statusFlow.value = GrillyUiState.Connected(
                        status = status,
                        history = historyBuffers.mapValues { it.value.toList() },
                    )
                    if (status.alarmActive) {
                        alarmController.onAlarmActive(status.probes.filter { it.alarm })
                    } else {
                        alarmController.onAlarmCleared()
                    }
                } catch (_: Exception) {
                    if (_statusFlow.value !is GrillyUiState.Disconnected) {
                        _statusFlow.value = GrillyUiState.Disconnected
                    }
                }
                delay(1_000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setDemoMode() {
        _statusFlow.value = GrillyUiState.Demo
    }

    suspend fun seedHistory() {
        // Skip gracefully if we know the device doesn't support history.
        if (!activeCapabilities.supports(Capabilities.HISTORY)) return
        runCatching {
            val response = api.getHistory()
            response.probes.forEach { probe ->
                val buf = historyBuffers.getOrPut(probe.id) { ArrayDeque(bufferCapacity) }
                probe.history.forEach { raw ->
                    if (buf.size >= bufferCapacity) buf.removeFirst()
                    buf.addLast(raw / 10f)
                }
            }
        }
    }

    private fun appendToHistory(status: GrillStatusResponse) {
        status.probes.filter { it.connected }.forEach { probe ->
            val buf = historyBuffers.getOrPut(probe.id) { ArrayDeque(bufferCapacity) }
            if (buf.size >= bufferCapacity) buf.removeFirst()
            buf.addLast(probe.temperature)
        }
    }

    fun getHistoryForProbe(probeId: Int): List<Float> =
        historyBuffers[probeId]?.toList() ?: emptyList()

    suspend fun muteAlarm(): Result<Unit> = runCatching {
        if (activeCapabilities.supports(Capabilities.ALARM_MUTE)) {
            api.muteAlarm()
        }
        alarmController.onAlarmCleared()
    }

    suspend fun updateProbeConfig(config: ProbeConfig): Result<Unit> = runCatching {
        api.updateProbes(listOf(config))
    }

    suspend fun updateSettings(
        grillName: String? = null,
        unit: String? = null,
        backlightTimeout: Int? = null,
        screenTimeout: Int? = null,
        /** §8 — Power-saving; only sent when non-null (device must have [Capabilities.POWER_SAVING]). */
        powerSaving: Boolean? = null,
    ): Result<Unit> = runCatching {
        api.updateSettings(
            org.battlo.freegrilly.data.api.models.DeviceSettings(
                grillName = grillName,
                temperatureUnit = unit,
                backlightTimeoutMinutes = backlightTimeout,
                screenTimeoutMinutes = screenTimeout,
                powerSaving = powerSaving,
            )
        )
    }

    suspend fun getDeviceInfo() = runCatching { api.getInfo() }.getOrNull()

    suspend fun getDeviceSettings() = runCatching { api.getSettings() }.getOrNull()
}
