@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeFull

@Composable
fun RecipeDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val recipeFull by viewModel.recipeFull.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    recipeFull?.let { full ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(full.recipe.name) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToEdit(full.recipe.id) }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Copy") },
                                    onClick = {
                                        viewModel.copyRecipe()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Archive") },
                                    onClick = {
                                        viewModel.archiveRecipe()
                                        showMenu = false
                                        onNavigateBack()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        viewModel.deleteRecipe()
                                        showMenu = false
                                        onNavigateBack()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Times & servings
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (full.recipe.prepTimeMinutes > 0) {
                        InfoChip("Prep: ${full.recipe.prepTimeMinutes} min")
                    }
                    if (full.recipe.cookTimeMinutes > 0) {
                        InfoChip("Cook: ${full.recipe.cookTimeMinutes} min")
                    }
                    InfoChip("Serves ${full.recipe.servings}")
                }

                // Tags
                if (full.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(full.tags) { tag ->
                            AssistChip(onClick = {}, label = { Text(tag.name) })
                        }
                    }
                }

                // Categories
                if (full.categories.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(full.categories) { cat ->
                            SuggestionChip(onClick = {}, label = { Text(cat.name) })
                        }
                    }
                }

                // Ingredients
                if (full.ingredients.isNotEmpty()) {
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            full.ingredients.forEach { ing ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        "• ",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        buildString {
                                            if (ing.quantity.isNotBlank()) append("${ing.quantity} ")
                                            if (ing.unit.isNotBlank()) append("${ing.unit} ")
                                            append(ing.name)
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Instructions
                if (full.recipe.instructions.isNotBlank()) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            full.recipe.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) }
    )
}
