@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBalance: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isSeeding by viewModel.isSeeding.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        val wasSeeded by viewModel.wasSeeded.collectAsState()
        
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Calendar") },
                supportingContent = { Text("First day of week, default meal types") },
                leadingContent = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.clickable { onNavigateToCalendar() }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Weekly Balance") },
                supportingContent = { Text("Meal variety goals and guidelines") },
                leadingContent = { Icon(Icons.Default.Star, null) },
                modifier = Modifier.clickable { onNavigateToBalance() }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Developer Tools",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Seed Sample Recipes") },
                supportingContent = { Text(if (wasSeeded) "Recipes already added" else "Add 20 family-sized recipes to your library") },
                leadingContent = { 
                    if (isSeeding) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Science, null) 
                },
                trailingContent = {
                    Button(
                        onClick = { viewModel.seedDatabase() },
                        enabled = !isSeeding && !wasSeeded
                    ) {
                        Text(if (wasSeeded) "Seeded" else "Seed")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Reset Categories to Defaults") },
                supportingContent = { Text("Restore the default hierarchical category tree") },
                leadingContent = { Icon(Icons.Default.Refresh, null) },
                trailingContent = {
                    TextButton(onClick = { viewModel.resetCategories() }) {
                        Text("Reset")
                    }
                }
            )
        }
    }
}
