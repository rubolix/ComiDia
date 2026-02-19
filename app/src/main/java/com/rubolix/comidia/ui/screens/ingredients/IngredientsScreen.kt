@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.ingredients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

enum class IngredientSortMode(val label: String) {
    BY_DAY("By Day"),
    ALPHABETICAL("A-Z"),
    BY_CATEGORY("By Type")
}

@Composable
fun IngredientsScreen(
    viewModel: IngredientsViewModel = hiltViewModel()
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val groupedIngredients by viewModel.groupedIngredients.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            IngredientSortMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = { viewModel.setSortMode(mode); showSortMenu = false },
                                    leadingIcon = {
                                        if (mode == sortMode) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }
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
        ) {
            // Week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousWeek) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week")
                }
                Text(
                    text = "${weekStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))} — ${
                        weekStart.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    }",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = viewModel::nextWeek) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week")
                }
            }

            if (groupedIngredients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No meals planned for this week.\nAdd recipes to your menu to see ingredients here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedIngredients.forEach { (groupName, ingredients) ->
                        item {
                            Text(
                                groupName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(ingredients) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    buildString {
                                        if (item.quantity.isNotBlank()) append("${item.quantity} ")
                                        if (item.unit.isNotBlank()) append("${item.unit} ")
                                        append(item.name)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.recipeNames.size > 1) {
                                    Text(
                                        "(${item.recipeNames.size} recipes)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
