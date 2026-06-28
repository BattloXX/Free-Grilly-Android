package org.battlo.freegrilly

import org.battlo.freegrilly.data.food.DonenessLevel
import org.battlo.freegrilly.data.food.FoodItem
import org.junit.Assert.*
import org.junit.Test

class FoodMappingTest {

    private val chickenBreast = FoodItem(
        id = "chicken_breast",
        nameDe = "Hähnchenbrust",
        nameEn = "Chicken Breast",
        category = "poultry",
        icon = "🐔",
        doneness = listOf(
            DonenessLevel("Sicher (saftig)", "Safe (juicy)", targetC = 74, minC = 71),
            DonenessLevel("Durchgegart", "Well Done", targetC = 80),
        )
    )

    @Test
    fun `doneness level maps to target temperature`() {
        val level = chickenBreast.doneness.first()
        assertEquals(74, level.targetC)
        assertEquals(71, level.minC)
    }

    @Test
    fun `doneness without min maps to single target`() {
        val level = chickenBreast.doneness.last()
        assertEquals(80, level.targetC)
        assertNull(level.minC)
    }

    @Test
    fun `food item has correct names`() {
        assertEquals("Hähnchenbrust", chickenBreast.nameDe)
        assertEquals("Chicken Breast", chickenBreast.nameEn)
    }

    @Test
    fun `food category matches`() {
        assertEquals("poultry", chickenBreast.category)
    }

    @Test
    fun `minimum temperature defaults to 0 when null`() {
        val level = chickenBreast.doneness.last()
        val minC = level.minC ?: 0
        assertEquals(0, minC)
    }
}
