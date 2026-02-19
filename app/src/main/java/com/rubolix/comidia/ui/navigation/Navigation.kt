package com.rubolix.comidia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object RecipeList : Screen("recipes")
    data object RecipeEdit : Screen("recipes/{recipeId}") {
        fun createRoute(recipeId: String) = "recipes/$recipeId"
    }
    data object MealPlan : Screen("mealplan")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val label: String,
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Recipes", Screen.RecipeList, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    BottomNavItem("Meal Plan", Screen.MealPlan, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem("Settings", Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)
