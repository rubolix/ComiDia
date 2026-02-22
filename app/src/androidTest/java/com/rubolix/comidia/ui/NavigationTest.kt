package com.rubolix.comidia.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.ui.navigation.Screen
import com.rubolix.comidia.ui.navigation.bottomNavItems
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavItems_hasThreeTabs() {
        assertEquals(3, bottomNavItems.size)
    }

    @Test
    fun bottomNavItems_hasCorrectLabels() {
        val labels = bottomNavItems.map { it.label }
        assertEquals(listOf("Menu", "Recipes", "Ingredients"), labels)
    }

    @Test
    fun screenRoutes_areCorrect() {
        assertEquals("menu", Screen.Menu.route)
        assertEquals("calendar", Screen.Calendar.route)
        assertEquals("recipes", Screen.RecipeList.route)
        assertEquals("ingredients", Screen.Ingredients.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("settings/calendar", Screen.SettingsCalendar.route)
        assertEquals("settings/balance", Screen.SettingsBalance.route)
    }

    @Test
    fun recipeDetailRoute_generatesCorrectly() {
        assertEquals("recipes/abc-123", Screen.RecipeDetail.createRoute("abc-123"))
    }

    @Test
    fun recipeEditRoute_generatesCorrectly() {
        assertEquals("recipes/abc-123/edit", Screen.RecipeEdit.createRoute("abc-123"))
    }

    @Test
    fun recipeEditRoute_newRecipe() {
        assertEquals("recipes/new/edit", Screen.RecipeEdit.createRoute("new"))
    }
}
