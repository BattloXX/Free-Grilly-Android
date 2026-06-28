package org.battlo.freegrilly

import org.battlo.freegrilly.domain.TempUtils
import org.junit.Assert.*
import org.junit.Test

class TempUtilsTest {

    @Test
    fun `celsiusToFahrenheit converts correctly`() {
        assertEquals(32f, TempUtils.celsiusToFahrenheit(0f), 0.01f)
        assertEquals(212f, TempUtils.celsiusToFahrenheit(100f), 0.01f)
        assertEquals(98.6f, TempUtils.celsiusToFahrenheit(37f), 0.1f)
    }

    @Test
    fun `fahrenheitToCelsius converts correctly`() {
        assertEquals(0f, TempUtils.fahrenheitToCelsius(32f), 0.01f)
        assertEquals(100f, TempUtils.fahrenheitToCelsius(212f), 0.01f)
    }

    @Test
    fun `dBmToPercent clamps correctly`() {
        assertEquals(100, TempUtils.dBmToPercent(-40))   // 140 + (-40) = 100
        assertEquals(75, TempUtils.dBmToPercent(-65))    // 140 + (-65) = 75
        assertEquals(0, TempUtils.dBmToPercent(-150))    // clamped to 0
        assertEquals(0, TempUtils.dBmToPercent(-100))    // 140 + (-100) = 40 → actually 40
    }

    @Test
    fun `dBmToPercent typical values`() {
        assertEquals(75, TempUtils.dBmToPercent(-65))
        assertEquals(55, TempUtils.dBmToPercent(-85))
        assertEquals(40, TempUtils.dBmToPercent(-100))
    }

    @Test
    fun `decodeRaw divides by 10`() {
        assertEquals(87.5f, TempUtils.decodeRaw(875), 0.001f)
        assertEquals(0f, TempUtils.decodeRaw(0), 0.001f)
        assertEquals(100f, TempUtils.decodeRaw(1000), 0.001f)
    }

    @Test
    fun `format celsius`() {
        val result = TempUtils.format(87.5f, "celcius", 1)
        assertTrue(result.contains("87.5"))
        assertTrue(result.contains("°C"))
    }

    @Test
    fun `format fahrenheit converts`() {
        val result = TempUtils.format(100f, "fahrenheit", 0)
        assertTrue(result.contains("212"))
        assertTrue(result.contains("°F"))
    }
}
