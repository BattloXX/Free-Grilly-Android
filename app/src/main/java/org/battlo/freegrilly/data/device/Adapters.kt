package org.battlo.freegrilly.data.device

import android.util.Base64
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.api.FakeGrillyApi
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.DeviceInfo
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import org.battlo.freegrilly.data.api.models.GrillyPlusProbePatch
import org.battlo.freegrilly.data.api.models.ProbeConfig
import org.battlo.freegrilly.data.api.models.ProbeStatus
import org.battlo.freegrilly.data.api.models.DeviceSettings as ApiDeviceSettings
import org.battlo.freegrilly.data.device.model.DeviceIdentity
import org.battlo.freegrilly.data.device.model.DeviceSettings
import org.battlo.freegrilly.data.device.model.DeviceWifiNetwork
import org.battlo.freegrilly.data.device.model.Diagnostics
import org.battlo.freegrilly.data.device.model.GrillState
import org.battlo.freegrilly.data.device.model.HistoryPoint
import org.battlo.freegrilly.data.device.model.HistorySeries
import org.battlo.freegrilly.data.device.model.HistoryTier
import org.battlo.freegrilly.data.device.model.Probe
import org.battlo.freegrilly.data.device.model.ProbePatch
import org.battlo.freegrilly.data.history.GrillyPlusHistoryMapper

private fun DeviceInfo.identity() = DeviceIdentity(
    uuid = resolvedUuid, name = name, hostname = resolvedHostname,
    firmwareName = firmware, firmwareVersion = resolvedFirmwareVersion,
    apiVersion = apiVersion, capabilities = Capabilities.normalize(capabilities),
)

private fun GrillStatusResponse.state(identity: DeviceIdentity) = GrillState(
    identity = identity.copy(
        uuid = identity.uuid.ifBlank { resolvedUuid }, name = identity.name.ifBlank { name },
        hostname = identity.hostname.ifBlank { resolvedHostname },
        firmwareVersion = identity.firmwareVersion.ifBlank { resolvedFirmware },
    ),
    temperatureUnit = temperatureUnit,
    batteryPercentage = batteryPercentage,
    batteryCharging = batteryCharging,
    wifiConnected = wifiConnected,
    wifiSignalDbm = wifiSignal,
    alarmActive = isAlarmSounding,
    cookSessionId = resolvedCookSessionId,
    probes = probes.map { it.probe() },
    diagnostics = Diagnostics(batteryMillivolts.takeIf { it != 0 }, lastOffReason.ifBlank { null }, lastResetReason.ifBlank { null }),
)

private fun ProbeStatus.probe() = Probe(
    id = resolvedId, name = name, connected = connected, temperatureC = temperature,
    targetTemperatureC = targetTemperature, minimumTemperatureC = minimumTemperature,
    alarm = alarm, etaSeconds = etaSeconds.takeIf { it >= 0 },
)

private fun ProbeConfig.probe() = Probe(
    id = resolvedId, name = name, type = probeType.ifBlank { type },
    targetTemperatureC = targetTemperature, minimumTemperatureC = minimumTemperature,
    calibrationOffsetC = offsetCelcius,
)

private fun ApiDeviceSettings.deviceSettings() = DeviceSettings(
    grillName = resolvedGrillName, wifiSsid = wifiSsid, wifiPassword = wifiPassword,
    temperatureUnit = temperatureUnit, backlightTimeoutMinutes = backlightTimeoutMinutes,
    screenTimeoutMinutes = screenTimeoutMinutes, powerSaving = powerSaving,
)

private fun File.firmwarePart(): MultipartBody.Part = MultipartBody.Part.createFormData(
    "firmware", name, asRequestBody("application/octet-stream".toMediaType()),
)

abstract class RetrofitGrillyDeviceApi(protected val service: GrillyApiService) : GrillyDeviceApi {
    protected var identity = DeviceIdentity()
    override val capabilities: Set<String> get() = identity.capabilities

    override suspend fun info(): DeviceIdentity = service.getInfo().identity().also { identity = it }
    override suspend fun status(): GrillState = service.getGrillStatus().state(identity)
    override suspend fun probes(): List<Probe> = service.getProbes().map { it.probe() }
    override suspend fun wifiScan(): List<DeviceWifiNetwork> = service.getWifiNetworks().map {
        DeviceWifiNetwork(it.ssid, it.resolvedRssi, it.resolvedEncryption)
    }
    override suspend fun muteAlarm(probeId: Int?) {
        when (alarmMuteEndpoint(capabilities, probeId)) {
            AlarmMuteEndpoint.GLOBAL -> service.muteAlarm()
            AlarmMuteEndpoint.PROBE -> service.muteProbeAlarm(requireNotNull(probeId))
        }
    }
}

