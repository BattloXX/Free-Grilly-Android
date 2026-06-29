package org.battlo.freegrilly.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var legacyHttpListener: NsdManager.DiscoveryListener? = null
    /** Held during active NSD scans to ensure mDNS multicast packets reach the app. */
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * @param includeOriginal  When true, also scans `_http._tcp` for original epieces firmware.
     * @param targetUuid       If non-null, only `Found` emits for this specific device UUID.
     */
    fun startDiscovery(includeOriginal: Boolean = true, targetUuid: String? = null) {
        _state.value = DiscoveryState.Searching

        // Acquire multicast lock so mDNS packets are not filtered by the Wi-Fi driver.
        // Requires CHANGE_WIFI_MULTICAST_STATE permission in the manifest.
        val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiMgr.createMulticastLock("GrillyNsd").also {
            it.setReferenceCounted(false)
            it.acquire()
        }

        val mgr = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).also { nsdManager = it }

        freeGrillyListener = makeListener(mgr, legacy = false, targetUuid = targetUuid)
        runCatching {
            mgr.discoverServices("_free-grilly._tcp", NsdManager.PROTOCOL_DNS_SD, freeGrillyListener)
        }.onFailure { Log.w(TAG, "free-grilly discovery failed: $it") }

        if (includeOriginal) {
            legacyHttpListener = makeListener(mgr, legacy = true, targetUuid = targetUuid)
            runCatching {
                mgr.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, legacyHttpListener)
            }.onFailure { Log.w(TAG, "http discovery failed: $it") }
        }
    }

    fun stopDiscovery() {
        runCatching { freeGrillyListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        runCatching { legacyHttpListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        freeGrillyListener = null
        legacyHttpListener = null
        _state.value = DiscoveryState.Idle
        _discoveredDevices.value = emptyList()
        // Release multicast lock when scanning stops.
        runCatching { multicastLock?.release() }
        multicastLock = null
    }

    private fun makeListener(
        mgr: NsdManager,
        legacy: Boolean,
        targetUuid: String?,
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
            mgr.resolveService(service, makeResolveListener(legacy, targetUuid))
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // Remove the device from the accumulated list (by service name as a proxy for uuid)
            _discoveredDevices.value = _discoveredDevices.value
                .filterNot { it.name == service.serviceName }
        }
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            if (!legacy) _state.value = DiscoveryState.Failed
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private fun makeResolveListener(
        legacy: Boolean,
        targetUuid: String?,
    ) = object : NsdManager.ResolveListener {
        override fun onResolveFailed(si: NsdServiceInfo, code: Int) {
            Log.w(TAG, "Resolve failed code=$code")
        }
        override fun onServiceResolved(si: NsdServiceInfo) {
            val ip = si.host?.hostAddress ?: return
            val name = si.serviceName ?: if (legacy) "Grilleye" else "Free-Grilly"
            val uuid = si.attributes?.get("uuid")?.let { String(it) } ?: ""
            if (targetUuid != null && uuid.isNotEmpty() && uuid != targetUuid) return
            val variant = if (legacy) "original" else "free_grilly"
            _state.value = DiscoveryState.Found(
                ip = ip,
                name = name,
                uuid = uuid,
                firmwareVariant = variant,
            )
            // §8 — accumulate into the multi-device list (upsert by uuid/ip)
            val discovered = DiscoveredDevice(ip = ip, name = name, uuid = uuid, serviceType = variant)
            _discoveredDevices.value = _discoveredDevices.value
                .filterNot { it.uuid == uuid || it.ip == ip }
                .plus(discovered)
        }
    }
}
