package org.battlo.freegrilly.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.battlo.freegrilly.data.device.GrillyDeviceApiHolder
import org.battlo.freegrilly.data.device.model.GrillState
import org.battlo.freegrilly.data.device.model.Probe
import org.battlo.freegrilly.data.device.model.ProbePatch
import org.battlo.freegrilly.data.device.model.DeviceSettings
import org.battlo.freegrilly.data.history.CookSessionEntity
import org.battlo.freegrilly.data.history.HistoryDao
import org.battlo.freegrilly.data.history.SessionAction
import org.battlo.freegrilly.data.history.decideSession
import org.battlo.freegrilly.data.history.mergeHistorySeries
import org.battlo.freegrilly.data.history.TempSample
import org.battlo.freegrilly.data.history.TempSampleEntity
import org.battlo.freegrilly.di.ApplicationScope
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GrillyRepository @Inject constructor(
    private val deviceApiHolder: GrillyDeviceApiHolder,
    private val alarmController: AlarmController,
    private val historyDao: HistoryDao,
    private val deviceStore: DeviceStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val TAG = "GrillyRepository"

    // RAM buffer feeds the compact dashboard sparkline (recent live values).
    private val historyBuffers = mutableMapOf<Int, ArrayDeque<Float>>()
    private val bufferCapacity = 600

    // Durable history (Room): one sample per connected probe every PERSIST_INTERVAL_MS, tied
    // to a cook session. Decoupled from the 1-s poll to bound DB growth and write load.
    private var currentSessionId: Long? = null
    private var currentFirmwareSessionId: String? = null
    private val _sessionIdFlow = MutableStateFlow<Long?>(null)
    val sessionIdFlow: StateFlow<Long?> = _sessionIdFlow.asStateFlow()
    private var lastPersistMs = 0L
    private val PERSIST_INTERVAL_MS = 10_000L
    // Resume the previous session (= same cook) instead of starting a new one if its last
    // sample is recent — so a cook survives app restarts and device reboots.
    private val SESSION_RESUME_GAP_MS = 60 * 60_000L

    // The grill runs a single-threaded web server: when several phones poll the same device at
    // once, an occasional request loses the race and times out even though the device is fine.
    // Tolerate a few consecutive misses before showing "Disconnected" so a second device doesn't
    // make the dashboard flicker offline. At the 1-s poll cadence this is a ~few-second grace.
    private val MAX_POLL_FAILURES = 4

    // Populated from /api/info after connecting. Empty = unknown (original firmware).
    private val _capabilitiesFlow = MutableStateFlow<Set<String>>(emptySet())
    val capabilitiesFlow: StateFlow<Set<String>> = _capabilitiesFlow.asStateFlow()
    private val _features = MutableStateFlow(DeviceFeatures())
    val features: StateFlow<DeviceFeatures> = _features.asStateFlow()

    var activeCapabilities: Set<String> = emptySet()
        private set

    fun setCapabilities(caps: List<String>) {
        activeCapabilities = Capabilities.normalize(caps)
        _capabilitiesFlow.value = activeCapabilities
        _features.value = DeviceFeatures.from(activeCapabilities)
    }

    private val _statusFlow = MutableStateFlow<GrillyUiState>(GrillyUiState.Loading)
    val statusFlow: StateFlow<GrillyUiState> = _statusFlow.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            // Default: 1-second polling (original firmware or SSE stream ended).
            Log.d(TAG, "Starting 1-second polling loop")
            var consecutiveFailures = 0
            while (isActive) {
                try {
                    val status = deviceApiHolder.api.status()
                    consecutiveFailures = 0
                    appendAndPersist(status)
                    _statusFlow.value = GrillyUiState.Connected(
                        status = status,
                        history = historyBuffers.mapValues { it.value.toList() },
                    )
                    alarmController.onAlarmStateChanged(status.alarmActive, status.probes, status.temperatureUnit)
                } catch (_: Exception) {
                    // A single miss is expected under multi-device contention — only flip to
                    // Disconnected after several consecutive failures (see MAX_POLL_FAILURES).
                    consecutiveFailures++
                    if (consecutiveFailures >= MAX_POLL_FAILURES &&
                        _statusFlow.value !is GrillyUiState.Disconnected
                    ) {
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
        // Clear the in-memory session pointer so the next start re-evaluates resume-vs-new
        // (after a process restart this is null anyway). Session rows are kept.
        currentSessionId = null
        currentFirmwareSessionId = null
        _sessionIdFlow.value = null
    }

    fun setDemoMode() {
        deviceApiHolder.selectDemo()
        _statusFlow.value = GrillyUiState.Demo
    }

    /** Show the "connecting" spinner while a manual reconnect is in flight. */
    fun setReconnecting() {
        _statusFlow.value = GrillyUiState.Loading
    }

    /**
     * Open the current cook session, resuming the previous one if its last sample is recent
     * (so a cook continues across app restarts / device reboots), otherwise creating a new one.
     * Returns (sessionId, isNew).
     */
    private suspend fun ensureSession(firmwareSessionId: String? = null): Pair<Long, Boolean> {
        if (currentSessionId != null && currentFirmwareSessionId == firmwareSessionId) {
            return currentSessionId!! to false
        }
        val deviceId = runCatching { deviceStore.selectedDeviceUuid.first() }.getOrNull() ?: "default"
        val now = System.currentTimeMillis()
        val latest = runCatching { historyDao.latestSession(deviceId) }.getOrNull()
        val resume = latest != null &&
            (runCatching { historyDao.lastSampleTs(latest.id) }.getOrNull()
                ?.let { now - it < SESSION_RESUME_GAP_MS } ?: false)
        val existingFirmware = firmwareSessionId?.let {
            runCatching { historyDao.latestFirmwareSession(deviceId, it) }.getOrNull()
        }
        val decision = decideSession(
            currentSessionId, currentFirmwareSessionId, firmwareSessionId,
            existingFirmware?.id, resume,
        )
        if (decision.closeCurrent) currentSessionId?.let { runCatching { historyDao.closeSession(it, now) } }
        val id = when (decision.action) {
            SessionAction.KEEP_CURRENT -> currentSessionId ?: -1L
            SessionAction.RESUME_EXISTING -> (existingFirmware ?: latest)?.id ?: -1L
            SessionAction.CREATE_NEW -> runCatching {
                historyDao.insertSession(CookSessionEntity(
                    deviceId = deviceId, firmwareSessionId = firmwareSessionId, startedAt = now,
                ))
            }.getOrDefault(-1L)
        }
        currentSessionId = id
        // A missing optional field must not erase a previously seen firmware id: if it returns
        // on a later poll we still need to detect a true id transition.
        if (firmwareSessionId != null) currentFirmwareSessionId = firmwareSessionId
        _sessionIdFlow.value = id.takeIf { it >= 0 }
        return id to (decision.action == SessionAction.CREATE_NEW)
    }

    suspend fun seedHistory() {
        // Obtain an optional firmware cook id before assigning imported samples. Current
        // firmware does not expose it, so this safely falls back to the legacy policy.
        val status = runCatching { deviceApiHolder.api.status() }.getOrNull()
        val (sessionId, isNew) = ensureSession(status?.cookSessionId)

        if (!activeCapabilities.supports(Capabilities.HISTORY)) return
        val series = runCatching { deviceApiHolder.api.history() }.getOrNull() ?: return
        series.filter { it.tier == org.battlo.freegrilly.data.device.model.HistoryTier.FINE }.forEach { item ->
            val buffer = historyBuffers.getOrPut(item.probeId) { ArrayDeque(bufferCapacity) }
            buffer.clear()
            item.points.forEach { point -> if (buffer.size >= bufferCapacity) buffer.removeFirst(); buffer.addLast(point.temperatureC) }
        }
        // Free-Grilly reconstructs timestamps relative to the request time, so importing an
        // existing session would create shifted duplicates. Grilly+ has stable tier timestamps.
        if (sessionId < 0 || (!isNew && !deviceApiHolder.api.supportsHistoryGapFill)) return
        // On a resumed cook only fill the gap after the last stored sample: tier timestamps are
        // derived from request time, so re-importing older points would add jittered near-duplicates.
        val gapStart = if (isNew) Long.MIN_VALUE
            else runCatching { historyDao.lastSampleTs(sessionId) }.getOrNull() ?: Long.MIN_VALUE
        val samples = mergeHistorySeries(series).filter { (_, point) -> point.timestampMs > gapStart }.map { (probeId, point) ->
            TempSampleEntity(
                sessionId = sessionId,
                probeId = probeId,
                tsMs = point.timestampMs,
                tempCx10 = (point.temperatureC * 10).roundToInt(),
            )
        }
        if (samples.isNotEmpty()) runCatching { historyDao.insertSamples(samples) }
    }

    private suspend fun appendAndPersist(status: GrillState) {
        val now = System.currentTimeMillis()
        // Evaluate on every live poll so a future firmware cook-id transition cannot append
        // samples to the previous cook while waiting for the persistence interval.
        ensureSession(status.cookSessionId)

        // RAM (every update) for the live cards.
        status.probes.filter { it.connected }.forEach { probe ->
            val buf = historyBuffers.getOrPut(probe.resolvedId) { ArrayDeque(bufferCapacity) }
            if (buf.size >= bufferCapacity) buf.removeFirst()
            buf.addLast(probe.resolvedTemperature)
        }

        // Room (throttled) for the durable detail / whole-cook view.
        if (now - lastPersistMs >= PERSIST_INTERVAL_MS) {
            lastPersistMs = now
            val sid = currentSessionId ?: ensureSession(status.cookSessionId).first
            if (sid >= 0) {
                val samples = status.probes.filter { it.connected }.map {
                    TempSampleEntity(
                        sessionId = sid,
                        probeId = it.resolvedId,
                        tsMs = now,
                        tempCx10 = (it.resolvedTemperature * 10f).roundToInt(),
                    )
                }
                if (samples.isNotEmpty()) runCatching { historyDao.insertSamples(samples) }
            }
        }
    }

    fun getHistoryForProbe(probeId: Int): List<Float> =
        historyBuffers[probeId]?.toList() ?: emptyList()

    /** Live, time-stamped samples for the current session's probe (whole session). */
    fun observeSamples(probeId: Int): Flow<List<TempSample>> =
        sessionIdFlow.flatMapLatest { sid ->
            if (sid == null) flowOf(emptyList())
            else historyDao.observeSamples(sid, probeId, 0L)
                .map { list -> list.map { TempSample(it.tsMs, it.tempCx10 / 10f) } }
        }

    fun observeSessions() = historyDao.observeSessions()

    suspend fun sessionProbeIds(sessionId: Long): List<Int> =
        runCatching { historyDao.probeIdsForSession(sessionId) }.getOrDefault(emptyList())

    suspend fun sessionSamples(sessionId: Long, probeId: Int): List<TempSample> =
        runCatching { historyDao.samplesForProbe(sessionId, probeId) }.getOrDefault(emptyList())
            .map { TempSample(it.tsMs, it.tempCx10 / 10f) }

    suspend fun muteAlarm(probeId: Int? = null): Result<Unit> = runCatching {
        if (activeCapabilities.supports(Capabilities.ALARM_MUTE) ||
            (probeId != null && activeCapabilities.hasFlag(Capabilities.ALARM_PROBE_MUTE))
        ) {
            deviceApiHolder.api.muteAlarm(probeId)
        }
        alarmController.dismissNotifications(probeId)
    }

    suspend fun patchProbe(patch: ProbePatch): Result<Unit> = runCatching { deviceApiHolder.api.updateProbe(patch) }

    /** Reads the probe configuration endpoint, including calibration offset when supported. */
    suspend fun getProbe(probeId: Int): Probe? =
        runCatching { deviceApiHolder.api.probes().firstOrNull { it.id == probeId } }.getOrNull()

    suspend fun updateSettings(
        grillName: String? = null,
        unit: String? = null,
        backlightTimeout: Int? = null,
        screenTimeout: Int? = null,
        /** §8 — Power-saving; only sent when non-null (device must have [Capabilities.POWER_SAVING]). */
        powerSaving: Boolean? = null,
    ): Result<Unit> = runCatching {
        deviceApiHolder.api.updateSettings(DeviceSettings(grillName, temperatureUnit = unit, backlightTimeoutMinutes = backlightTimeout, screenTimeoutMinutes = screenTimeout, powerSaving = powerSaving))
    }

    suspend fun getDeviceInfo() = runCatching { deviceApiHolder.api.info() }.getOrNull()

    suspend fun getDeviceSettings() = runCatching { deviceApiHolder.api.settings() }.getOrNull()

    suspend fun clearHistory(probeId: Int): Result<Unit> = runCatching {
        check(activeCapabilities.hasFlag(Capabilities.CLEAR_HISTORY))
        deviceApiHolder.api.clearHistory(probeId)
    }

}
