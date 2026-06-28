package org.battlo.freegrilly.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.api.models.ProbeConfig
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

    fun assignToProbe(probeId: Int, targetC: Int, minC: Int? = null) {
        viewModelScope.launch {
            grillyRepository.updateProbeConfig(
                ProbeConfig(
                    id = probeId,
                    name = "",
                    targetTemperature = targetC.toFloat(),
                    minimumTemperature = (minC ?: 0).toFloat(),
                )
            )
        }
    }
}
