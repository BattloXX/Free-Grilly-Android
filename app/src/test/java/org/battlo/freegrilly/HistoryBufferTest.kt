package org.battlo.freegrilly

import org.junit.Assert.*
import org.junit.Test

class HistoryBufferTest {

    @Test
    fun `ring buffer caps at max size`() {
        val buf = ArrayDeque<Float>(10)
        repeat(15) { i ->
            if (buf.size >= 10) buf.removeFirst()
            buf.addLast(i.toFloat())
        }
        assertEquals(10, buf.size)
        assertEquals(5f, buf.first(), 0.001f)
        assertEquals(14f, buf.last(), 0.001f)
    }

    @Test
    fun `ring buffer preserves insertion order`() {
        val buf = ArrayDeque<Float>()
        listOf(10f, 20f, 30f).forEach { buf.addLast(it) }
        assertEquals(listOf(10f, 20f, 30f), buf.toList())
    }

    @Test
    fun `raw decode in buffer`() {
        val rawValues = listOf(850, 855, 862, 870)
        val decoded = rawValues.map { it / 10f }
        assertEquals(85.0f, decoded[0], 0.001f)
        assertEquals(85.5f, decoded[1], 0.001f)
        assertEquals(86.2f, decoded[2], 0.001f)
        assertEquals(87.0f, decoded[3], 0.001f)
    }
}
