package org.battlo.freegrilly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.battlo.freegrilly.service.GrillyForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, GrillyForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
