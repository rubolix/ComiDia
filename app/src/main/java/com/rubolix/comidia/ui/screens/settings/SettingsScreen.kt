@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rubolix.comidia.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import com.rubolix.comidia.data.local.entity.TagEntity

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var showAddGoal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("ComiDia v1.0", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Meal planning made easy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Divider()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Weekly Meal Plan Goals", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddGoal = true }) {
                        Icon(Icons.Default.Add, "Add goal")
                    }
                }
                Text(
                    "Set targets for your weekly menu (e.g., at least 1 fish meal, max 2 pasta dishes)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (goals.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No goals yet. Tap + to add your first goal.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(goals) { goal ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.description, style = MaterialTheme.typography.bodyMedium)
                            val tagName = tags.find { it.id == goal.tagId }?.name ?: "Any"
                            Text(
                                "${goal.goalType} ${goal.targetCount}/week • Tag: $tagName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = goal.isActive,
                            onCheckedChange = { viewModel.toggleGoal(goal) }
                        )
                        IconButton(onClick = { viewModel.deleteGoal(goal) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Coming soon:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "📋 Shopping list generation",
                    "🤖 Smart recipe suggestions",
                    "☁️ OneDrive sync"
                ).forEach { feature ->
                    Text(
                        feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            tags = tags,
            onDismiss = { showAddGoal = false },
            onAdd = { goal ->
                viewModel.addGoal(goal)
                showAddGoal = false
            }
        )
    }
}

@Composable
private fun AddGoalDialog(
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onAdd: (MealPlanGoalEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var goalType by remember { mutableStateOf("min") }
    var targetCount by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Weekly Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Goal description") },
                    placeholder = { Text("e.g., Fish meals per week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tag picker
                Text("Match recipes with tag:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTagId == tag.id,
                            onClick = { selectedTagId = if (selectedTagId == tag.id) null else tag.id },
                            label = { Text(tag.name) }
                        )
                    }
                }

                // Goal type
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = goalType == "min",
                        onClick = { goalType = "min" },
                        label = { Text("At least") }
                    )
                    FilterChip(
                        selected = goalType == "max",
                        onClick = { goalType = "max" },
                        label = { Text("At most") }
                    )
                }

                OutlinedTextField(
                    value = targetCount,
                    onValueChange = { targetCount = it },
                    label = { Text("Times per week") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        MealPlanGoalEntity(
                            description = description,
                            tagId = selectedTagId,
                            goalType = goalType,
                            targetCount = targetCount.toIntOrNull() ?: 1
                        )
                    )
                },
                enabled = description.isNotBlank() && selectedTagId != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
