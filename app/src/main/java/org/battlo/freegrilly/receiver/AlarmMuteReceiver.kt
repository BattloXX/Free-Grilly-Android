package org.battlo.freegrilly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.di.ApplicationScope
import org.battlo.freegrilly.domain.AlarmController
import javax.inject.Inject

@AndroidEntryPoint
class AlarmMuteReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: GrillyRepository
    @Inject lateinit var alarmController: AlarmController
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MUTE_ALARM) return
        val probeId = intent.getIntExtra(EXTRA_PROBE_ID, NO_PROBE_ID)
        if (probeId != NO_PROBE_ID) {
            alarmController.dismissNotifications(probeId)
            applicationScope.launch { repository.muteAlarm(probeId) }
        }
    }

    companion object {
        const val ACTION_MUTE_ALARM = "org.battlo.freegrilly.action.MUTE_ALARM"
        const val EXTRA_PROBE_ID = "probe_id"
        private const val NO_PROBE_ID = Int.MIN_VALUE
    }
}
