@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories

@Composable
fun RecipeListScreen(
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToNewRecipe: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val showCategoriesGrid by viewModel.showCategoriesGrid.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search recipes...") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.onSearchQueryChange("")
                                    isSearchActive = false 
                                }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        )
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Recipes") },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        
                        var showSortMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                RecipeSortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        onClick = { 
                                            viewModel.setSortMode(mode)
                                            showSortMenu = false 
                                        },
                                        leadingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }

                        var showFilterMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, "Filter")
                            }
                            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Tags") },
                                    onClick = { viewModel.selectTag(null); showFilterMenu = false },
                                    leadingIcon = { if (selectedTagId == null) Icon(Icons.Default.Check, null) }
                                )
                                tags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = { Text(tag.name) },
                                        onClick = { viewModel.selectTag(tag.id); showFilterMenu = false },
                                        leadingIcon = { if (selectedTagId == tag.id) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Add recipe")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Quick navigation row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showCategoriesGrid && sortMode == RecipeSortMode.LATEST,
                    onClick = { viewModel.setSortMode(RecipeSortMode.LATEST) },
                    label = { Text("Latest") }
                )
                FilterChip(
                    selected = !showCategoriesGrid && sortMode == RecipeSortMode.TOP_HITS,
                    onClick = { viewModel.setSortMode(RecipeSortMode.TOP_HITS) },
                    label = { Text("Top Hits") }
                )
                if (categories.isNotEmpty()) {
                    FilterChip(
                        selected = showCategoriesGrid,
                        onClick = { viewModel.setShowCategoriesGrid(true) },
                        label = { Text("Categories") }
                    )
                }
            }

            if (showCategoriesGrid && categories.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categories) { cat ->
                        CategoryCard(name = cat.name, onClick = { viewModel.selectCategory(cat.id) })
                    }
                }
            } else {
                if (recipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recipes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(recipes, key = { it.recipe.id }) { rwt ->
                            RecipeListItem(rwt = rwt, onClick = { onNavigateToRecipe(rwt.recipe.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun RecipeListItem(rwt: RecipeWithTagsAndCategories, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { 
            Text(rwt.recipe.name, maxLines = 1, overflow = TextOverflow.Ellipsis) 
        },
        supportingContent = {
            Column {
                if (rwt.tags.isNotEmpty()) {
                    Text(
                        rwt.tags.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < rwt.recipe.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (rwt.recipe.isKidApproved) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Face,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null)
        }
    )
}
