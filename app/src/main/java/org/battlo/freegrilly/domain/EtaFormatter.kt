package org.battlo.freegrilly.domain

object EtaFormatter {
    fun format(etaSeconds: Int, locale: String = "de"): String {
        if (etaSeconds == 0) return if (locale == "en") "Done!" else "Fertig!"
        if (etaSeconds < 0) return "—"
        if (etaSeconds < 60) return "< 1 min"
        if (etaSeconds < 3600) {
            val m = etaSeconds / 60
            return "in $m min"
        }
        val h = etaSeconds / 3600
        val m = (etaSeconds % 3600) / 60
        return if (m == 0) "in $h h" else "in $h h $m min"
    }
}
