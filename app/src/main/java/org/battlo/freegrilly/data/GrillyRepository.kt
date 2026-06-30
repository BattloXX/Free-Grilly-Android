package org.battlo.freegrilly.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import org.battlo.freegrilly.data.api.models.ProbeConfig
import org.battlo.freegrilly.data.history.CookSessionEntity
import org.battlo.freegrilly.data.history.HistoryDao
import org.battlo.freegrilly.data.history.TempSample
import org.battlo.freegrilly.data.history.TempSampleEntity
import org.battlo.freegrilly.data.stream.GrillEventSource
import org.battlo.freegrilly.di.ApplicationScope
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GrillyRepository @Inject constructor(
    private val api: GrillyApiService,
    private val alarmController: AlarmController,
    private val eventSource: GrillEventSource,
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
    private val _sessionIdFlow = MutableStateFlow<Long?>(null)
    val sessionIdFlow: StateFlow<Long?> = _sessionIdFlow.asStateFlow()
    private var lastPersistMs = 0L
    private val PERSIST_INTERVAL_MS = 10_000L
    // Resume the previous session (= same cook) instead of starting a new one if its last
    // sample is recent — so a cook survives app restarts and device reboots.
    private val SESSION_RESUME_GAP_MS = 60 * 60_000L

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
                        appendAndPersist(status)
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
                    appendAndPersist(status)
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
        // Clear the in-memory session pointer so the next start re-evaluates resume-vs-new
        // (after a process restart this is null anyway). Session rows are kept.
        currentSessionId = null
        _sessionIdFlow.value = null
    }

    fun setDemoMode() {
        _statusFlow.value = GrillyUiState.Demo
    }

    /**
     * Open the current cook session, resuming the previous one if its last sample is recent
     * (so a cook continues across app restarts / device reboots), otherwise creating a new one.
     * Returns (sessionId, isNew).
     */
    private suspend fun ensureSession(): Pair<Long, Boolean> {
        currentSessionId?.let { return it to false }
        val deviceId = runCatching { deviceStore.selectedDeviceUuid.first() }.getOrNull() ?: "default"
        val now = System.currentTimeMillis()
        val latest = runCatching { historyDao.latestSession(deviceId) }.getOrNull()
        val resume = latest != null &&
            (runCatching { historyDao.lastSampleTs(latest.id) }.getOrNull()
                ?.let { now - it < SESSION_RESUME_GAP_MS } ?: false)
        val id = if (resume && latest != null) {
            latest.id
        } else {
            runCatching { historyDao.insertSession(CookSessionEntity(deviceId = deviceId, startedAt = now)) }
                .getOrDefault(-1L)
        }
        currentSessionId = id
        _sessionIdFlow.value = id.takeIf { it >= 0 }
        return id to !(resume && latest != null)
    }

    suspend fun seedHistory() {
        val (sessionId, isNew) = ensureSession()

        if (!activeCapabilities.supports(Capabilities.HISTORY)) return
        val response = runCatching { api.getHistory() }.getOrNull() ?: return

        // RAM buffers (fine tier) for the live dashboard sparkline.
        response.probes.forEach { probe ->
            val buf = historyBuffers.getOrPut(probe.id) { ArrayDeque(bufferCapacity) }
            buf.clear()
            probe.history.forEach { raw ->
                if (buf.size >= bufferCapacity) buf.removeFirst()
                buf.addLast(raw / 10f)
            }
        }

        // Backfill Room only for a brand-new session (avoid re-seeding a resumed cook).
        if (!isNew || sessionId < 0) return
        val now = System.currentTimeMillis()
        val fineMs = response.intervalSeconds.coerceAtLeast(1) * 1000L
        val coarseMs = response.coarseIntervalSeconds.coerceAtLeast(1) * 1000L
        val samples = ArrayList<TempSampleEntity>()
        response.probes.forEach { probe ->
            val nf = probe.history.size
            // Newest fine sample ≈ now; walk back at fineMs.
            val fineStart = now - (nf - 1).coerceAtLeast(0).toLong() * fineMs
            val nc = probe.historyCoarse.size
            // Coarse: only points strictly older than the fine window → no overlap, no dups.
            probe.historyCoarse.forEachIndexed { i, raw ->
                val ts = now - (nc - 1 - i).toLong() * coarseMs
                if (ts < fineStart) {
                    samples.add(TempSampleEntity(sessionId = sessionId, probeId = probe.id, tsMs = ts, tempCx10 = raw))
                }
            }
            probe.history.forEachIndexed { i, raw ->
                val ts = now - (nf - 1 - i).toLong() * fineMs
                samples.add(TempSampleEntity(sessionId = sessionId, probeId = probe.id, tsMs = ts, tempCx10 = raw))
            }
        }
        if (samples.isNotEmpty()) runCatching { historyDao.insertSamples(samples) }
    }

    private suspend fun appendAndPersist(status: GrillStatusResponse) {
        val now = System.currentTimeMillis()

        // RAM (every update) for the live cards.
        status.probes.filter { it.connected }.forEach { probe ->
            val buf = historyBuffers.getOrPut(probe.id) { ArrayDeque(bufferCapacity) }
            if (buf.size >= bufferCapacity) buf.removeFirst()
            buf.addLast(probe.temperature)
        }

        // Room (throttled) for the durable detail / whole-cook view.
        if (now - lastPersistMs >= PERSIST_INTERVAL_MS) {
            lastPersistMs = now
            val sid = currentSessionId ?: ensureSession().first
            if (sid >= 0) {
                val samples = status.probes.filter { it.connected }.map {
                    TempSampleEntity(
                        sessionId = sid,
                        probeId = it.id,
                        tsMs = now,
                        tempCx10 = (it.temperature * 10f).roundToInt(),
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
