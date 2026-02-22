@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeCategoryEntity
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories
import com.rubolix.comidia.ui.components.CategoryNode
import com.rubolix.comidia.ui.components.CategoryTreeUtils
import com.rubolix.comidia.ui.components.DialogUnderlay
import androidx.compose.foundation.combinedClickable

@Composable
fun CategoryRecipesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryRecipesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var movingRecipeId by remember { mutableStateOf<Pair<String, String>?>(null) } // recipeId, fromCategoryId
    var viewingRecipeId by remember { mutableStateOf<String?>(null) }
    var pendingRemoval by remember { mutableStateOf<Pair<String, String>?>(null) } // recipeId, categoryId

    val isAnyDialogOpen = showAddDialog || movingRecipeId != null || 
            viewingRecipeId != null || pendingRemoval != null

    fun dismissAll() {
        showAddDialog = false
        movingRecipeId = null
        viewingRecipeId = null
        pendingRemoval = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.currentCategory?.name ?: "Category Recipes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isModified) {
                        IconButton(onClick = { viewModel.saveChanges(onNavigateBack) }) {
                            Icon(Icons.Default.Check, "Save Changes")
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Recipes")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val rootNode = remember(state.currentCategory, state.allCategories) {
                state.currentCategory?.let { current ->
                    CategoryNode(current, CategoryTreeUtils.buildTree(state.allCategories, current.id))
                }
            }

            if (rootNode != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        CategoryRecipeTreeNode(
                            node = rootNode,
                            depth = 0,
                            recipeLinks = state.recipeLinks,
                            allRecipes = state.allRecipes,
                            allCategories = state.allCategories,
                            onIsRuleManaged = { viewModel.isRecipeRuleManaged(it) },
                            onRemove = { rid, cid -> 
                                val conflicts = viewModel.getConflictingTags(rid, cid)
                                if (conflicts.isNotEmpty()) {
                                    pendingRemoval = rid to cid
                                } else {
                                    viewModel.removeRecipeFromCategory(rid, cid)
                                }
                            },
                            onMove = { rid, cid -> movingRecipeId = rid to cid },
                            onView = { viewingRecipeId = it }
                        )
                    }
                }
            }

            if (isAnyDialogOpen) {
                DialogUnderlay(onDismiss = { dismissAll() })
            }
        }
    }

    if (showAddDialog) {
        state.currentCategory?.let { current ->
            BatchRecipeDialog(
                category = current,
                allRecipes = state.allRecipes,
                currentLinks = state.recipeLinks,
                onDismiss = { showAddDialog = false },
                onAdd = { ids -> 
                    viewModel.addRecipesToCategory(ids, current.id)
                    showAddDialog = false
                }
            )
        }
    }

    movingRecipeId?.let { (recipeId, fromId) ->
        val isRuleManaged = viewModel.isRecipeRuleManaged(recipeId)
        MoveRecipeDialog(
            recipeName = state.allRecipes.find { it.recipe.id == recipeId }?.recipe?.name ?: "Recipe",
            allCategories = state.allCategories,
            isCopy = isRuleManaged,
            onDismiss = { movingRecipeId = null },
            onConfirm = { toId ->
                if (isRuleManaged) {
                    viewModel.addRecipesToCategory(listOf(recipeId), toId)
                } else {
                    viewModel.moveRecipe(recipeId, fromId, toId)
                }
                movingRecipeId = null
            }
        )
    }

    viewingRecipeId?.let { id ->
        FullRecipeDialog(
            recipeId = id,
            onDismiss = { viewingRecipeId = null },
            onEdit = { /* Maybe navigate to edit? */ }
        )
    }

    pendingRemoval?.let { (recipeId, categoryId) ->
        val recipeName = state.allRecipes.find { it.recipe.id == recipeId }?.recipe?.name ?: "Recipe"
        val categoryName = state.allCategories.find { it.id == categoryId }?.name ?: "Category"
        val conflictingTags = viewModel.getConflictingTags(recipeId, categoryId)
        
        TagRemovalConfirmationDialog(
            recipeName = recipeName,
            categoryName = categoryName,
            conflictingTags = conflictingTags,
            onDismiss = { pendingRemoval = null },
            onConfirm = { removeTags ->
                viewModel.removeRecipeFromCategory(recipeId, categoryId, removeTags)
                pendingRemoval = null
            }
        )
    }
}

