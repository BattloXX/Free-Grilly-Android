package org.battlo.freegrilly.data.food

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val nameDe: String,
    val nameEn: String,
    val category: String,
    val icon: String = "🍖",
    val targetC: Int,
    val minC: Int? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
