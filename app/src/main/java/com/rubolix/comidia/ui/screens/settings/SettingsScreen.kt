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

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBalance: () -> Unit
) {
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
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Calendar") },
                supportingContent = { Text("First day of week, default meal types") },
                leadingContent = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.clickable { onNavigateToCalendar() }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Weekly Balance") },
                supportingContent = { Text("Meal variety goals and guidelines") },
                leadingContent = { Icon(Icons.Default.Star, null) },
                modifier = Modifier.clickable { onNavigateToBalance() }
            )
        }
    }
}
