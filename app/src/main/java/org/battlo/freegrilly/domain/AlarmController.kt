package org.battlo.freegrilly.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.battlo.freegrilly.MainActivity
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.api.models.ProbeStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "grilly_alarm"
        const val NOTIFICATION_ID = 1001
        const val FOREGROUND_NOTIFICATION_ID = 1000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var alarmActive = false

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Grill-Alarm",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Benachrichtigung wenn eine Sonde die Zieltemperatur erreicht"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun onAlarmActive(alarmingProbes: List<ProbeStatus>) {
        if (alarmActive) return
        alarmActive = true
        val probe = alarmingProbes.firstOrNull() ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_thermometer)
            .setContentTitle("${probe.name} ist fertig!")
            .setContentText("${probe.name} hat ${probe.targetTemperature.toInt()}°C erreicht")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun onAlarmCleared() {
        alarmActive = false
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun buildForegroundNotification() =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_thermometer)
            .setContentTitle("Free-Grilly läuft")
            .setContentText("Temperaturüberwachung aktiv")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