/** Free-Grilly's probe endpoint replaces the submitted probe, so preserve all other fields. */
@Singleton
open class FreeGrillyApiAdapter @Inject constructor(service: GrillyApiService) : RetrofitGrillyDeviceApi(service) {
    override suspend fun updateProbe(patch: ProbePatch) {
        val current = service.getProbes().firstOrNull { it.resolvedId == patch.id }
            ?: error("Probe ${patch.id} not found")
        service.updateProbes(listOf(current.copy(
            name = patch.name ?: current.name,
            targetTemperature = patch.targetTemperatureC ?: current.targetTemperature,
            minimumTemperature = patch.minimumTemperatureC ?: current.minimumTemperature,
            type = patch.type ?: current.type,
            offsetCelcius = patch.calibrationOffsetC ?: current.offsetCelcius,
        )))
    }

    override suspend fun settings(): DeviceSettings = service.getSettings().deviceSettings()
    override suspend fun updateSettings(settings: DeviceSettings) {
        service.updateSettings(ApiDeviceSettings(
            grillName = settings.grillName, wifiSsid = settings.wifiSsid, wifiPassword = settings.wifiPassword,
            temperatureUnit = settings.temperatureUnit, backlightTimeoutMinutes = settings.backlightTimeoutMinutes,
            screenTimeoutMinutes = settings.screenTimeoutMinutes, powerSaving = settings.powerSaving,
        ))
    }
    override suspend fun history(): List<HistorySeries> {
        val response = service.getHistory(); val now = System.currentTimeMillis()
        return response.probes.flatMap { p -> listOf(
            HistorySeries(p.id, HistoryTier.FINE, p.history.mapIndexed { i, raw ->
                HistoryPoint(now - (p.history.size - 1 - i).toLong() * response.intervalSeconds.coerceAtLeast(1) * 1000, raw / 10f) }),
            HistorySeries(p.id, HistoryTier.COARSE, p.historyCoarse.mapIndexed { i, raw ->
                HistoryPoint(now - (p.historyCoarse.size - 1 - i).toLong() * response.coarseIntervalSeconds.coerceAtLeast(1) * 1000, raw / 10f) }),
        ) }
    }
    override suspend fun clearHistory(probeId: Int) = error("History clearing is unsupported")
    override suspend fun uploadFirmware(file: File, adminPassword: String) { service.uploadFirmware(file.firmwarePart()) }
}

@Singleton
class GrillyPlusApiAdapter @Inject constructor(service: GrillyApiService) : RetrofitGrillyDeviceApi(service) {
    override val firmwareUpdateSource = FirmwareUpdateSource("bardesss", "grilly-plus", true)
    override val supportsHistoryGapFill = true
    override suspend fun updateProbe(patch: ProbePatch) {
        service.patchGrillyPlusProbes(listOf(GrillyPlusProbePatch(
            probeId = patch.id, name = patch.name, targetTemperature = patch.targetTemperatureC,
            minimumTemperature = patch.minimumTemperatureC, probeType = patch.type, offsetCelcius = patch.calibrationOffsetC,
        )))
    }
    override suspend fun settings(): DeviceSettings = service.getSettings().deviceSettings()
    override suspend fun updateSettings(settings: DeviceSettings) {
        service.updateSettings(ApiDeviceSettings(
            name = settings.grillName, wifiSsid = settings.wifiSsid, wifiPassword = settings.wifiPassword,
            temperatureUnit = settings.temperatureUnit, backlightTimeoutMinutes = settings.backlightTimeoutMinutes,
            screenTimeoutMinutes = settings.screenTimeoutMinutes, powerSaving = settings.powerSaving,
        ))
    }
    override suspend fun history(): List<HistorySeries> {
        val now = System.currentTimeMillis()
        // Plain /api/history is coarse only in current Grilly+ firmware. Ask each connected
        // probe for its fine tier, retaining the plain response as a safe fallback.
        val coarse = service.getGrillyPlusHistory()
        val fine = service.getGrillStatus().probes.filter { it.connected }.flatMap { probe ->
            runCatching { service.getGrillyPlusHistory(probe.resolvedId).probes }.getOrDefault(emptyList())
        }
        return (coarse.probes + fine).flatMap { p -> listOfNotNull(
            p.fine?.let { HistorySeries(p.probeId, HistoryTier.FINE, GrillyPlusHistoryMapper.samples(p.probeId, it, now).map { s -> HistoryPoint(s.tsMs, s.tempCx10 / 10f) }) },
            p.coarse?.let { HistorySeries(p.probeId, HistoryTier.COARSE, GrillyPlusHistoryMapper.samples(p.probeId, it, now).map { s -> HistoryPoint(s.tsMs, s.tempCx10 / 10f) }) },
        ) }
    }
    override suspend fun clearHistory(probeId: Int) { service.clearGrillyPlusHistory(mapOf("probe_id" to probeId)) }
    override suspend fun uploadFirmware(file: File, adminPassword: String) {
        val auth = adminPassword.takeIf { it.isNotEmpty() }?.let { "Basic " + Base64.encodeToString("admin:$it".toByteArray(), Base64.NO_WRAP) }
        service.uploadGrillyPlusFirmware(authorization = auth, firmware = file.firmwarePart())
    }
}

/** Demo mode exercises the same neutral boundary as a physical device. */
@Singleton
class DemoGrillyDeviceApi @Inject constructor(fake: FakeGrillyApi) : FreeGrillyApiAdapter(fake)
