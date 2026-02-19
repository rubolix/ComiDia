@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import java.util.UUID

@Composable
fun WeeklyBalanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Balance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add goal")
            }
        }
    ) { padding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No balance goals yet", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add goals like \"at least 1 fish meal\" or \"max 2 pasta dishes\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onToggle = { viewModel.toggleGoal(goal) },
                        onDelete = { viewModel.deleteGoal(goal) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddGoalDialog(
                tags = tags.map { it.name },
                onDismiss = { showAddDialog = false },
                onAdd = { goal ->
                    viewModel.addGoal(goal)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: MealPlanGoalEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (goal.isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = goal.isActive,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val desc = buildString {
                    if (goal.goalType == "min") append("At least ${goal.targetCount}")
                    else append("At most ${goal.targetCount}")
                    append(" ${goal.description} meal(s)/week")
                }
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    tags: List<String>,
    onDismiss: () -> Unit,
    onAdd: (MealPlanGoalEntity) -> Unit
) {
    var selectedTag by remember { mutableStateOf(tags.firstOrNull() ?: "") }
    var goalType by remember { mutableStateOf("min") }
    var targetText by remember { mutableStateOf("1") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Balance Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tag selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedTag,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = { selectedTag = tag; expanded = false }
                            )
                        }
                    }
                }

                // Goal type
                Text("Goal type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("min" to "At least", "max" to "At most").forEach { (type, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = goalType == type,
                                onClick = { goalType = type }
                            )
                            Text(label)
                        }
                    }
                }

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Count per week") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toIntOrNull()
                    if (selectedTag.isNotBlank() && target != null && target > 0) {
                        onAdd(
                            MealPlanGoalEntity(
                                id = UUID.randomUUID().toString(),
                                description = selectedTag,
                                tagId = selectedTag,
                                goalType = goalType,
                                targetCount = target,
                                isActive = true
                            )
                        )
                    }
                },
                enabled = selectedTag.isNotBlank() && (targetText.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
