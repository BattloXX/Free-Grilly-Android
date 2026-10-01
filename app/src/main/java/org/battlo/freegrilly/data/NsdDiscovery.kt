package org.battlo.freegrilly.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.battlo.freegrilly.data.api.models.DeviceInfo
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DiscoveryState {
    object Idle : DiscoveryState
    object Searching : DiscoveryState
    data class Found(
        val ip: String,
        val name: String,
        val uuid: String,
        /** "free_grilly" for BattloXX fork, "original" for epieces firmware */
        val firmwareVariant: String = "free_grilly",
    ) : DiscoveryState
    object Failed : DiscoveryState
}

/** Represents a single device found during an NSD scan. */
data class DiscoveredDevice(
    val ip: String,
    val name: String,
    val uuid: String,
    val serviceType: String,  // "free_grilly" or "original"
)

/** Pure UUID-first upsert used for services advertised under multiple mDNS types. */
object DiscoveryDedupe {
    fun upsert(existing: List<DiscoveredDevice>, incoming: DiscoveredDevice): List<DiscoveredDevice> =
        existing.filterNot {
            if (incoming.uuid.isNotBlank()) it.uuid == incoming.uuid else it.ip == incoming.ip
        }.plus(incoming)
}

@Singleton
class NsdDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "NsdDiscovery"

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    /**
     * §8 — Accumulated list of all devices found during the current scan session.
     * Devices are added when resolved and removed when [NsdManager] reports them lost.
     * Cleared on [stopDiscovery]. Consumers should use this for a multi-device picker UI.
     */
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var freeGrillyListener: NsdManager.DiscoveryListener? = null
    private var grillyPlusListener: NsdManager.DiscoveryListener? = null
    private var grillyListener: NsdManager.DiscoveryListener? = null
    private var legacyHttpListener: NsdManager.DiscoveryListener? = null
    private val discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolveJobs = mutableListOf<Job>()
    private val infoClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .build()
    private val infoJson = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    @Volatile private var scanGeneration = 0
    /** Held during active NSD scans to ensure mDNS multicast packets reach the app. */
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * @param includeOriginal  When true, also scans `_http._tcp` for original epieces firmware.
     * @param targetUuid       If non-null, only `Found` emits for this specific device UUID.
     */
    fun startDiscovery(includeOriginal: Boolean = true, targetUuid: String? = null) {
        _state.value = DiscoveryState.Searching
        val generation = ++scanGeneration

        // Acquire multicast lock so mDNS packets are not filtered by the Wi-Fi driver.
        // Requires CHANGE_WIFI_MULTICAST_STATE permission in the manifest.
        val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiMgr.createMulticastLock("GrillyNsd").also {
            it.setReferenceCounted(false)
            it.acquire()
        }

        val mgr = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).also { nsdManager = it }

        freeGrillyListener = makeListener(mgr, legacy = false, targetUuid = targetUuid, generation = generation)
        runCatching {
            mgr.discoverServices("_free-grilly._tcp", NsdManager.PROTOCOL_DNS_SD, freeGrillyListener)
        }.onFailure { Log.w(TAG, "free-grilly discovery failed: $it") }
        grillyPlusListener = makeListener(mgr, legacy = false, grillyPlus = true, targetUuid = targetUuid, generation = generation)
        runCatching {
            mgr.discoverServices("_grilly-plus._tcp", NsdManager.PROTOCOL_DNS_SD, grillyPlusListener)
        }.onFailure { Log.w(TAG, "grilly-plus discovery failed: $it") }
        grillyListener = makeListener(mgr, legacy = false, targetUuid = targetUuid, generation = generation)
        runCatching {
            mgr.discoverServices("_grilly._tcp", NsdManager.PROTOCOL_DNS_SD, grillyListener)
        }.onFailure { Log.w(TAG, "grilly discovery failed: $it") }

        if (includeOriginal) {
            legacyHttpListener = makeListener(mgr, legacy = true, targetUuid = targetUuid, generation = generation)
            runCatching {
                mgr.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, legacyHttpListener)
            }.onFailure { Log.w(TAG, "http discovery failed: $it") }
        }
    }

    fun stopDiscovery() {
        ++scanGeneration // Ignore a request that finishes after its scan is stopped.
        runCatching { freeGrillyListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        runCatching { grillyPlusListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        runCatching { grillyListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        runCatching { legacyHttpListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        freeGrillyListener = null
        grillyPlusListener = null
        grillyListener = null
        legacyHttpListener = null
        synchronized(resolveJobs) {
            resolveJobs.forEach { it.cancel() }
            resolveJobs.clear()
        }
        _state.value = DiscoveryState.Idle
        _discoveredDevices.value = emptyList()
        // Release multicast lock when scanning stops.
        runCatching { multicastLock?.release() }
        multicastLock = null
    }

    private fun makeListener(
        mgr: NsdManager,
        legacy: Boolean,
        grillyPlus: Boolean = false,
        targetUuid: String?,
        generation: Int,
    ) = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(serviceType: String) {
            Log.d(TAG, "Discovery started: $serviceType (legacy=$legacy)")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            if (legacy) {
                val n = service.serviceName ?: ""
                val isGrilleye = n.contains("grilleye", ignoreCase = true) ||
                        n.contains("free-grilly", ignoreCase = true) ||
                        n.contains("freegrilly", ignoreCase = true)
                if (!isGrilleye) return
            }
            mgr.resolveService(service, makeResolveListener(legacy, grillyPlus, targetUuid, generation))
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // Do not remove a newer confirmation of the same UUID from another service type.
            val variant = if (legacy) "original" else if (grillyPlus) "grilly_plus" else "free_grilly"
            _discoveredDevices.value = _discoveredDevices.value
                .filterNot { it.name == service.serviceName && it.serviceType == variant }
        }
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            if (!legacy) _state.value = DiscoveryState.Failed
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private fun makeResolveListener(
        legacy: Boolean,
        grillyPlus: Boolean,
        targetUuid: String?,
        generation: Int,
    ) = object : NsdManager.ResolveListener {
        override fun onResolveFailed(si: NsdServiceInfo, code: Int) {
            Log.w(TAG, "Resolve failed code=$code")
        }
        override fun onServiceResolved(si: NsdServiceInfo) {
            // Prefer IPv4: hostAddress may return a scoped IPv6 link-local address (e.g.
            // "fe80::c2f1:abcd%wlan0") which OkHttp cannot use as an HTTP host. Strip the
            // scope suffix; if what remains still looks IPv6 (contains ':'), skip this
            // resolution — the NSD stack will deliver another result with the IPv4 address.
            val rawAddress = si.host?.hostAddress ?: return
            val ip = rawAddress.substringBefore('%').let { addr ->
                if (addr.contains(':')) return  // IPv6 — skip, wait for IPv4 result
                addr
            }
            if (!isPrivateOrLocalIp(ip)) return
            val advertisedName = si.attributes?.get("name")?.let { String(it) } ?: si.serviceName
                ?: if (legacy) "Grilleye" else if (grillyPlus) "Grilly+" else "Free-Grilly"
            val advertisedUuid = si.attributes?.get("uuid")?.let { String(it) } ?: ""
            // TXT is only a cheap prefilter. The /api/info UUID is authoritative when present.
            if (targetUuid != null && advertisedUuid.isNotEmpty() && advertisedUuid != targetUuid) return
            val job = discoveryScope.launch {
                val info = fetchInfo(ip)
                if (generation != scanGeneration) return@launch
                val uuid = info?.resolvedUuid?.ifBlank { advertisedUuid } ?: advertisedUuid
                if (targetUuid != null && uuid.isNotEmpty() && uuid != targetUuid) return@launch
                val name = info?.name?.ifBlank { advertisedName } ?: advertisedName
                val variant = when {
                    legacy -> "original"
                    info?.isGrillyPlus == true || grillyPlus -> "grilly_plus"
                    else -> "free_grilly"
                }
                _state.value = DiscoveryState.Found(ip, name, uuid, variant)
                val discovered = DiscoveredDevice(ip, name, uuid, variant)
                _discoveredDevices.value = DiscoveryDedupe.upsert(_discoveredDevices.value, discovered)
            }
            synchronized(resolveJobs) { resolveJobs += job }
        }
    }

    /** This intentionally bypasses [BaseUrlInterceptor] so scanning cannot change the active device. */
    private fun fetchInfo(ip: String): DeviceInfo? = runCatching {
        infoClient.newCall(Request.Builder().url("http://$ip/api/info").build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            infoJson.decodeFromString<DeviceInfo>(body)
        }
    }.onFailure { Log.d(TAG, "Discovery info request failed for $ip: $it") }.getOrNull()

    private fun isPrivateOrLocalIp(ip: String): Boolean {
        val octets = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        val (a, b, _, _) = octets
        return a == 10 || a == 172 && b in 16..31 || a == 192 && b == 168 ||
            a == 169 && b == 254 || a == 127
    }
}
