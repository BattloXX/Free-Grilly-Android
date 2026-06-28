package org.battlo.freegrilly.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
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
        val notification = alarmController.buildForegroundNotification()
        startForeground(AlarmController.FOREGROUND_NOTIFICATION_ID, notification)
        repository.startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        repository.stopPolling()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
