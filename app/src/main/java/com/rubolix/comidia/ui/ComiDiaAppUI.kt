package com.rubolix.comidia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rubolix.comidia.ui.navigation.Screen
import com.rubolix.comidia.ui.navigation.bottomNavItems
import com.rubolix.comidia.ui.screens.calendar.CalendarScreen
import com.rubolix.comidia.ui.screens.ingredients.IngredientsScreen
import com.rubolix.comidia.ui.screens.mealplan.MenuScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeDetailScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeEditScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeListScreen
import com.rubolix.comidia.ui.screens.settings.SettingsScreen

@Composable
fun ComiDiaAppUI() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.let { route ->
        !route.contains("/edit") && route != Screen.Settings.route &&
            !(route.startsWith("recipes/") && route != Screen.RecipeList.route)
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
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
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.RecipeList.route) {
                RecipeListScreen(
                    onNavigateToRecipe = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id))
                    },
                    onNavigateToNewRecipe = {
                        navController.navigate(Screen.RecipeEdit.createRoute("new"))
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) {
                RecipeEditScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Ingredients.route) {
                IngredientsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
