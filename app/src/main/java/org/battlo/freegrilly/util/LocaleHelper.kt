package org.battlo.freegrilly.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the app's language preference ("system" / "de" / "en") via the AndroidX per-app
 * language API. [MainActivity][org.battlo.freegrilly.MainActivity] is a plain
 * `ComponentActivity`, not `AppCompatActivity`, so unlike a typical AppCompat app the running
 * activity is not auto-recreated when the locale changes below API 33 — callers must recreate
 * the current activity themselves after calling [applyLanguage] for the change to take effect
 * immediately.
 */
object LocaleHelper {
    fun applyLanguage(lang: String) {
        val locales = if (lang == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(lang)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
