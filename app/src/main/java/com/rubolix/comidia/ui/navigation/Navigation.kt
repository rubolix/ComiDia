package com.rubolix.comidia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Menu : Screen("menu")
    data object Calendar : Screen("calendar")
    data object RecipeList : Screen("recipes")
    data object RecipeDetail : Screen("recipes/{recipeId}") {
        fun createRoute(recipeId: String) = "recipes/$recipeId"
    }
    data object RecipeEdit : Screen("recipes/{recipeId}/edit") {
        fun createRoute(recipeId: String) = "recipes/$recipeId/edit"
    }
    data object Ingredients : Screen("ingredients")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val label: String,
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Menu", Screen.Menu, Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu),
    BottomNavItem("Calendar", Screen.Calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem("Recipes", Screen.RecipeList, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    BottomNavItem("Ingredients", Screen.Ingredients, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
)
