@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.rubolix.comidia.ui.screens.ingredients

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import com.rubolix.comidia.data.local.entity.ShoppingItem
import kotlin.math.roundToInt

@Composable
fun ShoppingListScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val fullList by viewModel.shoppingList.collectAsState()
    val weekMetadata by viewModel.weekMetadata.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val period by viewModel.period.collectAsState()
    val context = LocalContext.current
    
    val checkAvailabilityItems = fullList["! CHECK AVAILABILITY"] ?: emptyList()
    val hasAvailabilityItems = checkAvailabilityItems.isNotEmpty()
    val isShoppingInProgress by viewModel.isShoppingInProgress.collectAsState()
    
    var selectedTabIndex by remember(hasAvailabilityItems, isShoppingInProgress) { 
        mutableIntStateOf(
            if (isShoppingInProgress) 1 
            else if (hasAvailabilityItems) 0 
            else 1
        ) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showPeriodMenu = true }) {
                            Icon(Icons.Default.DateRange, "Period")
                        }
                        DropdownMenu(expanded = showPeriodMenu, onDismissRequest = { showPeriodMenu = false }) {
                            ShoppingPeriod.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.label) },
                                    onClick = { viewModel.setPeriod(p); showPeriodMenu = false },
                                    leadingIcon = { if (p == period) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, viewModel.getShareText())
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            ShoppingSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = { viewModel.setSortMode(mode); showSortMenu = false },
                                    leadingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = if (hasAvailabilityItems) selectedTabIndex else selectedTabIndex - 1) {
                if (hasAvailabilityItems) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                        Text("Availability", modifier = Modifier.padding(12.dp))
                    }
                }
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                    Text("To Buy", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }) {
                    Text("Purchased", modifier = Modifier.padding(12.dp))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> AvailabilityPassView(
                        items = checkAvailabilityItems,
                        onCommit = { need, have -> 
                            viewModel.commitAvailabilityPass(need, have)
                            selectedTabIndex = 1
                        }
                    )
                    1 -> MainShoppingListView(
                        fullList = fullList,
                        isPurchasedView = false,
                        passDone = weekMetadata?.availabilityPassDone ?: false,
                        sortMode = sortMode,
                        onToggle = { viewModel.togglePurchased(it) },
                        onCompleteShopping = { 
                            viewModel.completeShopping()
                            onNavigateBack()
                        }
                    )
                    2 -> MainShoppingListView(
                        fullList = fullList,
                        isPurchasedView = true,
                        passDone = true,
                        sortMode = sortMode,
                        onToggle = { viewModel.togglePurchased(it) },
                        onCompleteShopping = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityPassView(
    items: List<ShoppingItem>,
    onCommit: (Set<String>, Set<String>) -> Unit
) {
    var unknownItems by remember(items) { mutableStateOf(items.map { it.name }.toSet()) }
    var needToBuy by remember { mutableStateOf(setOf<String>()) }
    var haveIt by remember { mutableStateOf(setOf<String>()) }

    val hasChanged = needToBuy.isNotEmpty() || haveIt.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sort unknown items by dragging them",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    unknownItems.forEach { name ->
                        DraggableItem(
                            name = name,
                            onDrop = { target ->
                                when (target) {
                                    "need" -> {
                                        unknownItems = unknownItems - name
                                        needToBuy = needToBuy + name
                                    }
                                    "have" -> {
                                        unknownItems = unknownItems - name
                                        haveIt = haveIt + name
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.weight(1.5f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DropZone(
                title = "Need To Buy",
                color = MaterialTheme.colorScheme.errorContainer,
                items = needToBuy,
                onItemClick = { name ->
                    needToBuy = needToBuy - name
                    unknownItems = unknownItems + name
                }
            )

            DropZone(
                title = "We Have This",
                color = Color(0xFFE0F2F1),
                items = haveIt,
                onItemClick = { name ->
                    haveIt = haveIt - name
                    unknownItems = unknownItems + name
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onCommit(needToBuy, haveIt) },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasChanged
        ) {
            Text("Confirm & Apply Changes")
        }
    }
}

@Composable
private fun DraggableItem(name: String, onDrop: (String) -> Unit) {
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.1f else 1.0f, label = "scale")
    
    Surface(
        modifier = Modifier
            .offset { offset }
            .zIndex(if (isDragging) 1f else 0f)
            .scale(scale)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (offset.y > 150) { 
                            if (offset.x < 0) onDrop("need") else onDrop("have")
                        }
                        offset = IntOffset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        offset = IntOffset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset = IntOffset(
                            (offset.x + dragAmount.x).roundToInt(),
                            (offset.y + dragAmount.y).roundToInt()
                        )
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = if (isDragging) 8.dp else 2.dp
    ) {
        Text(
            name, 
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), 
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RowScope.DropZone(
    title: String, 
    color: Color, 
    items: Set<String>, 
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            items(items.toList()) { name ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onItemClick(name) },
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.5f)
                ) {
                    Text(name, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun MainShoppingListView(
    fullList: Map<String, List<ShoppingItem>>,
    isPurchasedView: Boolean,
    passDone: Boolean,
    sortMode: ShoppingSortMode,
    onToggle: (ShoppingItem) -> Unit,
    onCompleteShopping: () -> Unit
) {
    var checkSectionExpanded by remember { mutableStateOf(!passDone) }
    val hasPurchasedItems = fullList.values.flatten().any { it.isPurchased }

    Column(Modifier.fillMaxSize()) {
        if (!isPurchasedView && hasPurchasedItems) {
            Button(
                onClick = onCompleteShopping,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.DoneAll, null)
                Spacer(Modifier.width(8.dp))
                Text("Shopping Done?")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            fullList.forEach { (group, items) ->
                val isCheckSection = group.startsWith("!")
                val filteredItems = if (isPurchasedView) items.filter { it.isPurchased } else items.filter { !it.isPurchased }
                
                if (filteredItems.isNotEmpty()) {
                    if (isCheckSection) {
                        item {
                            Surface(
                                onClick = { checkSectionExpanded = !checkSectionExpanded },
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                color = Color.Transparent
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (checkSectionExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, tint = Color(0xFF4F797B))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Check Availability",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF4F797B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (checkSectionExpanded) {
                            items(filteredItems, key = { "check|${it.name}" }) { item ->
                                ShoppingListItem(item = item, showRecipeNames = sortMode != ShoppingSortMode.BY_RECIPE, onToggle = { onToggle(item) })
                            }
                        }
                    } else {
                        item {
                            Text(
                                group,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(filteredItems, key = { "${isPurchasedView}|${group}|${it.name}" }) { item ->
                            ShoppingListItem(item = item, showRecipeNames = sortMode != ShoppingSortMode.BY_RECIPE, onToggle = { onToggle(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListItem(
    item: ShoppingItem,
    showRecipeNames: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = buildString {
                    if (item.quantity.isNotBlank()) append("${item.quantity} ")
                    if (item.unit.isNotBlank()) append("${item.unit} ")
                    append(item.name)
                },
                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null,
                color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            if (showRecipeNames && item.recipeNames.isNotEmpty() && item.recipeNames.first() != "Staple") {
                Text(item.recipeNames.joinToString(", "))
            }
        },
        leadingContent = {
            Checkbox(checked = item.isPurchased, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            if (item.needsChecking && !item.isPurchased) {
                Icon(Icons.Default.QuestionMark, null, tint = Color(0xFF4F797B), modifier = Modifier.size(16.dp))
            }
        }
    )
}
