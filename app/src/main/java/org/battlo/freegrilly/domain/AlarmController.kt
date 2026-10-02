package org.battlo.freegrilly.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt
import org.battlo.freegrilly.MainActivity
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.device.model.Probe
import org.battlo.freegrilly.receiver.AlarmMuteReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "grilly_alarm"
        const val NOTIFICATION_ID_BASE = 1001
        const val FOREGROUND_NOTIFICATION_ID = 1000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val tracker = AlarmProbeTracker()

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Applies the firmware's alarm state; it never infers an alarm from temperatures locally. */
    fun onAlarmStateChanged(alarmSounding: Boolean, probes: List<Probe>, unit: String) {
        val probesById = probes.associateBy { it.id }
        val change = tracker.update(alarmSounding, probes.map { AlarmProbe(it.id, it.alarm) })
        change.stopped.forEach(::cancelProbeNotification)
        change.started.forEach { probeId -> probesById[probeId]?.let { showProbeNotification(it, unit) } }
    }

    /** Hides notifications after an explicit mute, retaining firmware state until it clears. */
    fun dismissNotifications(probeId: Int?) {
        if (probeId == null) tracker.activeIds.forEach(::cancelProbeNotification)
        else cancelProbeNotification(probeId)
    }

    private fun showProbeNotification(probe: Probe, unit: String) {
        val notificationId = notificationId(probe.id)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val muteIntent = Intent(context, AlarmMuteReceiver::class.java).apply {
            action = AlarmMuteReceiver.ACTION_MUTE_ALARM
            putExtra(AlarmMuteReceiver.EXTRA_PROBE_ID, probe.id)
        }
        val mutePi = PendingIntent.getBroadcast(
            context, notificationId, muteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val targetC = probe.targetTemperatureC ?: probe.temperatureC ?: return
        val displayTemperature = TempUtils.displayTemp(targetC, unit).roundToInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_thermometer)
            .setContentTitle(context.getString(R.string.alarm_notification_title, probe.name))
            .setContentText(context.getString(
                R.string.alarm_notification_text, probe.name, displayTemperature, TempUtils.unitSymbol(unit),
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .addAction(R.drawable.ic_thermometer, context.getString(R.string.alarm_action_mute), mutePi)
            .addAction(R.drawable.ic_thermometer, context.getString(R.string.alarm_action_open), pi)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun cancelProbeNotification(probeId: Int) = notificationManager.cancel(notificationId(probeId))

    private fun notificationId(probeId: Int) = NOTIFICATION_ID_BASE + probeId

    fun buildForegroundNotification() =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_thermometer)
            .setContentTitle(context.getString(R.string.foreground_notification_title))
            .setContentText(context.getString(R.string.foreground_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
