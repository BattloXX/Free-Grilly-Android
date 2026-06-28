package org.battlo.freegrilly.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import org.battlo.freegrilly.data.api.models.ProbeConfig
import org.battlo.freegrilly.di.ApplicationScope
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrillyRepository @Inject constructor(
    private val api: GrillyApiService,
    private val alarmController: AlarmController,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val historyBuffers = mutableMapOf<Int, ArrayDeque<Float>>()
    private val bufferCapacity = 600

    // Populated from /api/info after connecting. Empty = unknown (original firmware).
    var activeCapabilities: Set<String> = emptySet()
        private set

    fun setCapabilities(caps: List<String>) {
        activeCapabilities = caps.toSet()
    }

    private val _statusFlow = MutableStateFlow<GrillyUiState>(GrillyUiState.Loading)
    val statusFlow: StateFlow<GrillyUiState> = _statusFlow.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
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
        if (activeCapabilities.isNotEmpty() && "history" !in activeCapabilities) return
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
        if (activeCapabilities.isEmpty() || "alarm_mute" in activeCapabilities) {
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
    ): Result<Unit> = runCatching {
        api.updateSettings(
            org.battlo.freegrilly.data.api.models.DeviceSettings(
                grillName = grillName,
                temperatureUnit = unit,
                backlightTimeoutMinutes = backlightTimeout,
                screenTimeoutMinutes = screenTimeout,
            )
        )
    }

    suspend fun getDeviceInfo() = runCatching { api.getInfo() }.getOrNull()
}
