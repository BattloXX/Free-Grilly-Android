package org.battlo.freegrilly.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject

@AndroidEntryPoint
class GrillyForegroundService : Service() {

    @Inject lateinit var repository: GrillyRepository
    @Inject lateinit var alarmController: AlarmController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = alarmController.buildForegroundNotification()
            startForeground(AlarmController.FOREGROUND_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("GrillyForegroundService", "Could not start foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        repository.startPolling()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        repository.stopPolling()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
