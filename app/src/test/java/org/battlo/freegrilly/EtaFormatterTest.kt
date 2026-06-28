package org.battlo.freegrilly

import org.battlo.freegrilly.domain.EtaFormatter
import org.junit.Assert.*
import org.junit.Test

class EtaFormatterTest {

    @Test
    fun `format negative returns dash`() {
        assertEquals("—", EtaFormatter.format(-1))
    }

    @Test
    fun `format zero returns Fertig`() {
        assertEquals("Fertig!", EtaFormatter.format(0))
    }

    @Test
    fun `format under 60s returns under 1 min`() {
        assertEquals("< 1 min", EtaFormatter.format(30))
        assertEquals("< 1 min", EtaFormatter.format(59))
    }

    @Test
    fun `format minutes only`() {
        assertEquals("in 5 min", EtaFormatter.format(300))
        assertEquals("in 59 min", EtaFormatter.format(3540))
    }

    @Test
    fun `format hours and minutes`() {
        assertEquals("in 1 h 30 min", EtaFormatter.format(5400))
        assertEquals("in 2 h", EtaFormatter.format(7200))
    }

    @Test
    fun `format english done`() {
        assertEquals("Done!", EtaFormatter.format(0, "en"))
    }
}
