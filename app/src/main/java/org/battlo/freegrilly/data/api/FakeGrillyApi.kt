package org.battlo.freegrilly.data.api

import okhttp3.MultipartBody
import org.battlo.freegrilly.data.api.models.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class FakeGrillyApi @Inject constructor() : GrillyApiService {

    private var callCount = 0
    private val startTemp = mapOf(1 to 65f, 2 to 45f, 3 to 20f)
    private val targetTemp = mapOf(1 to 72f, 2 to 90f, 3 to 63f)
    private val probeNames = mapOf(1 to "Hähnchenbrust", 2 to "Rinderbrisket", 3 to "Schweinenacken")

    override suspend fun getGrillStatus(): GrillStatusResponse {
        callCount++
        val elapsed = callCount * 0.5f

        val probes = (1..3).map { id ->
            val base = startTemp[id] ?: 20f
            val target = targetTemp[id] ?: 80f
            val current = min(base + elapsed * 0.3f, target + 2f)
            val atTarget = current >= target
            ProbeStatus(
                id = id,
                name = probeNames[id] ?: "Sonde $id",
                connected = true,
                temperature = (current * 10).roundToInt() / 10f,
                targetTemperature = target,
                minimumTemperature = 0f,
                alarm = atTarget,
                etaSeconds = if (atTarget) 0 else ((target - current) / 0.3f * 60).toInt()
            )
        } + listOf(
            ProbeStatus(id = 4, name = "", connected = false, temperature = 0f,
                targetTemperature = 0f, minimumTemperature = 0f, alarm = false, etaSeconds = -1),
            ProbeStatus(id = 5, name = "", connected = false, temperature = 0f,
                targetTemperature = 0f, minimumTemperature = 0f, alarm = false, etaSeconds = -1),
            ProbeStatus(id = 6, name = "", connected = false, temperature = 0f,
                targetTemperature = 0f, minimumTemperature = 0f, alarm = false, etaSeconds = -1),
            ProbeStatus(id = 7, name = "", connected = false, temperature = 0f,
                targetTemperature = 0f, minimumTemperature = 0f, alarm = false, etaSeconds = -1),
            ProbeStatus(id = 8, name = "", connected = false, temperature = 0f,
                targetTemperature = 0f, minimumTemperature = 0f, alarm = false, etaSeconds = -1)
        )

        return GrillStatusResponse(
            name = "Demo Griller",
            uuid = "demo-1234-5678-abcd",
            temperatureUnit = "celcius",
            batteryPercentage = maxOf(20, 100 - callCount / 10),
            batteryCharging = false,
            wifiConnected = true,
            wifiSignal = -55,
            alarmActive = probes.any { it.alarm },
            mdnsHostname = "free-grilly-demo1234",
            probes = probes
        )
    }

    override suspend fun getProbes(): List<ProbeConfig> = (1..3).map { id ->
        ProbeConfig(
            id = id,
            name = probeNames[id] ?: "Sonde $id",
            targetTemperature = targetTemp[id] ?: 80f
        )
    }

    override suspend fun updateProbes(probes: List<ProbeConfig>) = SuccessResponse(success = true)

    override suspend fun getSettings() = DeviceSettings(
        grillName = "Demo Griller",
        temperatureUnit = "celcius",
        backlightTimeoutMinutes = 3,
        screenTimeoutMinutes = 0,
        powerSaving = true,
    )

    override suspend fun updateSettings(settings: DeviceSettings) = SuccessResponse(success = true)

    override suspend fun getHistory(): HistoryResponse {
        val historyPoints = (1..30).map { it * 10 }
        return HistoryResponse(
            intervalSeconds = 10,
            probes = (1..3).map { id ->
                val base = startTemp[id] ?: 20f
                ProbeHistory(
                    id = id,
                    name = probeNames[id] ?: "Sonde $id",
                    history = historyPoints.map { sec ->
                        ((base + sec * 0.003f) * 10).roundToInt()
                    }
                )
            }
        )
    }

    override suspend fun muteAlarm(body: Map<String, String>) = SuccessResponse(success = true)

    override suspend fun getInfo() = DeviceInfo(
        uuid = "demo-1234-5678-abcd",
        name = "Demo Griller",
        firmware = "demo",
        mdnsHostname = "free-grilly-demo1234",
        capabilities = listOf("history", "alarm_mute", "eta", "power_saving")
    )

    override suspend fun getWifiNetworks() = listOf(
        WifiNetwork("HomeNetwork", -45, "WPA2"),
        WifiNetwork("Nachbar_WiFi", -72, "WPA2"),
        WifiNetwork("GästenetzFW", -68, "WPA2")
    )

    // §8 — OTA stub (demo: always returns success, no real upload)
    override suspend fun uploadFirmware(firmware: MultipartBody.Part) =
        SuccessResponse(success = true, message = "Demo OTA: no-op")
}
