@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.ui.components.DialogUnderlay
import com.rubolix.comidia.ui.components.FullRecipeDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecipeListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToNewRecipe: (String?) -> Unit,
    onNavigateToManageCategories: () -> Unit,
    onSelectLeftover: (String, String, String) -> Unit = { _, _, _ -> },
    onSelectFlexible: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val displayCategories by viewModel.displayCategories.collectAsState()
    val sourceLeftovers by viewModel.sourceLeftovers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    
    val currentParentId by viewModel.currentParentCategoryId.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

    val minStars by viewModel.filterMinStars.collectAsState()
    val kidApproved by viewModel.filterKidApproved.collectAsState()
    val madeBefore by viewModel.filterMadeBefore.collectAsState()
    val fast by viewModel.filterFast.collectAsState()
    val shortPrep by viewModel.filterShortPrep.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showTagSearch by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var viewingRecipeId by remember { mutableStateOf<String?>(null) }

    val isPickerMode by viewModel.isPickerMode.collectAsState()
    val pickerDate by viewModel.pickerDate.collectAsState()
    val pickerMealType by viewModel.pickerMealType.collectAsState()
    
    val titleDate = remember(pickerDate) {
        pickerDate?.let {
            try {
                LocalDate.parse(it).format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            } catch (e: Exception) { it }
        }
    }

    val isAnyDialogOpen = showTagSearch || viewingRecipeId != null

    fun dismissAll() {
        showSortMenu = false
        showFilterMenu = false
        showTagSearch = false
        isSearchActive = false
        viewingRecipeId = null
    }

    Scaffold(
        topBar = {
            RecipeListTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearchClose = {
                    viewModel.onSearchQueryChange("")
                    isSearchActive = false
                },
                isPickerMode = isPickerMode,
                pickerMealType = pickerMealType,
                titleDate = titleDate,
                sortMode = sortMode,
                currentParentId = currentParentId,
                selectedCategoryId = selectedCategoryId,
                onNavigateBack = onNavigateBack,
                onNavigateUp = viewModel::navigateUp,
                onSearchActive = { isSearchActive = true },
                onShowSortMenu = { showSortMenu = true },
                onShowFilterMenu = { showFilterMenu = true },
                minStars = minStars,
                kidApproved = kidApproved,
                madeBefore = madeBefore,
                fast = fast,
                shortPrep = shortPrep,
                selectedTagIds = selectedTagIds,
                sortModes = RecipeSortMode.entries.toTypedArray(),
                onSetSortMode = viewModel::setSortMode,
                onManageCategories = onNavigateToManageCategories,
                showSortMenu = showSortMenu,
                onDismissSortMenu = { showSortMenu = false },
                showFilterMenu = showFilterMenu,
                onDismissFilterMenu = { showFilterMenu = false },
                onSetMinStars = viewModel::setMinStars,
                onToggleKidApproved = viewModel::toggleKidApproved,
                onToggleMadeBefore = viewModel::toggleMadeBefore,
                onToggleFast = viewModel::toggleFast,
                onToggleShortPrep = viewModel::toggleShortPrep,
                onShowTagSearch = {
                    showFilterMenu = false
                    showTagSearch = true
                },
                onClearAllFilters = viewModel::clearAllFilters
            )
        },
        floatingActionButton = {
            if (!isPickerMode) {
                FloatingActionButton(onClick = { 
                    val currentCat = selectedCategoryId ?: currentParentId
                    onNavigateToNewRecipe(currentCat)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add recipe")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isPickerMode) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                            Text("Recipes", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                            Text("Leftovers", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }) {
                            Text("Flexible", modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                when (selectedTabIndex) {
                    0 -> RecipeTabContent(
                        isPickerMode = isPickerMode,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        showFilterMenu = showFilterMenu,
                        onShowFilterMenu = { showFilterMenu = true },
                        onDismissFilterMenu = { showFilterMenu = false },
                        minStars = minStars,
                        kidApproved = kidApproved,
                        fast = fast,
                        selectedTagIds = selectedTagIds,
                        onSetMinStars = viewModel::setMinStars,
                        onToggleKidApproved = viewModel::toggleKidApproved,
                        onToggleFast = viewModel::toggleFast,
                        onShowTagSearch = {
                            showFilterMenu = false
                            showTagSearch = true
                        },
                        onClearAllFilters = viewModel::clearAllFilters,
                        showSortMenu = showSortMenu,
                        onShowSortMenu = { showSortMenu = true },
                        onDismissSortMenu = { showSortMenu = false },
                        sortMode = sortMode,
                        onSetSortMode = viewModel::setSortMode,
                        currentParentId = currentParentId,
                        selectedCategoryId = selectedCategoryId,
                        allCategories = allCategories,
                        onSelectCategory = viewModel::selectCategory,
                        displayCategories = displayCategories,
                        recipes = recipes,
                        onNavigateToRecipe = onNavigateToRecipe,
                        onViewDetails = { viewingRecipeId = it },
                        allLimit = viewModel.allLimit.collectAsState().value,
                        topHitsLimit = viewModel.topHitsLimit.collectAsState().value,
                        onLoadMoreAll = viewModel::loadMoreAll,
                        onLoadMoreTopHits = viewModel::loadMoreTopHits,
                        tags = tags,
                        madeBefore = madeBefore,
                        shortPrep = shortPrep,
                        onToggleMadeBefore = viewModel::toggleMadeBefore,
                        onToggleShortPrep = viewModel::toggleShortPrep,
                        onToggleTag = viewModel::toggleTag
                    )
                    1 -> LeftoversTabContent(
                        leftovers = sourceLeftovers,
                        onSelect = { rid -> viewModel.onSelectLeftover(rid, onSelectLeftover) }
                    )
                    2 -> FlexibleTabContent(
                        onSelect = { title, type -> viewModel.onSelectFlexibleMeal(title, type, onSelectFlexible) }
                    )
                }
            }

            if (isAnyDialogOpen) {
                DialogUnderlay(onDismiss = { dismissAll() })
            }
        }
    }

    if (showTagSearch) {
        TagSearchDialog(
            tags = tags,
            selectedTagIds = selectedTagIds,
            onToggle = { viewModel.toggleTag(it) },
            onDismiss = { showTagSearch = false }
        )
    }

    viewingRecipeId?.let { rid ->
        FullRecipeDialog(
            recipeId = rid,
            onDismiss = { viewingRecipeId = null },
            onEdit = { 
                viewingRecipeId = null
                onNavigateToNewRecipe(rid)
            }
        )
    }
}

@Composable
private fun RecipeListTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    isPickerMode: Boolean,
    pickerMealType: String?,
    titleDate: String?,
    sortMode: RecipeSortMode,
    currentParentId: String?,
    selectedCategoryId: String?,
    onNavigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    onSearchActive: () -> Unit,
    onShowSortMenu: () -> Unit,
    onShowFilterMenu: () -> Unit,
    minStars: Int,
    kidApproved: Boolean,
    madeBefore: Boolean,
    fast: Boolean,
    shortPrep: Boolean,
    selectedTagIds: Set<String>,
    sortModes: Array<RecipeSortMode>,
    onSetSortMode: (RecipeSortMode) -> Unit,
    onManageCategories: () -> Unit,
    showSortMenu: Boolean,
    onDismissSortMenu: () -> Unit,
    showFilterMenu: Boolean,
    onDismissFilterMenu: () -> Unit,
    onSetMinStars: (Int) -> Unit,
    onToggleKidApproved: () -> Unit,
    onToggleMadeBefore: () -> Unit,
    onToggleFast: () -> Unit,
    onToggleShortPrep: () -> Unit,
    onShowTagSearch: () -> Unit,
    onClearAllFilters: () -> Unit
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search recipes...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onSearchClose) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                )
            }
        )
    } else {
        TopAppBar(
            title = {
                Column {
                    Text(if (isPickerMode) "Add Recipe" else "Recipes")
                    if (isPickerMode) {
                        Text(
                            text = "${pickerMealType?.replaceFirstChar { it.uppercase() }} — $titleDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                if (isPickerMode) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                } else if (sortMode == RecipeSortMode.BY_CATEGORIES && (currentParentId != null || selectedCategoryId != null)) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            },
            actions = {
                if (!isPickerMode) {
                    IconButton(onClick = onSearchActive) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    
                    Box {
                        IconButton(onClick = onShowSortMenu) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = onDismissSortMenu) {
                            sortModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = { 
                                        onSetSortMode(mode)
                                        onDismissSortMenu()
                                    },
                                    leadingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Manage Categories") },
                                onClick = { 
                                    onDismissSortMenu()
                                    onManageCategories()
                                },
                                leadingIcon = { Icon(Icons.Default.Category, null) }
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = onShowFilterMenu) {
                            val hasFilters = minStars > 0 || kidApproved || madeBefore || fast || shortPrep || selectedTagIds.isNotEmpty()
                            BadgedBox(badge = { if (hasFilters) Badge() }) {
                                Icon(Icons.Default.FilterList, "Filter")
                            }
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = onDismissFilterMenu) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Rating: ")
                                        repeat(5) { i ->
                                            Icon(
                                                if (i < minStars) Icons.Default.Star else Icons.Default.StarBorder,
                                                null,
                                                modifier = Modifier.size(18.dp).clickable { onSetMinStars(i + 1) },
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (minStars > 0) {
                                            IconButton(onClick = { onSetMinStars(0) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                },
                                onClick = { }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Kid Approved") },
                                onClick = onToggleKidApproved,
                                trailingIcon = { if (kidApproved) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Made Before") },
                                onClick = onToggleMadeBefore,
                                trailingIcon = { if (madeBefore) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Fast (<30m)") },
                                onClick = onToggleFast,
                                trailingIcon = { if (fast) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Short Prep (<20m)") },
                                onClick = onToggleShortPrep,
                                trailingIcon = { if (shortPrep) Icon(Icons.Default.Check, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Filter by Tags...") },
                                onClick = onShowTagSearch,
                                trailingIcon = { if (selectedTagIds.isNotEmpty()) Icon(Icons.Default.Check, null) }
                            )
                            if (minStars > 0 || kidApproved || madeBefore || fast || shortPrep || selectedTagIds.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear All Filters", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        onClearAllFilters()
                                        onDismissFilterMenu()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun RecipeTabContent(
    isPickerMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showFilterMenu: Boolean,
    onShowFilterMenu: () -> Unit,
    onDismissFilterMenu: () -> Unit,
    minStars: Int,
    kidApproved: Boolean,
    fast: Boolean,
    selectedTagIds: Set<String>,
    onSetMinStars: (Int) -> Unit,
    onToggleKidApproved: () -> Unit,
    onToggleFast: () -> Unit,
    onShowTagSearch: () -> Unit,
    onClearAllFilters: () -> Unit,
    showSortMenu: Boolean,
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    sortMode: RecipeSortMode,
    onSetSortMode: (RecipeSortMode) -> Unit,
    currentParentId: String?,
    selectedCategoryId: String?,
    allCategories: List<com.rubolix.comidia.data.local.entity.RecipeCategoryEntity>,
    onSelectCategory: (String?) -> Unit,
    displayCategories: List<RecipeListViewModel.CategoryWithCount>,
    recipes: List<RecipeWithTagsAndCategories>,
    onNavigateToRecipe: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    allLimit: Int,
    topHitsLimit: Int,
    onLoadMoreAll: () -> Unit,
    onLoadMoreTopHits: () -> Unit,
    tags: List<TagEntity>,
    madeBefore: Boolean,
    shortPrep: Boolean,
    onToggleMadeBefore: () -> Unit,
    onToggleShortPrep: () -> Unit,
    onToggleTag: (String) -> Unit
) {
    if (isPickerMode) {
        // Inline controls for picker
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.medium
            )
            
            IconButton(onClick = onShowFilterMenu) {
                val hasFilters = minStars > 0 || kidApproved || selectedTagIds.isNotEmpty() || fast
                BadgedBox(badge = { if (hasFilters) Badge() }) {
                    Icon(Icons.Default.FilterList, "Filter")
                }
            }
            
            Box {
                IconButton(onClick = onShowSortMenu) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = onDismissSortMenu) {
                    RecipeSortMode.entries.take(3).forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = { 
                                onSetSortMode(mode)
                                onDismissSortMenu()
                            },
                            leadingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }
        }
        
        DropdownMenu(expanded = showFilterMenu, onDismissRequest = onDismissFilterMenu) {
            DropdownMenuItem(
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating: ")
                        repeat(5) { i ->
                            Icon(
                                if (i < minStars) Icons.Default.Star else Icons.Default.StarBorder,
                                null,
                                modifier = Modifier.size(18.dp).clickable { onSetMinStars(i + 1) },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = { }
            )
            DropdownMenuItem(text = { Text("Kid Approved") }, onClick = onToggleKidApproved, trailingIcon = { if (kidApproved) Icon(Icons.Default.Check, null) })
            DropdownMenuItem(text = { Text("Fast (<30m)") }, onClick = onToggleFast, trailingIcon = { if (fast) Icon(Icons.Default.Check, null) })
            DropdownMenuItem(text = { Text("Filter by Tags...") }, onClick = onShowTagSearch, trailingIcon = { if (selectedTagIds.isNotEmpty()) Icon(Icons.Default.Check, null) })
            if (minStars > 0 || kidApproved || selectedTagIds.isNotEmpty()) {
                DropdownMenuItem(text = { Text("Clear Filters", color = MaterialTheme.colorScheme.error) }, onClick = { onClearAllFilters(); onDismissFilterMenu() })
            }
        }
    }

    // Sorting Selector Row
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecipeSortMode.entries.take(3).forEach { mode ->
            FilterChip(
                selected = sortMode == mode,
                onClick = { onSetSortMode(mode) },
                label = { Text(mode.label) }
            )
        }
    }

    // Breadcrumbs
    if (sortMode == RecipeSortMode.BY_CATEGORIES && (currentParentId != null || selectedCategoryId != null)) {
        BreadcrumbBar(
            currentParentId = currentParentId,
            selectedCategoryId = selectedCategoryId,
            allCategories = allCategories,
            onCategoryClick = onSelectCategory,
            onRootClick = { onClearAllFilters(); onSetSortMode(RecipeSortMode.BY_CATEGORIES) }
        )
    }

    // Active Filters Row
    ActiveFiltersRow(
        minStars = minStars,
        kidApproved = kidApproved,
        madeBefore = madeBefore,
        fast = fast,
        shortPrep = shortPrep,
        selectedTagIds = selectedTagIds,
        allTags = tags,
        selectedCategoryId = selectedCategoryId,
        allCategories = allCategories,
        onClearAll = onClearAllFilters,
        onToggleKid = onToggleKidApproved,
        onToggleMade = onToggleMadeBefore,
        onToggleFast = onToggleFast,
        onToggleShort = onToggleShortPrep,
        onClearStars = { onSetMinStars(0) },
        onRemoveTag = onToggleTag,
        onClearCategory = { onSelectCategory(null) }
    )

    if (sortMode == RecipeSortMode.BY_CATEGORIES && selectedCategoryId == null) {
        // Category Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(displayCategories) { catWithCount ->
                CategoryCard(
                    name = catWithCount.category.name,
                    count = catWithCount.count,
                    onClick = { onSelectCategory(catWithCount.category.id) }
                )
            }
        }
    } else {
        // List View
        if (recipes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recipes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(recipes, key = { it.recipe.id }) { rwt ->
                    RecipeListItem(
                        rwt = rwt, 
                        onClick = { onNavigateToRecipe(rwt.recipe.id) },
                        onViewDetails = { onViewDetails(rwt.recipe.id) }
                    )
                }
                
                if (sortMode == RecipeSortMode.ALL || sortMode == RecipeSortMode.TOP_HITS) {
                    val currentLimit = if (sortMode == RecipeSortMode.ALL) allLimit else topHitsLimit
                    if (recipes.size >= currentLimit) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                TextButton(onClick = if (sortMode == RecipeSortMode.ALL) onLoadMoreAll else onLoadMoreTopHits) {
                                    Text("View More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftoversTabContent(
    leftovers: List<com.rubolix.comidia.data.local.dao.MealPlanDao.RecipeWithUsage>,
    onSelect: (String) -> Unit
) {
    if (leftovers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No leftovers available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val distinctLeftovers = leftovers.distinctBy { it.recipe.id }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(distinctLeftovers) { item ->
                ListItem(
                    modifier = Modifier.clickable { onSelect(item.recipe.id) },
                    headlineContent = { Text(item.recipe.name) },
                    supportingContent = { 
                        val dateLabel = try {
                            LocalDate.parse(item.lastUsedDate).format(DateTimeFormatter.ofPattern("MMM d"))
                        } catch(e: Exception) { item.lastUsedDate }
                        Text("Source: $dateLabel") 
                    },
                    leadingContent = { Icon(Icons.Default.History, null) }
                )
            }
        }
    }
}

@Composable
private fun FlexibleTabContent(
    onSelect: (String, String) -> Unit
) {
    var flexibleTitle by remember { mutableStateOf("") }
    var flexibleType by remember { mutableStateOf("takeout") }
    val types = listOf(
        "takeout" to "Takeout", 
        "eating_out" to "Eating Out", 
        "freezer" to "From Freezer", 
        "pantry" to "Pantry/Simple", 
        "other" to "Other"
    )
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = flexibleTitle, 
            onValueChange = { flexibleTitle = it }, 
            label = { Text("Meal description") }, 
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text("Type", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { (id, label) ->
                FilterChip(
                    selected = flexibleType == id, 
                    onClick = { flexibleType = id }, 
                    label = { Text(label) }
                )
            }
        }
        Button(
            onClick = { 
                val finalTitle = flexibleTitle.ifBlank { types.find { it.first == flexibleType }?.second ?: "Meal" }
                onSelect(finalTitle, flexibleType)
            }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = flexibleTitle.isNotBlank() || flexibleType != "other"
        ) {
            Text("Add Flexible Meal")
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    minStars: Int,
    kidApproved: Boolean,
    madeBefore: Boolean,
    fast: Boolean,
    shortPrep: Boolean,
    selectedTagIds: Set<String>,
    allTags: List<TagEntity>,
    selectedCategoryId: String?,
    allCategories: List<com.rubolix.comidia.data.local.entity.RecipeCategoryEntity>,
    onClearAll: () -> Unit,
    onToggleKid: () -> Unit,
    onToggleMade: () -> Unit,
    onToggleFast: () -> Unit,
    onToggleShort: () -> Unit,
    onClearStars: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onClearCategory: () -> Unit
) {
    val hasOtherFilters = minStars > 0 || kidApproved || madeBefore || fast || shortPrep || selectedTagIds.isNotEmpty()
    if (!hasOtherFilters) return

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            IconButton(onClick = onClearAll, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.FilterListOff, "Clear All", tint = MaterialTheme.colorScheme.error)
            }
        }
        if (selectedCategoryId != null) {
            val catName = if (selectedCategoryId == "virtual_uncategorized") "Uncategorized" 
                          else allCategories.find { it.id == selectedCategoryId }?.name ?: "Category"
            item {
                FilterChip(selected = true, onClick = onClearCategory, label = { Text(catName) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        if (minStars > 0) {
            item {
                FilterChip(selected = true, onClick = onClearStars, label = { Text("$minStars+ Stars") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        if (kidApproved) {
            item {
                FilterChip(selected = true, onClick = onToggleKid, label = { Text("Kid Approved") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        if (madeBefore) {
            item {
                FilterChip(selected = true, onClick = onToggleMade, label = { Text("Made Before") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        if (fast) {
            item {
                FilterChip(selected = true, onClick = onToggleFast, label = { Text("Fast") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        if (shortPrep) {
            item {
                FilterChip(selected = true, onClick = onToggleShort, label = { Text("Short Prep") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
            }
        }
        items(selectedTagIds.toList()) { tid ->
            val tagName = allTags.find { it.id == tid }?.name ?: "..."
            FilterChip(selected = true, onClick = { onRemoveTag(tid) }, label = { Text(tagName) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
        }
    }
}

@Composable
private fun CategoryCard(name: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = if (count == 1) "1 recipe" else "$count recipes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecipeListItem(
    rwt: RecipeWithTagsAndCategories, 
    onClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { 
            Text(rwt.recipe.name, maxLines = 1, overflow = TextOverflow.Ellipsis) 
        },
        supportingContent = {
            Column {
                if (rwt.tags.isNotEmpty()) {
                    Text(
                        rwt.tags.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < rwt.recipe.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (rwt.recipe.isKidApproved) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Face,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onViewDetails) {
                Icon(Icons.Default.ChevronRight, "View details")
            }
        }
    )
}

@Composable
private fun BreadcrumbBar(
    currentParentId: String?,
    selectedCategoryId: String?,
    allCategories: List<com.rubolix.comidia.data.local.entity.RecipeCategoryEntity>,
    onCategoryClick: (String?) -> Unit,
    onRootClick: () -> Unit
) {
    val breadcrumbs = remember(currentParentId, selectedCategoryId, allCategories) {
        val leafId = selectedCategoryId ?: currentParentId
        if (leafId == null) emptyList()
        else {
            val list = mutableListOf<com.rubolix.comidia.data.local.entity.RecipeCategoryEntity>()
            var currentId: String? = leafId
            while (currentId != null) {
                val cat = allCategories.find { it.id == currentId }
                if (cat != null) {
                    list.add(0, cat)
                    currentId = cat.parentId
                } else if (currentId == "virtual_uncategorized") {
                    list.add(0, com.rubolix.comidia.data.local.entity.RecipeCategoryEntity(id = "virtual_uncategorized", name = "Uncategorized"))
                    currentId = null
                } else {
                    currentId = null
                }
            }
            list
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                "All",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onRootClick() }
            )
        }
        breadcrumbs.forEachIndexed { index, cat ->
            item(key = cat.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { onCategoryClick(cat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagSearchDialog(
    tags: List<TagEntity>,
    selectedTagIds: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = tags.filter { it.name.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Tags") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search tags...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { tag ->
                        val isSelected = tag.id in selectedTagIds
                        ListItem(
                            modifier = Modifier.clickable { onToggle(tag.id) },
                            headlineContent = { Text(tag.name) },
                            leadingContent = { 
                                Checkbox(checked = isSelected, onCheckedChange = { onToggle(tag.id) })
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
