package org.battlo.freegrilly.data

import android.util.Log
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.GrillyApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared connection logic used by both [org.battlo.freegrilly.ui.MainViewModel] (auto-reconnect
 * on launch) and [org.battlo.freegrilly.ui.devices.DeviceSelectorViewModel] (manual selection).
 *
 * Strategy:
 * 1. Fast path: try the last-known IP directly (3 s timeout on GET /api/grill).
 * 2. Slow path: if the IP is stale (DHCP changed), run NSD re-discovery filtered to the device
 *    UUID (15 s timeout), then switch to the new IP.
 * 3. Fetch /api/info → update [KnownDevice] (name, capabilities, firmware, lastSeen) → persist
 *    into [DeviceStore] and push capabilities to [GrillyRepository].
 *
 * For demo mode the device UUID/IP is "demo"; calls are short-circuited to avoid
 * hitting the network interceptor with an invalid host.
 */
@Singleton
class DeviceConnector @Inject constructor(
    private val api: GrillyApiService,
    private val nsdDiscovery: NsdDiscovery,
    private val deviceStore: DeviceStore,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val repository: GrillyRepository,
) {
    private val TAG = "DeviceConnector"

    /**
     * Connect to a [KnownDevice] using its saved IP (fast path) with automatic mDNS fallback
     * when the IP is unreachable (slow path).
     */
    suspend fun connect(device: KnownDevice): ConnectResult {
        // Demo device — always "connected", no network needed.
        if (device.ip == "demo" || device.uuid == "demo") return ConnectResult(success = true)

        baseUrlInterceptor.currentHost.value = device.ip
        return if (tryDirectConnect(device.ip)) {
            Log.d(TAG, "Fast-path connect succeeded for ${device.name} at ${device.ip}")
            enrichAndSave(device)
            ConnectResult(success = true)
        } else {
            Log.d(TAG, "Fast-path failed for ${device.name}; starting mDNS re-discovery")
            // Slow path: rediscover by UUID
            nsdDiscovery.startDiscovery(includeOriginal = true, targetUuid = device.uuid)
            val found = withTimeoutOrNull(15_000) {
                nsdDiscovery.state.filterIsInstance<DiscoveryState.Found>().first()
            }
            nsdDiscovery.stopDiscovery()

            if (found == null) {
                Log.w(TAG, "mDNS re-discovery timed out for ${device.uuid}")
                ConnectResult(
                    success = false,
                    errorMessage = "Gerät nicht gefunden. Prüfe ob der Grill eingeschaltet und im selben WLAN ist.",
                )
            } else {
                Log.d(TAG, "mDNS found ${device.name} at new IP ${found.ip}")
                baseUrlInterceptor.currentHost.value = found.ip
                enrichAndSave(device.copy(ip = found.ip))
                ConnectResult(success = true)
            }
        }
    }

    /**
     * Connect by raw IP address (no UUID known — e.g. manual entry or freshly discovered device).
     * Fetches /api/info to build a full [KnownDevice] record.
     */
    suspend fun connectByIp(ip: String): ConnectResult {
        baseUrlInterceptor.currentHost.value = ip
        // 8 s timeout — must exceed OkHttp connectTimeout(5s). Capture the full response here so
        // we can use legacy fields (unique_id, firmware_version) for epiecs firmware that has no
        // /api/info endpoint.
        val status = withTimeoutOrNull(8_000) {
            runCatching { api.getGrillStatus() }.getOrNull()
        }
        if (status == null) {
            return ConnectResult(success = false, errorMessage = "Keine Verbindung zu $ip möglich.")
        }
        // /api/info may not exist (epiecs firmware has no /api/info) — fall back to /api/grill fields
        val info = runCatching { api.getInfo() }.getOrNull()
        val device = KnownDevice(
            uuid = info?.uuid?.ifBlank { status.resolvedUuid } ?: status.resolvedUuid.ifBlank { ip },
            name = info?.name?.ifBlank { status.name } ?: status.name.ifBlank { "Grilleye" },
            ip = ip,
            mdnsHostname = info?.mdnsHostname ?: status.mdnsHostname,
            lastSeen = System.currentTimeMillis(),
            capabilities = info?.capabilities ?: emptyList(),
            firmwareVersion = info?.firmware?.ifBlank { status.resolvedFirmware } ?: status.resolvedFirmware,
        )
        repository.setCapabilities(device.capabilities)
        deviceStore.saveKnownDevice(device)
        deviceStore.setSelectedDevice(device)
        Log.d(TAG, "connectByIp: connected to ${device.name} caps=${device.capabilities}")
        return ConnectResult(success = true)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    private suspend fun tryDirectConnect(ip: String): Boolean {
        baseUrlInterceptor.currentHost.value = ip
        // 8 s timeout — must exceed OkHttp's connectTimeout(5s) + first-byte latency on a
        // freshly-booted device or right after the phone joins the network.
        return withTimeoutOrNull(8_000) {
            runCatching { api.getGrillStatus() }.isSuccess
        } ?: false
    }

    private suspend fun enrichAndSave(device: KnownDevice) {
        val info = runCatching { api.getInfo() }.getOrNull()
        val updated = if (info != null) {
            device.copy(
                name = info.name.ifBlank { device.name },
                ip = baseUrlInterceptor.currentHost.value, // may have changed in slow path
                capabilities = info.capabilities,
                firmwareVersion = info.firmware,
                lastSeen = System.currentTimeMillis(),
            )
        } else {
            device.copy(
                ip = baseUrlInterceptor.currentHost.value,
                lastSeen = System.currentTimeMillis(),
            )
        }
        repository.setCapabilities(updated.capabilities)
        deviceStore.saveKnownDevice(updated)
        deviceStore.setSelectedDevice(updated)
        Log.d(TAG, "enrichAndSave: ${updated.name} caps=${updated.capabilities}")
    }
}

/** Result of a [DeviceConnector] connect operation. */
data class ConnectResult(val success: Boolean, val errorMessage: String? = null)
