@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.RecipeCategoryEntity
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.ui.components.CategoryNode
import com.rubolix.comidia.ui.components.DialogUnderlay
import android.content.ClipData
import android.content.ClipDescription

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCategoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategoryRecipes: (String) -> Unit,
    viewModel: RecipeCategoryViewModel = hiltViewModel()
) {
    val tree by viewModel.categoryTree.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val isReorderMode by viewModel.isReorderMode.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val hasSmartRuleMap by viewModel.hasSmartRuleMap.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()

    var showAddDialogParentId by remember { mutableStateOf<String?>(null) }
    var isRootAddActive by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<RecipeCategoryEntity?>(null) }
    var showBatchDialog by remember { mutableStateOf<RecipeCategoryEntity?>(null) }
    var showTagDialog by remember { mutableStateOf<RecipeCategoryEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<RecipeCategoryEntity?>(null) }
    var showSmartRuleDialog by remember { mutableStateOf<RecipeCategoryEntity?>(null) }

    val isAnyDialogOpen = showAddDialogParentId != null || isRootAddActive || 
            showEditDialog != null || showBatchDialog != null || 
            showTagDialog != null || showDeleteConfirm != null || showSmartRuleDialog != null

    LaunchedEffect(Unit) {
        viewModel.collapseAll()
    }

    fun dismissAll() {
        showAddDialogParentId = null
        isRootAddActive = false
        showEditDialog = null
        showBatchDialog = null
        showTagDialog = null
        showDeleteConfirm = null
        showSmartRuleDialog = null
        viewModel.selectCategoryForSmartRule(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReorderMode) "Reorder Categories" else "Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isReorderMode) {
                            viewModel.cancelChanges()
                        } else {
                            viewModel.saveChanges()
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isReorderMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            if (isReorderMode) "Cancel Reorder" else "Back"
                        )
                    }
                },
                actions = {
                    if (isModified) {
                        IconButton(onClick = viewModel::undo) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                        }
                    }
                    
                    if (isReorderMode) {
                        IconButton(onClick = viewModel::saveChanges) {
                            Icon(Icons.Default.Check, "Save Changes")
                        }
                    } else {
                        IconButton(onClick = viewModel::toggleReorderMode) {
                            Icon(Icons.Default.Reorder, "Reorder Mode")
                        }
                        IconButton(onClick = { isRootAddActive = true }) {
                            Icon(Icons.Default.Add, "Add Root Category")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Root Drop Target for moving to top level
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val categoryId = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                            viewModel.reorderCategory(categoryId, null, 0)
                            return true
                        }
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(tree) { index, node ->
                    if (isReorderMode) {
                        DropGap(parentId = null, index = index, onDrop = { droppedId ->
                            viewModel.reorderCategory(droppedId, null, index)
                        })
                    }
                    CategoryTreeItem(
                        node = node,
                        depth = 0,
                        isReorderMode = isReorderMode,
                        hasSmartRule = hasSmartRuleMap[node.category.id] == true,
                        hasSmartRuleMap = hasSmartRuleMap,
                        expandedIds = expandedIds,
                        onToggleExpansion = { viewModel.toggleExpansion(it) },
                        onAddChild = { showAddDialogParentId = it },
                        onEdit = { showEditDialog = it },
                        onDelete = { showDeleteConfirm = it },
                        onManageRecipes = { onNavigateToCategoryRecipes(it.id) },
                        onManageTags = { showTagDialog = it },
                        onManageSmartRule = { 
                            showSmartRuleDialog = it
                            viewModel.selectCategoryForSmartRule(it.id)
                        },
                        onMove = { cat, up -> viewModel.moveCategory(cat, up) },
                        onDrop = { droppedId, targetParentId ->
                            viewModel.reorderCategory(droppedId, targetParentId, 0)
                        },
                        onReorder = { id, p, i -> viewModel.reorderCategory(id, p, i) }
                    )
                    if (isReorderMode && index == tree.lastIndex) {
                        DropGap(parentId = null, index = tree.size, onDrop = { droppedId ->
                            viewModel.reorderCategory(droppedId, null, tree.size)
                        })
                    }
                }
            }

            if (isAnyDialogOpen) {
                DialogUnderlay(onDismiss = { dismissAll() })
            }
        }
    }

    if (isRootAddActive) {
        AddEditCategoryDialog(
            title = "Add Root Category",
            onDismiss = { isRootAddActive = false },
            onConfirm = { name -> viewModel.createCategory(name, null); isRootAddActive = false }
        )
    }

    showAddDialogParentId?.let { parentId ->
        AddEditCategoryDialog(
            title = "Add Subcategory",
            onDismiss = { showAddDialogParentId = null },
            onConfirm = { name -> viewModel.createCategory(name, parentId); showAddDialogParentId = null }
        )
    }

    showEditDialog?.let { cat ->
        AddEditCategoryDialog(
            title = "Edit Category",
            initialName = cat.name,
            onDismiss = { showEditDialog = null },
            onConfirm = { name -> viewModel.updateCategory(cat.copy(name = name)); showEditDialog = null }
        )
    }

    showBatchDialog?.let { cat ->
        BatchRecipeDialog(
            category = cat,
            allRecipes = recipes,
            onDismiss = { showBatchDialog = null },
            onAdd = { ids -> viewModel.batchAddRecipesToCategory(ids, cat.id) },
            onRemove = { ids -> viewModel.batchRemoveRecipesFromCategory(ids, cat.id) }
        )
    }

    showTagDialog?.let { cat ->
        TagAssociationDialog(
            category = cat,
            allTags = tags,
            onDismiss = { showTagDialog = null },
            onToggle = { tagId, active, autoSync -> 
                viewModel.associateTagWithCategory(tagId, if (active) cat.id else null, autoSync)
            },
            onCreateTag = { name, catId -> viewModel.createTag(name, catId) },
            onSyncAll = { viewModel.syncCategoryRecipesFromTags(it) }
        )
    }

    showDeleteConfirm?.let { cat ->
        DeleteCategoryDialog(
            category = cat,
            onDismiss = { showDeleteConfirm = null },
            onConfirm = { viewModel.deleteCategory(cat); showDeleteConfirm = null }
        )
    }

    showSmartRuleDialog?.let { cat ->
        val rule by viewModel.smartRule.collectAsState()
        SmartRuleDialog(
            category = cat,
            existingRule = rule,
            allTags = tags,
            allCategories = tree.map { it.category }, // Simplified list for selection
            onDismiss = { dismissAll() },
            onSaveAndApply = { viewModel.saveAndApplySmartRule(it) },
            onReset = { viewModel.resetSmartRule(cat.id) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryTreeItem(
    node: CategoryNode,
    depth: Int,
    isReorderMode: Boolean,
    hasSmartRule: Boolean,
    hasSmartRuleMap: Map<String, Boolean>,
    expandedIds: Set<String>,
    onToggleExpansion: (String) -> Unit,
    onAddChild: (String) -> Unit,
    onEdit: (RecipeCategoryEntity) -> Unit,
    onDelete: (RecipeCategoryEntity) -> Unit,
    onManageRecipes: (RecipeCategoryEntity) -> Unit,
    onManageTags: (RecipeCategoryEntity) -> Unit,
    onManageSmartRule: (RecipeCategoryEntity) -> Unit,
    onMove: (RecipeCategoryEntity, Boolean) -> Unit,
    onDrop: (String, String?) -> Unit,
    onReorder: (String, String?, Int) -> Unit
) {
    val expanded = expandedIds.contains(node.category.id)
    var isDragOver by remember { mutableStateOf(false) }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false
                val droppedId = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                if (droppedId != node.category.id) {
                    onDrop(droppedId, node.category.id)
                    return true
                }
                return false
            }

            override fun onStarted(event: DragAndDropEvent) {}
            override fun onEntered(event: DragAndDropEvent) { isDragOver = true }
            override fun onExited(event: DragAndDropEvent) { isDragOver = false }
            override fun onEnded(event: DragAndDropEvent) { isDragOver = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dragAndDropTarget
            )
            .background(if (isDragOver) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .then(
                    if (isReorderMode) {
                        Modifier.dragAndDropSource {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    startTransfer(
                                        DragAndDropTransferData(
                                            clipData = ClipData.newPlainText("category_id", node.category.id)
                                        )
                                    )
                                },
                                onDrag = { _, _ -> }
                            )
                        }
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onToggleExpansion(node.category.id) },
                enabled = node.children.isNotEmpty(),
                modifier = Modifier.size(24.dp)
            ) {
                if (node.children.isNotEmpty()) {
                    Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null)
                }
            }
            
            if (isReorderMode) {
                Icon(
                    Icons.Default.DragHandle,
                    null,
                    modifier = Modifier.padding(end = 8.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = node.category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            if (!isReorderMode) {
                IconButton(onClick = { onMove(node.category, true) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowUpward, "Move Up", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onMove(node.category, false) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowDownward, "Move Down", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onAddChild(node.category.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, "Add child", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onEdit(node.category) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(node.category) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { onManageRecipes(node.category) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Restaurant, "Manage recipes", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onManageTags(node.category) }, 
                    modifier = Modifier.size(24.dp),
                    enabled = !hasSmartRule
                ) {
                    Icon(
                        if (hasSmartRule) Icons.Default.LabelOff else Icons.Default.Label, 
                        if (hasSmartRule) "Tags managed by smart rule" else "Associate tags", 
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { onManageSmartRule(node.category) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AutoMode, "Smart rules", modifier = Modifier.size(18.dp))
                }
            } else {
                // Reorder mode: Show Add child and Delete
                IconButton(onClick = { onAddChild(node.category.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, "Add child", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(node.category) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (expanded) {
            node.children.forEachIndexed { index, child ->
                if (isReorderMode) {
                    DropGap(parentId = node.category.id, index = index, onDrop = { droppedId ->
                        onReorder(droppedId, node.category.id, index)
                    })
                }
                CategoryTreeItem(
                    child, depth + 1, isReorderMode, 
                    hasSmartRule = hasSmartRuleMap[child.category.id] == true,
                    hasSmartRuleMap = hasSmartRuleMap,
                    expandedIds = expandedIds,
                    onToggleExpansion = onToggleExpansion,
                    onAddChild, onEdit, onDelete,
                    onManageRecipes, onManageTags, onManageSmartRule, onMove, onDrop, onReorder
                )
                if (isReorderMode && index == node.children.lastIndex) {
                    DropGap(parentId = node.category.id, index = node.children.size, onDrop = { droppedId ->
                        onReorder(droppedId, node.category.id, node.children.size)
                    })
                }
            }
        }
    }
}

@Composable
private fun AddEditCategoryDialog(
    title: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BatchRecipeDialog(
    category: RecipeCategoryEntity,
    allRecipes: List<RecipeWithTagsAndCategories>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
    onRemove: (List<String>) -> Unit
) {
    val currentRecipeIds = allRecipes.filter { rwt -> rwt.categories.any { it.id == category.id } }.map { it.recipe.id }.toSet()
    var selectedIds by remember { mutableStateOf(currentRecipeIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecipes = allRecipes.filter { it.recipe.name.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.recipe.name.lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recipes in ${category.name}") },
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
                    items(filteredRecipes) { rwt ->
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
            Button(onClick = {
                val toAdd = selectedIds - currentRecipeIds
                val toRemove = currentRecipeIds - selectedIds
                if (toAdd.isNotEmpty()) onAdd(toAdd.toList())
                if (toRemove.isNotEmpty()) onRemove(toRemove.toList())
                onDismiss()
            }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun TagAssociationDialog(
    category: RecipeCategoryEntity,
    allTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean, Boolean) -> Unit, // tagId, active, autoSync
    onCreateTag: (String, String) -> Unit,
    onSyncAll: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingToggle by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    
    val filteredTags = remember(allTags, searchQuery, category.id) {
        allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareByDescending<TagEntity> { it.categoryId == category.id }
                .thenBy { it.name.lowercase() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tags for ${category.name}")
                TextButton(onClick = { onSyncAll(category.id) }) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                Text("Any recipe with these tags will be automatically categorized here.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search or create tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank() && allTags.none { it.name.equals(searchQuery, true) }) {
                            IconButton(onClick = { 
                                onCreateTag(searchQuery, category.id)
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Add, "Create Tag")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTags) { tag ->
                        val isAssociated = tag.categoryId == category.id
                        ListItem(
                            modifier = Modifier.clickable { 
                                if (!isAssociated) {
                                    pendingToggle = tag.id to true
                                } else {
                                    onToggle(tag.id, false, false)
                                }
                            },
                            headlineContent = { Text(tag.name) },
                            leadingContent = { Checkbox(checked = isAssociated, onCheckedChange = null) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    pendingToggle?.let { (tagId, active) ->
        AlertDialog(
            onDismissRequest = { pendingToggle = null },
            title = { Text("Sync recipes?") },
            text = { Text("Should all recipes with this tag be added to '${category.name}'?") },
            confirmButton = {
                TextButton(onClick = { 
                    onToggle(tagId, active, true)
                    pendingToggle = null
                }) { Text("Yes, Sync") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onToggle(tagId, active, false)
                    pendingToggle = null
                }) { Text("Just Tag") }
            }
        )
    }
}

@Composable
private fun DeleteCategoryDialog(
    category: RecipeCategoryEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category") },
        text = { Text("Are you sure you want to delete '${category.name}'? This will also delete all its subcategories. Recipes will remain but will be uncategorized.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SmartRuleDialog(
    category: RecipeCategoryEntity,
    existingRule: com.rubolix.comidia.data.local.entity.CategorySmartRuleEntity?,
    allTags: List<TagEntity>,
    allCategories: List<RecipeCategoryEntity>,
    onDismiss: () -> Unit,
    onSaveAndApply: (com.rubolix.comidia.data.local.entity.CategorySmartRuleEntity) -> Unit,
    onReset: () -> Unit
) {
    var minStars by remember(existingRule) { mutableIntStateOf(existingRule?.minStars ?: 0) }
    var kidApproved by remember(existingRule) { mutableStateOf(existingRule?.kidApprovedOnly ?: false) }
    var maxTotal by remember(existingRule) { mutableStateOf(existingRule?.maxTotalTime?.toString() ?: "") }
    var minTotal by remember(existingRule) { mutableStateOf(existingRule?.minTotalTime?.toString() ?: "") }
    var maxPrep by remember(existingRule) { mutableStateOf(existingRule?.maxPrepTime?.toString() ?: "") }
    var selectedTagIds by remember(existingRule) { mutableStateOf(existingRule?.includeTagIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var selectedCatIds by remember(existingRule) { mutableStateOf(existingRule?.includeCategoryIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    fun buildRule() = com.rubolix.comidia.data.local.entity.CategorySmartRuleEntity(
        id = existingRule?.id ?: java.util.UUID.randomUUID().toString(),
        categoryId = category.id,
        minStars = minStars,
        kidApprovedOnly = kidApproved,
        maxTotalTime = maxTotal.toIntOrNull(),
        minTotalTime = minTotal.toIntOrNull(),
        maxPrepTime = maxPrep.toIntOrNull(),
        includeTagIds = selectedTagIds.joinToString(","),
        includeCategoryIds = selectedCatIds.joinToString(","),
        createdAt = existingRule?.createdAt ?: System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Rule: ${category.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Recipes matching these criteria will be automatically added to this category.", style = MaterialTheme.typography.bodySmall)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Min Rating: ", style = MaterialTheme.typography.bodyMedium)
                    repeat(5) { i ->
                        IconButton(onClick = { minStars = i + 1 }, modifier = Modifier.size(32.dp)) {
                            Icon(if (i < minStars) Icons.Default.Star else Icons.Default.StarBorder, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = kidApproved, onCheckedChange = { kidApproved = it })
                    Text("Kid Approved Only", style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maxPrep, onValueChange = { maxPrep = it }, label = { Text("Max Prep (m)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = minTotal, onValueChange = { minTotal = it }, label = { Text("Min Total (m)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = maxTotal, onValueChange = { maxTotal = it }, label = { Text("Max Total (m)") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Text("Include from tags:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    allTags.take(10).forEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTagIds,
                            onClick = { selectedTagIds = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id },
                            label = { Text(tag.name) }
                        )
                    }
                }

                Text("Include from categories (recursive):", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    allCategories.filter { it.id != category.id }.take(10).forEach { cat ->
                        FilterChip(
                            selected = cat.id in selectedCatIds,
                            onClick = { selectedCatIds = if (cat.id in selectedCatIds) selectedCatIds - cat.id else selectedCatIds + cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existingRule != null) {
                    TextButton(onClick = { showRemoveConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Remove Rule")
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = { 
                    onSaveAndApply(buildRule())
                    onDismiss()
                }) { Text("Save & Apply") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove Smart Rule?") },
            text = { Text("This will remove the rule and all recipes currently in this category. Recipes themselves will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        showRemoveConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DropGap(
    parentId: String?,
    index: Int,
    onDrop: (String) -> Unit
) {
    var isDragOver by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isDragOver) 32.dp else 8.dp)
            .padding(vertical = 2.dp)
            .background(if (isDragOver) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = remember {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            isDragOver = false
                            val droppedId = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                            onDrop(droppedId)
                            return true
                        }
                        override fun onStarted(event: DragAndDropEvent) {}
                        override fun onEntered(event: DragAndDropEvent) { isDragOver = true }
                        override fun onExited(event: DragAndDropEvent) { isDragOver = false }
                        override fun onEnded(event: DragAndDropEvent) { isDragOver = false }
                    }
                }
            )
    )
}
