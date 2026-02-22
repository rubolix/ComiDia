@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.recipes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
        val context = LocalContext.current

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
                // Interactive Star rating & Kid Approved
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            IconButton(
                                onClick = { 
                                    viewModel.updateRating(if (full.recipe.rating == (i + 1).toFloat()) 0f else (i + 1).toFloat()) 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (i < full.recipe.rating.toInt()) Icons.Default.Star
                                    else Icons.Default.StarBorder,
                                    contentDescription = "Rate ${i + 1}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { viewModel.updateKidApproved(!full.recipe.isKidApproved) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "Kid Approved",
                            tint = if (full.recipe.isKidApproved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Times & servings
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (full.recipe.totalTimeMinutes > 0) {
                        InfoChip("Total: ${full.recipe.totalTimeMinutes} min")
                    }
                    if (full.recipe.prepTimeMinutes > 0) {
                        InfoChip("Prep: ${full.recipe.prepTimeMinutes} min")
                    }
                    if (full.recipe.cookTimeMinutes > 0) {
                        InfoChip("Cook: ${full.recipe.cookTimeMinutes} min")
                    }
                    InfoChip("Serves ${full.recipe.servings}")
                }

                // Source URL
                if (!full.recipe.sourceUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(full.recipe.sourceUrl)))
                            } catch(e: Exception) { }
                        }
                    ) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "View source recipe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
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

                // Notes
                if (full.recipe.notes.isNotBlank()) {
                    Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            full.recipe.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
