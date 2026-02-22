@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.ui.screens.recipes.RecipeDetailViewModel

@Composable
fun FullRecipeDialog(
    recipeId: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(recipeId) {
        viewModel.setRecipeId(recipeId)
    }
    val recipeFull by viewModel.recipeFull.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipeFull?.recipe?.name ?: "Loading...") },
        text = {
            recipeFull?.let { full ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    if (i < full.recipe.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (full.recipe.isKidApproved) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Face,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (full.recipe.totalTimeMinutes > 0) {
                                Text(
                                    "Total: ${full.recipe.totalTimeMinutes}m",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (full.recipe.prepTimeMinutes > 0) {
                                Text(
                                    "Prep: ${full.recipe.prepTimeMinutes}m",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    if (full.ingredients.isNotEmpty()) {
                        Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                        full.ingredients.forEach { ing ->
                            Text(
                                "• ${if (ing.quantity.isNotBlank()) ing.quantity + " " else ""}${if (ing.unit.isNotBlank()) ing.unit + " " else ""}${ing.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (full.recipe.instructions.isNotBlank()) {
                        Text("Instructions", style = MaterialTheme.typography.titleSmall)
                        Text(full.recipe.instructions, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {
            Row {
                if (recipeFull != null) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(placeholder) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
