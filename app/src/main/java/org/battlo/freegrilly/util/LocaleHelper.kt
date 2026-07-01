package org.battlo.freegrilly.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the app's language preference ("system" / "de" / "en") via the AndroidX per-app
 * language API. Requires [MainActivity][org.battlo.freegrilly.MainActivity] to be an
 * `AppCompatActivity` — only then does AppCompat intercept `attachBaseContext()` to actually
 * rewrite resource resolution, and auto-recreate the running activity on change.
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
