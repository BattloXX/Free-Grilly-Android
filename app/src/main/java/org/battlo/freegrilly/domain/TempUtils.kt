package org.battlo.freegrilly.domain

object TempUtils {
    fun celsiusToFahrenheit(celsius: Float): Float = celsius * 9f / 5f + 32f
    fun fahrenheitToCelsius(fahrenheit: Float): Float = (fahrenheit - 32f) * 5f / 9f

    fun format(celsius: Float, unit: String, decimals: Int = 1): String {
        val display = if (unit == "fahrenheit") celsiusToFahrenheit(celsius) else celsius
        val symbol = unitSymbol(unit)
        return if (decimals == 0) "${display.toInt()}°$symbol"
        else "%.${decimals}f°$symbol".format(display)
    }

    fun unitSymbol(unit: String): String = if (unit == "fahrenheit") "F" else "C"

    /** Map dBm to % via: percent = 140 + dBm, clamped 0-100 */
    fun dBmToPercent(dBm: Int): Int = (140 + dBm).coerceIn(0, 100)

    /** Decode firmware celsius×10 raw integer to Float °C */
    fun decodeRaw(raw: Int): Float = raw / 10f

    fun displayTemp(celsius: Float, unit: String): Float =
        if (unit == "fahrenheit") celsiusToFahrenheit(celsius) else celsius
}
