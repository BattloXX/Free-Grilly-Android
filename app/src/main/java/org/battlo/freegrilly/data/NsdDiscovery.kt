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
    data class Found(val ip: String, val name: String, val uuid: String) : DiscoveryState
    object Failed : DiscoveryState
}

@Singleton
class NsdDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "NsdDiscovery"
    private val SERVICE_TYPE = "_free-grilly._tcp"

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        _state.value = DiscoveryState.Searching
        val mgr = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager = mgr

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                mgr.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, code: Int) {
                        Log.w(TAG, "Resolve failed: $code")
                    }
                    override fun onServiceResolved(si: NsdServiceInfo) {
                        val ip = si.host?.hostAddress ?: return
                        val name = si.serviceName ?: "Free-Grilly"
                        val uuid = si.attributes["uuid"]?.let { String(it) } ?: ""
                        _state.value = DiscoveryState.Found(ip = ip, name = name, uuid = uuid)
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _state.value = DiscoveryState.Failed
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        runCatching {
            mgr.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }.onFailure {
            _state.value = DiscoveryState.Failed
        }
    }

    fun stopDiscovery() {
        runCatching { discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        discoveryListener = null
        _state.value = DiscoveryState.Idle
    }
}
