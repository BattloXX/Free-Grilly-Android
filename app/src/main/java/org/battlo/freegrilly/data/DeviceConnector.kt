package org.battlo.freegrilly.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.util.Permissions
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
    @ApplicationContext private val context: Context,
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

        // 1) Fast path: the last-known IP.
        baseUrlInterceptor.currentHost.value = device.ip
        if (tryDirectConnect(device.ip)) {
            Log.d(TAG, "Fast-path connect succeeded for ${device.name} at ${device.ip}")
            enrichAndSave(device)
            return ConnectResult(success = true)
        }

        // 2) mDNS hostname (<host>.local): survives DHCP IP changes and is resolved by the
        //    OS, so it works even when the app's own NSD scan can't run. Cheap → try before NSD.
        val mdnsHost = device.mdnsHostname.takeIf { it.isNotBlank() }
            ?.let { if (it.endsWith(".local")) it else "$it.local" }
        if (mdnsHost != null && tryDirectConnect(mdnsHost)) {
            Log.d(TAG, "Hostname-path connect succeeded for ${device.name} at $mdnsHost")
            baseUrlInterceptor.currentHost.value = mdnsHost
            enrichAndSave(device.copy(ip = mdnsHost))
            return ConnectResult(success = true)
        }

        // 3) Slow path: NSD re-discovery by UUID. Requires the Nearby-Wi-Fi runtime
        //    permission on Android 13+; without it discovery silently finds nothing, so
        //    surface an actionable message instead of a generic "not found".
        if (!Permissions.hasNearbyWifi(context)) {
            Log.w(TAG, "Nearby-Wi-Fi permission missing — cannot run mDNS discovery")
            return ConnectResult(
                success = false,
                errorMessage = "Berechtigung „Geräte in der Nähe\" fehlt – ohne sie kann der Grill im WLAN nicht gefunden werden. Bitte in den App-Einstellungen erlauben.",
            )
        }

        Log.d(TAG, "Fast/hostname path failed for ${device.name}; starting mDNS re-discovery")
        nsdDiscovery.startDiscovery(includeOriginal = true, targetUuid = device.uuid)
        val found = withTimeoutOrNull(15_000) {
            nsdDiscovery.state.filterIsInstance<DiscoveryState.Found>().first()
        }
        nsdDiscovery.stopDiscovery()

        return if (found == null) {
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
