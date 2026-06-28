package org.battlo.freegrilly.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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

@Singleton
class NsdDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "NsdDiscovery"

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var freeGrillyListener: NsdManager.DiscoveryListener? = null
    private var legacyHttpListener: NsdManager.DiscoveryListener? = null

    /**
     * @param includeOriginal  When true, also scans `_http._tcp` for original epieces firmware.
     * @param targetUuid       If non-null, only `Found` emits for this specific device UUID.
     */
    fun startDiscovery(includeOriginal: Boolean = true, targetUuid: String? = null) {
        _state.value = DiscoveryState.Searching
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

        override fun onServiceLost(service: NsdServiceInfo) {}
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
            _state.value = DiscoveryState.Found(
                ip = ip,
                name = name,
                uuid = uuid,
                firmwareVariant = if (legacy) "original" else "free_grilly",
            )
        }
    }
}
