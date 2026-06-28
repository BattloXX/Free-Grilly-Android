package org.battlo.freegrilly.data.food

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodItem(
    val id: String,
    @SerialName("name_de") val nameDe: String,
    @SerialName("name_en") val nameEn: String,
    val category: String,
    val icon: String = "🥩",
    val cuts: List<String>? = null,
    val recommendation: String? = null,
    val doneness: List<DonenessLevel> = emptyList(),
)

@Serializable
data class DonenessLevel(
    @SerialName("level_de") val levelDe: String,
    @SerialName("level_en") val levelEn: String,
    @SerialName("target_c") val targetC: Int,
    @SerialName("min_c") val minC: Int? = null,
)
