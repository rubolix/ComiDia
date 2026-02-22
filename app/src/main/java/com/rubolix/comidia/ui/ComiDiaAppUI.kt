@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import com.rubolix.comidia.ui.navigation.Screen
import com.rubolix.comidia.ui.navigation.bottomNavItems
import com.rubolix.comidia.ui.screens.calendar.CalendarScreen
import com.rubolix.comidia.ui.screens.ingredients.IngredientsScreen
import com.rubolix.comidia.ui.screens.ingredients.ShoppingListScreen
import com.rubolix.comidia.ui.screens.ingredients.ShoppingListViewModel
import com.rubolix.comidia.ui.screens.ingredients.StaplesScreen
import com.rubolix.comidia.ui.screens.mealplan.MenuScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeDetailScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeEditScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeListScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeCategoryScreen
import com.rubolix.comidia.ui.screens.recipes.CategoryRecipesScreen
import com.rubolix.comidia.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun ComiDiaAppUI() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val shoppingViewModel: ShoppingListViewModel = hiltViewModel()
    val isShoppingInProgress by shoppingViewModel.isShoppingInProgress.collectAsState()

    val isMainView = currentDestination?.route?.split("?")?.firstOrNull() in listOf(
        Screen.Menu.route,
        Screen.RecipeList.route,
        Screen.Ingredients.route
    )

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.AutoMirrored.Filled.MenuOpen, "Close Menu")
                    }
                    Text(
                        "ComiDia", 
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Menu") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Menu.route } == true,
                    onClick = {
                        navController.navigate(Screen.Menu.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.RestaurantMenu, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Recipes") },
                    selected = currentDestination?.hierarchy?.any { it.route?.startsWith(Screen.RecipeList.route) == true } == true,
                    onClick = {
                        navController.navigate(Screen.RecipeList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Restaurant, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Ingredients") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Ingredients.route } == true,
                    onClick = {
                        navController.navigate(Screen.Ingredients.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.ShoppingCart, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Categories") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.ManageCategories.route } == true,
                    onClick = {
                        navController.navigate(Screen.ManageCategories.route)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Category, null) }
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Staples") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Staples.route } == true,
                    onClick = {
                        navController.navigate(Screen.Staples.route)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Inventory, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                    onClick = {
                        navController.navigate(Screen.Settings.route)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isMainView) {
                    CenterAlignedTopAppBar(
                        title = { 
                            val route = currentDestination?.route?.split("?")?.firstOrNull()
                            Text(
                                when (route) {
                                    Screen.Menu.route -> "Meal Plan"
                                    Screen.RecipeList.route -> "Recipes"
                                    Screen.Ingredients.route -> "Ingredients"
                                    Screen.ManageCategories.route -> "Categories"
                                    else -> "ComiDia"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        },
                        actions = {
                            if (currentDestination?.route == Screen.Menu.route) {
                                IconButton(onClick = { navController.navigate(Screen.Calendar.route) }) {
                                    Icon(Icons.Default.CalendarMonth, "Select Week")
                                }
                            }
                            if (isShoppingInProgress) {
                                IconButton(onClick = { navController.navigate(Screen.ShoppingList.route) }) {
                                    BadgedBox(badge = { Badge { Text("!") } }) {
                                        Icon(Icons.Default.ShoppingCart, "Shopping", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isMainView) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route?.startsWith(item.screen.route) == true } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Menu.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Menu.route) {
                    MenuScreen(
                        navController = navController,
                        onNavigateToRecipeEdit = { id ->
                            navController.navigate(Screen.RecipeEdit.createRoute(id))
                        }
                    )
                }
                composable(Screen.Calendar.route) {
                    CalendarScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.RecipeList.route) {
                    RecipeListScreen(
                        onNavigateToRecipe = { id ->
                            navController.navigate(Screen.RecipeDetail.createRoute(id))
                        },
                        onNavigateToNewRecipe = { catId ->
                            navController.navigate(Screen.RecipeEdit.createRoute("new", catId))
                        },
                        onNavigateToManageCategories = {
                            navController.navigate(Screen.ManageCategories.route)
                        }
                    )
                }
                composable(
                    Screen.RecipeDetail.route,
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) {
                    RecipeDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { id ->
                            navController.navigate(Screen.RecipeEdit.createRoute(id))
                        }
                    )
                }
                composable(
                    Screen.RecipeEdit.route,
                    arguments = listOf(
                        navArgument("recipeId") { type = NavType.StringType },
                        navArgument("initialCategoryId") { 
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    RecipeEditScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Ingredients.route) {
                    IngredientsScreen(
                        onNavigateToShoppingList = { navController.navigate(Screen.ShoppingList.route) },
                        onNavigateToStaples = { navController.navigate(Screen.Staples.route) }
                    )
                }
                composable(Screen.ShoppingList.route) {
                    ShoppingListScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Staples.route) {
                    StaplesScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.ManageCategories.route) {
                    RecipeCategoryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCategoryRecipes = { id ->
                            navController.navigate(Screen.CategoryRecipes.createRoute(id))
                        }
                    )
                }
                composable(
                    Screen.CategoryRecipes.route,
                    arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                ) {
                    CategoryRecipesScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCalendar = { navController.navigate(Screen.SettingsCalendar.route) },
                        onNavigateToBalance = { navController.navigate(Screen.SettingsBalance.route) }
                    )
                }
                composable(Screen.SettingsCalendar.route) {
                    com.rubolix.comidia.ui.screens.settings.CalendarSettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.SettingsBalance.route) {
                    com.rubolix.comidia.ui.screens.settings.WeeklyBalanceScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
