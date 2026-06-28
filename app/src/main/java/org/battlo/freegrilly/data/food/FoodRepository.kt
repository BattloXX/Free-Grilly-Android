package org.battlo.freegrilly.data.food

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
) {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private var _bundledFoods: List<FoodItem> = emptyList()

    val bundledFoods: List<FoodItem> get() = _bundledFoods
    val customFoods: Flow<List<FoodEntity>> = foodDao.getAll()
    val favorites: Flow<List<FoodEntity>> = foodDao.getFavorites()

    suspend fun loadBundledFoods() {
        if (_bundledFoods.isNotEmpty()) return
        withContext(Dispatchers.IO) {
            runCatching {
                val json = context.assets.open("foods.json").bufferedReader().readText()
                _bundledFoods = jsonParser.decodeFromString<List<FoodItem>>(json)
            }
        }
    }

    fun getCategories(): List<String> = _bundledFoods.map { it.category }.distinct().sorted()

    fun getFoodsByCategory(category: String): List<FoodItem> =
        _bundledFoods.filter { it.category == category }

    fun searchFoods(query: String, languageCode: String = "de"): List<FoodItem> {
        val q = query.lowercase()
        return _bundledFoods.filter { food ->
            val name = if (languageCode == "en") food.nameEn else food.nameDe
            name.lowercase().contains(q) || food.category.contains(q)
        }
    }

    suspend fun addCustomFood(food: FoodEntity) = foodDao.insert(food)
    suspend fun deleteCustomFood(id: String) = foodDao.deleteById(id)
    suspend fun toggleFavorite(id: String, current: Boolean) = foodDao.setFavorite(id, !current)
}