@Composable
private fun TagRemovalConfirmationDialog(
    recipeName: String,
    categoryName: String,
    conflictingTags: List<com.rubolix.comidia.data.local.entity.TagEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove from $categoryName?") },
        text = {
            Column {
                Text("'$recipeName' has tags that automatically link it to this category:")
                Spacer(Modifier.height(8.dp))
                conflictingTags.forEach { tag ->
                    Text("• ${tag.name}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("If you don't remove these tags, the recipe will be added back automatically.")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(true) }) { Text("Remove Recipe & Tags") }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }) { Text("Just Remove Recipe") }
        }
    )
}

@Composable
private fun CategoryRecipeTreeNode(
    node: CategoryNode,
    depth: Int,
    recipeLinks: Map<String, Set<String>>,
    allRecipes: List<RecipeWithTagsAndCategories>,
    allCategories: List<RecipeCategoryEntity>,
    onIsRuleManaged: (String) -> Boolean,
    onRemove: (String, String) -> Unit,
    onMove: (String, String) -> Unit,
    onView: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    val recipesInThisCategory = remember(node.category.id, recipeLinks, allRecipes) {
        allRecipes.filter { rwt ->
            recipeLinks[rwt.recipe.id]?.contains(node.category.id) == true
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                node.category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (recipesInThisCategory.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Badge { Text(recipesInThisCategory.size.toString()) }
            }
        }

        if (expanded) {
            // Subcategories
            node.children.forEach { child ->
                CategoryRecipeTreeNode(
                    child, depth + 1, recipeLinks, allRecipes, allCategories, 
                    onIsRuleManaged, onRemove, onMove, onView
                )
            }
            
            // Recipes
            recipesInThisCategory.forEach { rwt ->
                val isRuleManaged = onIsRuleManaged(rwt.recipe.id)
                RecipeItemRow(
                    recipe = rwt,
                    categoryId = node.category.id,
                    isRuleManaged = isRuleManaged,
                    onRemove = onRemove,
                    onMove = onMove,
                    onView = onView
                )
            }
        }
    }
}

@Composable
private fun RecipeItemRow(
    recipe: RecipeWithTagsAndCategories,
    categoryId: String,
    isRuleManaged: Boolean,
    onRemove: (String, String) -> Unit,
    onMove: (String, String) -> Unit,
    onView: (String) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (!isRuleManaged && (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd)) {
                onRemove(recipe.recipe.id, categoryId)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isRuleManaged,
        enableDismissFromEndToStart = !isRuleManaged,
        backgroundContent = {
            if (!isRuleManaged) {
                val color = MaterialTheme.colorScheme.errorContainer
                Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = {
            var showMenu by remember { mutableStateOf(false) }
            
            ListItem(
                modifier = Modifier.combinedClickable(
                    onClick = { onView(recipe.recipe.id) },
                    onLongClick = { showMenu = true }
                ),
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(recipe.recipe.name)
                        if (isRuleManaged) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.AutoMode, "Managed by smart rule", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                supportingContent = {
                    if (recipe.tags.isNotEmpty()) {
                        Text(recipe.tags.joinToString { it.name }, style = MaterialTheme.typography.bodySmall)
                    }
                },
                leadingContent = { Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(20.dp)) }
            )

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("View Details") },
                    onClick = { showMenu = false; onView(recipe.recipe.id) },
                    leadingIcon = { Icon(Icons.Default.Visibility, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (isRuleManaged) "Copy to another category" else "Move to another category") },
                    onClick = { showMenu = false; onMove(recipe.recipe.id, categoryId) },
                    leadingIcon = { Icon(if (isRuleManaged) Icons.Default.ContentCopy else Icons.Default.DriveFileMove, null) }
                )
                if (!isRuleManaged) {
                    DropdownMenuItem(
                        text = { Text("Remove from this category", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove(recipe.recipe.id, categoryId) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    )
}

@Composable
private fun BatchRecipeDialog(
    category: RecipeCategoryEntity,
    allRecipes: List<RecipeWithTagsAndCategories>,
    currentLinks: Map<String, Set<String>>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = allRecipes.filter { rwt ->
        rwt.recipe.name.contains(searchQuery, ignoreCase = true) &&
        currentLinks[rwt.recipe.id]?.contains(category.id) != true
    }.sortedBy { it.recipe.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add recipes to ${category.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Recipes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { rwt ->
                        val isSelected = rwt.recipe.id in selectedIds
                        ListItem(
                            modifier = Modifier.clickable { 
                                selectedIds = if (isSelected) selectedIds - rwt.recipe.id else selectedIds + rwt.recipe.id
                            },
                            headlineContent = { Text(rwt.recipe.name) },
                            leadingContent = { Checkbox(checked = isSelected, onCheckedChange = null) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedIds.toList()) }, enabled = selectedIds.isNotEmpty()) {
                Text("Add Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MoveRecipeDialog(
    recipeName: String,
    allCategories: List<RecipeCategoryEntity>,
    isCopy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val tree = remember(allCategories) { CategoryTreeUtils.buildTree(allCategories) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCopy) "Copy '$recipeName' to..." else "Move '$recipeName'") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                Text("Select new category:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        CategoryTreeSelection(tree, 0, onConfirm)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategoryTreeSelection(
    nodes: List<CategoryNode>,
    depth: Int,
    onSelect: (String) -> Unit
) {
    nodes.forEach { node ->
        var isExpanded by remember { mutableStateOf(false) }
        
        Column(modifier = Modifier.padding(start = (depth * 12).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.children.isNotEmpty()) {
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(24.dp))
                }
                
                Text(
                    text = node.category.name,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(node.category.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isExpanded) {
                CategoryTreeSelection(node.children, depth + 1, onSelect)
            }
        }
    }
}

@Composable
private fun FullRecipeDialog(
    recipeId: String, 
    onDismiss: () -> Unit, 
    onEdit: () -> Unit = {},
    viewModel: com.rubolix.comidia.ui.screens.recipes.RecipeDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(recipeId) { viewModel.setRecipeId(recipeId) }
    val recipeFull by viewModel.recipeFull.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipeFull?.recipe?.name ?: "Loading...") },
        text = {
            recipeFull?.let { full ->
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i -> Icon(if (i < full.recipe.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    HorizontalDivider()
                    if (full.ingredients.isNotEmpty()) {
                        Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                        full.ingredients.forEach { ing -> Text("• ${if(ing.quantity.isNotBlank()) ing.quantity + " " else ""}${if(ing.unit.isNotBlank()) ing.unit + " " else ""}${ing.name}", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (full.recipe.instructions.isNotBlank()) {
                        Text("Instructions", style = MaterialTheme.typography.titleSmall)
                        Text(full.recipe.instructions, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } ?: CircularProgressIndicator()
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
