package org.battlo.freegrilly.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.food.FoodItem
import org.battlo.freegrilly.data.food.FoodRepository
import javax.inject.Inject

@HiltViewModel
class MeatLibraryViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val grillyRepository: GrillyRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    val filteredFoods: StateFlow<List<FoodItem>> = combine(_searchQuery, _selectedCategory) { q, cat ->
        Pair(q, cat)
    }.map { (q, cat) ->
        when {
            q.isNotBlank() -> foodRepository.searchFoods(q)
            cat != null -> foodRepository.getFoodsByCategory(cat)
            else -> foodRepository.bundledFoods
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            foodRepository.loadBundledFoods()
            _categories.value = foodRepository.getCategories()
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setCategory(cat: String?) { _selectedCategory.value = cat }

    /**
     * Assign a food to a probe: name the probe after the food and set its target/minimum.
     * Read-modify-write so the probe's type and thermistor calibration are preserved (assigning
     * a food describes what's cooking, not which physical probe is plugged in).
     */
    fun assignToProbe(probeId: Int, food: FoodItem, targetC: Int, minC: Int? = null) {
        viewModelScope.launch {
            grillyRepository.patchProbe(probeId) {
                it.copy(
                    name = food.nameDe,
                    targetTemperature = targetC.toFloat(),
                    minimumTemperature = (minC ?: 0).toFloat(),
                )
            }
        }
    }
}
