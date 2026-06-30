package org.battlo.freegrilly.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.food.DonenessLevel
import org.battlo.freegrilly.data.food.FoodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeatLibraryScreen(
    targetProbeId: Int? = null,
    onBack: () -> Unit,
    viewModel: MeatLibraryViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val foods by viewModel.filteredFoods.collectAsStateWithLifecycle()
    var expandedFoodId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meat_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.all)) },
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(foods, key = { it.id }) { food ->
                    FoodItemCard(
                        food = food,
                        targetProbeId = targetProbeId,
                        isExpanded = expandedFoodId == food.id,
                        onToggle = { expandedFoodId = if (expandedFoodId == food.id) null else food.id },
                        onAssign = { doneness ->
                            if (targetProbeId != null) {
                                viewModel.assignToProbe(targetProbeId, food, doneness.targetC, doneness.minC)
                                onBack()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodItemCard(
    food: FoodItem,
    targetProbeId: Int?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAssign: (DonenessLevel) -> Unit,
) {
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(food.icon, style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(food.nameDe, style = MaterialTheme.typography.titleMedium)
                    Text(
                        food.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isExpanded && food.doneness.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                food.doneness.forEach { doneness ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(doneness.levelDe, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${doneness.targetC}°C${doneness.minC?.let { " (ab ${it}°C)" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (targetProbeId != null) {
                            TextButton(onClick = { onAssign(doneness) }) {
                                Text(stringResource(R.string.assign))
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
