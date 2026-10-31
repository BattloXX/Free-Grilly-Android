package org.battlo.freegrilly.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    val appVisible: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    internal fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, GrillyForegroundService::class.java))
        } catch (e: Exception) {
            Log.w("MonitoringController", "Could not start monitoring", e)
        }
    }

    fun stop() {
        context.stopService(Intent(context, GrillyForegroundService::class.java))
    }
}
