package org.battlo.freegrilly.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.GrillyUiState
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject

@AndroidEntryPoint
class GrillyForegroundService : Service() {

    companion object {
        const val ACTION_STOP = "org.battlo.freegrilly.STOP_MONITORING"
        private const val LOCK_TIMEOUT_MS = 24 * 60 * 60_000L
        private const val NOTIFICATION_INTERVAL_MS = 5_000L
    }

    @Inject lateinit var repository: GrillyRepository
    @Inject lateinit var alarmController: AlarmController
    @Inject lateinit var monitoringController: MonitoringController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (monitoringJob != null) return START_NOT_STICKY
        try {
            val notification = alarmController.buildForegroundNotification()
            ServiceCompat.startForeground(
                this, AlarmController.FOREGROUND_NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0,
            )
            acquireLocks()
        } catch (e: Exception) {
            Log.e("GrillyForegroundService", "Could not start foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        monitoringController.setRunning(true)
        repository.startPolling()
        monitoringJob = serviceScope.launch {
            var autoStop = MonitorAutoStop.State()
            var text = getString(R.string.foreground_notification_text)
            var lastText = text
            var lastNotificationAt = SystemClock.elapsedRealtime()
            launch {
                repository.statusFlow.collect { state ->
                    val now = SystemClock.elapsedRealtime()
                    val status = (state as? GrillyUiState.Connected)?.status
                    autoStop = MonitorAutoStop.update(
                        autoStop, now, state is GrillyUiState.Disconnected,
                        status?.probes?.any { it.connected } == true,
                    )
                    text = when (state) {
                        is GrillyUiState.Connected -> MonitorNotificationText.format(
                            state.status.probes, state.status.temperatureUnit, getString(R.string.monitor_no_probes),
                        )
                        GrillyUiState.Disconnected -> getString(R.string.monitor_disconnected)
                        GrillyUiState.Loading -> getString(R.string.foreground_notification_text)
                        GrillyUiState.Demo -> {
                            stopSelf()
                            return@collect
                        }
                    }
                }
            }
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                if (MonitorAutoStop.shouldStop(autoStop, now)) {
                    stopSelf()
                    break
                }
                if (text != lastText && now - lastNotificationAt >= NOTIFICATION_INTERVAL_MS) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(AlarmController.FOREGROUND_NOTIFICATION_ID, alarmController.buildForegroundNotification(text))
                    lastText = text
                    lastNotificationAt = now
                }
                delay(1_000)
            }
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FreeGrilly:monitor").apply {
            acquire(LOCK_TIMEOUT_MS)
        }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FreeGrilly:monitor").apply {
            acquire()
        }
    }

    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        monitoringController.setRunning(false)
        runCatching { wakeLock?.let { if (it.isHeld) it.release() } }
        runCatching { wifiLock?.let { if (it.isHeld) it.release() } }
        if (!monitoringController.appVisible) repository.stopPolling()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
