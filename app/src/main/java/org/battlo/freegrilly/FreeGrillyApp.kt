package org.battlo.freegrilly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.battlo.freegrilly.data.DeviceStore
import org.battlo.freegrilly.util.LocaleHelper
import javax.inject.Inject

@HiltAndroidApp
class FreeGrillyApp : Application() {

    @Inject lateinit var deviceStore: DeviceStore

    override fun onCreate() {
        super.onCreate()
        // The stored language preference was never applied to the running app (the setting had
        // no effect) — apply it once at process start so existing installs that already picked
        // a language get it too, before any Activity/Composable reads a string resource.
        val language = runBlocking { deviceStore.appLanguage.first() }
        LocaleHelper.applyLanguage(language)
    }
}
