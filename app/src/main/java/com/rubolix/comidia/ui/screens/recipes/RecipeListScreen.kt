@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories

enum class RecipeViewMode { LATEST, BY_TAGS, BY_CATEGORIES }

@Composable
fun RecipeListScreen(
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToNewRecipe: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Add recipe")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search recipes...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // View mode toggles
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewMode == RecipeViewMode.LATEST,
                    onClick = { viewModel.setViewMode(RecipeViewMode.LATEST) },
                    label = { Text("Latest") }
                )
                FilterChip(
                    selected = viewMode == RecipeViewMode.BY_TAGS,
                    onClick = { viewModel.setViewMode(RecipeViewMode.BY_TAGS) },
                    label = { Text("By Tags") }
                )
                FilterChip(
                    selected = viewMode == RecipeViewMode.BY_CATEGORIES,
                    onClick = { viewModel.setViewMode(RecipeViewMode.BY_CATEGORIES) },
                    label = { Text("By Category") }
                )
            }

            // Tag filter chips (when in BY_TAGS mode)
            if (viewMode == RecipeViewMode.BY_TAGS && tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        FilterChip(
                            selected = selectedTagFilter == tag.id,
                            onClick = {
                                viewModel.onTagFilterChange(
                                    if (selectedTagFilter == tag.id) null else tag.id
                                )
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recipe list
            if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No recipes yet",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add your first recipe",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (viewMode) {
                    RecipeViewMode.BY_TAGS -> {
                        val grouped = recipes.groupBy { r ->
                            r.tags.firstOrNull()?.name ?: "Untagged"
                        }.toSortedMap()
                        GroupedRecipeList(grouped, onNavigateToRecipe, viewModel)
                    }
                    RecipeViewMode.BY_CATEGORIES -> {
                        val grouped = recipes.groupBy { r ->
                            r.categories.firstOrNull()?.name ?: "Uncategorized"
                        }.toSortedMap()
                        GroupedRecipeList(grouped, onNavigateToRecipe, viewModel)
                    }
                    RecipeViewMode.LATEST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recipes, key = { it.recipe.id }) { recipe ->
                                RecipeCard(recipe, onNavigateToRecipe, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupedRecipeList(
    grouped: Map<String, List<RecipeWithTagsAndCategories>>,
    onNavigateToRecipe: (String) -> Unit,
    viewModel: RecipeListViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (group, recipesInGroup) ->
            item {
                Text(
                    group,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(recipesInGroup, key = { it.recipe.id }) { recipe ->
                RecipeCard(recipe, onNavigateToRecipe, viewModel)
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipeWithDetails: RecipeWithTagsAndCategories,
    onNavigateToRecipe: (String) -> Unit,
    viewModel: RecipeListViewModel
) {
    val recipe = recipeWithDetails.recipe
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = { onNavigateToRecipe(recipe.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (recipeWithDetails.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipeWithDetails.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
                if (recipe.prepTimeMinutes > 0 || recipe.cookTimeMinutes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            if (recipe.prepTimeMinutes > 0) append("Prep: ${recipe.prepTimeMinutes}min")
                            if (recipe.prepTimeMinutes > 0 && recipe.cookTimeMinutes > 0) append(" · ")
                            if (recipe.cookTimeMinutes > 0) append("Cook: ${recipe.cookTimeMinutes}min")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = { viewModel.copyRecipe(recipe.id); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { viewModel.archiveRecipe(recipe.id); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Archive, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { viewModel.deleteRecipe(recipe.id); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}
