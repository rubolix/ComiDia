package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class SeedDatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.seedDatabase(db)
        }
    }
}

object DatabaseSeeder {

    fun seedDatabase(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF;")
        db.beginTransaction()
        try {
            // 1. Seed Categories first so they exist for tags and recipes to reference
            seedDefaultCategories(db)
            
            // 2. Seed Standard Tags
            seedStandardTags(db)
            
            // 3. Link Tags to Categories (name matching)
            linkTagsToCategoriesByMatch(db)

            // Clear existing recipe data to avoid duplicates with different ID generation logic
            db.execSQL("DELETE FROM recipe_category_cross_ref")
            db.execSQL("DELETE FROM recipe_tag_cross_ref")
            db.execSQL("DELETE FROM ingredients")
            db.execSQL("DELETE FROM recipes")
            
            // 4. Seed sample recipes
            data class IngredientData(
            val name: String,
            val qty: String,
            val unit: String,
            val category: String = ""
        )

        data class Recipe(
            val name: String,
            val instructions: String,
            val servings: Int,
            val prepMin: Int,
            val cookMin: Int,
            val ingredients: List<IngredientData>,
            val tagNames: List<String>,
            val sourceUrl: String = "",
            val rating: Float = 0f,
            val notes: String = ""
        )

        val recipes = listOf(
            Recipe(
                "Honey Garlic Chicken Thighs",
                "1. Season chicken thighs with salt and pepper.\n2. Sear in a hot skillet until golden, about 4 min per side.\n3. Mix honey, soy sauce, garlic, and ginger. Pour over chicken.\n4. Bake at 400°F for 25 minutes until internal temp reaches 165°F.",
                5, 10, 25,
                listOf(
                    IngredientData("chicken thighs", "10", "pieces", "Protein"),
                    IngredientData("honey", "1/3", "cup", "Pantry"),
                    IngredientData("soy sauce", "2", "tbsp", "Pantry"),
                    IngredientData("garlic", "3", "cloves", "Produce")
                ),
                listOf("Main Dish", "Meat", "Quick"),
                rating = 4.5f
            ),
            Recipe(
                "One-Pot Beef Chili",
                "1. Brown ground beef in a large pot.\n2. Add diced onion, bell pepper, and garlic.\n3. Stir in crushed tomatoes, beans, and spices.\n4. Simmer for 30 minutes.",
                5, 15, 35,
                listOf(
                    IngredientData("ground beef", "2", "lbs", "Protein"),
                    IngredientData("kidney beans", "2", "cans", "Canned"),
                    IngredientData("onion", "1", "large", "Produce"),
                    IngredientData("bell pepper", "1", "large", "Produce"),
                    IngredientData("crushed tomatoes", "28", "oz", "Canned")
                ),
                listOf("Main Dish", "Meat", "Batch Cooking", "Spoon Dish"),
                rating = 4f
            ),
            Recipe(
                "Lentil Soup",
                "1. Sauté onions, carrots, and celery.\n2. Add lentils, vegetable broth, and spices.\n3. Simmer until lentils are tender, about 25-30 minutes.",
                4, 10, 30,
                listOf(
                    IngredientData("dried lentils", "1", "cup", "Pantry"),
                    IngredientData("vegetable broth", "4", "cups", "Pantry"),
                    IngredientData("onion", "1", "", "Produce"),
                    IngredientData("carrot", "2", "", "Produce"),
                    IngredientData("celery", "2", "stalks", "Produce")
                ),
                listOf("Main Dish", "Vegetarian", "Vegan", "Spoon Dish"),
                rating = 4.2f
            ),
            Recipe(
                "Spaghetti Carbonara",
                "1. Boil pasta.\n2. Fry guanciale or pancetta.\n3. Mix eggs and pecorino cheese.\n4. Combine pasta with meat, then quickly toss with egg mixture away from heat.",
                2, 5, 15,
                listOf(
                    IngredientData("spaghetti", "250", "g", "Pantry"),
                    IngredientData("eggs", "2", "large", "Dairy"),
                    IngredientData("pecorino romano", "50", "g", "Dairy"),
                    IngredientData("guanciale", "100", "g", "Meat")
                ),
                listOf("Main Dish", "Pasta", "Meat", "Quick"),
                rating = 4.8f
            ),
            Recipe(
                "Green Salad with Lemon Vinaigrette",
                "1. Wash and dry greens.\n2. Whisk olive oil, lemon juice, salt, and pepper.\n3. Toss greens with dressing right before serving.",
                2, 10, 0,
                listOf(
                    IngredientData("mixed greens", "4", "cups", "Produce"),
                    IngredientData("lemon", "1", "", "Produce"),
                    IngredientData("olive oil", "3", "tbsp", "Pantry")
                ),
                listOf("Side Dish", "Vegetarian", "Vegan", "Quick"),
                rating = 3.5f
            ),
            Recipe(
                "Baked Salmon",
                "1. Place salmon fillets on a baking sheet.\n2. Drizzle with olive oil and top with lemon slices.\n3. Bake at 400°F for 12-15 minutes.",
                2, 5, 15,
                listOf(
                    IngredientData("salmon fillets", "2", "", "Fish and Seafood"),
                    IngredientData("lemon", "1", "", "Produce")
                ),
                listOf("Main Dish", "Fish and Seafood", "Quick"),
                rating = 4.6f
            ),
            Recipe(
                "Chicken Stir-Fry",
                "1. Sauté chicken strips until cooked.\n2. Add sliced veggies and stir-fry for 5 minutes.\n3. Pour in stir-fry sauce and cook until thickened.",
                3, 15, 10,
                listOf(
                    IngredientData("chicken breast", "1", "lb", "Protein"),
                    IngredientData("broccoli", "1", "head", "Produce"),
                    IngredientData("soy sauce", "3", "tbsp", "Pantry"),
                    IngredientData("ginger", "1", "tsp", "Produce")
                ),
                listOf("Main Dish", "Meat", "Quick"),
                rating = 4.0f
            ),
            Recipe(
                "Berry Smoothie",
                "1. Combine berries, banana, and yogurt in a blender.\n2. Blend until smooth.",
                1, 5, 0,
                listOf(
                    IngredientData("frozen berries", "1", "cup", "Frozen"),
                    IngredientData("banana", "1", "", "Produce"),
                    IngredientData("greek yogurt", "1/2", "cup", "Dairy")
                ),
                listOf("Breakfast", "Snack", "Beverage", "Quick"),
                rating = 4.3f
            ),
            Recipe(
                "Beef Tacos",
                "1. Brown ground beef.\n2. Stir in taco seasoning and water.\n3. Serve in tortillas with toppings.",
                4, 10, 15,
                listOf(
                    IngredientData("ground beef", "1", "lb", "Protein"),
                    IngredientData("taco shells", "8", "", "Pantry"),
                    IngredientData("shredded lettuce", "1", "cup", "Produce")
                ),
                listOf("Main Dish", "Meat", "Family Staple"),
                rating = 4.5f
            ),
            Recipe(
                "Roasted Root Vegetables",
                "1. Chop carrots, potatoes, and parsnips.\n2. Toss with olive oil, salt, and rosemary.\n3. Roast at 400°F for 40 minutes.",
                4, 15, 40,
                listOf(
                    IngredientData("carrots", "3", "", "Produce"),
                    IngredientData("potatoes", "3", "", "Produce"),
                    IngredientData("rosemary", "1", "sprig", "Produce")
                ),
                listOf("Side Dish", "Vegetarian", "Vegan"),
                rating = 4.1f
            ),
            Recipe(
                "Guacamole",
                "1. Mash avocados.\n2. Mix in lime juice, salt, and diced onion.\n3. Serve with chips.",
                4, 10, 0,
                listOf(
                    IngredientData("avocados", "3", "", "Produce"),
                    IngredientData("lime", "1", "", "Produce"),
                    IngredientData("red onion", "1/4", "cup", "Produce")
                ),
                listOf("Snack", "Vegetarian", "Vegan", "Quick"),
                rating = 4.9f
            ),
            Recipe(
                "Pancakes",
                "1. Whisk flour, sugar, baking powder, egg, and milk.\n2. Cook on a hot griddle until golden on both sides.",
                3, 10, 15,
                listOf(
                    IngredientData("flour", "1.5", "cups", "Pantry"),
                    IngredientData("milk", "1.25", "cups", "Dairy"),
                    IngredientData("egg", "1", "", "Dairy")
                ),
                listOf("Breakfast", "Family Staple", "Sweet"),
                rating = 4.4f
            ),
            Recipe(
                "Mushroom Risotto",
                "1. Sauté mushrooms.\n2. Gradually add warm broth to arborio rice, stirring until absorbed.\n3. Mix in sautéed mushrooms, butter, and parmesan.",
                3, 10, 35,
                listOf(
                    IngredientData("arborio rice", "1", "cup", "Pantry"),
                    IngredientData("mushrooms", "8", "oz", "Produce"),
                    IngredientData("parmesan cheese", "1/2", "cup", "Dairy")
                ),
                listOf("Main Dish", "Vegetarian", "Spoon Dish"),
                rating = 4.7f
            ),
            Recipe(
                "Caprese Skewers",
                "1. Thread cherry tomato, basil leaf, and mozzarella ball onto skewers.\n2. Drizzle with balsamic glaze.",
                4, 15, 0,
                listOf(
                    IngredientData("cherry tomatoes", "1", "pint", "Produce"),
                    IngredientData("fresh basil", "1", "bunch", "Produce"),
                    IngredientData("mozzarella pearls", "8", "oz", "Dairy")
                ),
                listOf("Snack", "Vegetarian", "Quick"),
                rating = 4.5f
            ),
            Recipe(
                "Beef Stew",
                "1. Brown beef chunks.\n2. Add veggies, broth, and herbs.\n3. Simmer for 2 hours until beef is tender.",
                6, 20, 120,
                listOf(
                    IngredientData("stew beef", "2", "lbs", "Protein"),
                    IngredientData("potatoes", "4", "", "Produce"),
                    IngredientData("beef broth", "4", "cups", "Pantry")
                ),
                listOf("Main Dish", "Meat", "Batch Cooking", "Spoon Dish"),
                rating = 4.6f
            ),
            Recipe(
                "Quinoa Bowl",
                "1. Cook quinoa.\n2. Top with black beans, avocado, and corn.\n3. Drizzle with lime juice.",
                2, 10, 15,
                listOf(
                    IngredientData("quinoa", "1", "cup", "Pantry"),
                    IngredientData("black beans", "1", "can", "Canned"),
                    IngredientData("corn", "1", "cup", "Produce")
                ),
                listOf("Main Dish", "Vegetarian", "Vegan", "Quick"),
                rating = 4.2f
            ),
            Recipe(
                "Chocolate Chip Cookies",
                "1. Cream butter and sugar.\n2. Add eggs, flour, and chocolate chips.\n3. Bake at 350°F for 10-12 minutes.",
                24, 15, 12,
                listOf(
                    IngredientData("flour", "2.25", "cups", "Pantry"),
                    IngredientData("chocolate chips", "2", "cups", "Pantry"),
                    IngredientData("butter", "1", "cup", "Dairy")
                ),
                listOf("Dessert", "Sweet", "Family Staple"),
                rating = 4.9f
            ),
            Recipe(
                "Hummus",
                "1. Blend chickpeas, tahini, lemon juice, and garlic.\n2. Stream in olive oil until smooth.",
                6, 10, 0,
                listOf(
                    IngredientData("chickpeas", "1", "can", "Canned"),
                    IngredientData("tahini", "1/4", "cup", "Pantry"),
                    IngredientData("lemon", "1", "", "Produce")
                ),
                listOf("Snack", "Vegetarian", "Vegan", "Quick"),
                rating = 4.4f
            ),
            Recipe(
                "Eggplant Parmesan",
                "1. Slice and bread eggplant.\n2. Fry until golden.\n3. Layer with marinara and mozzarella, then bake.",
                4, 30, 30,
                listOf(
                    IngredientData("eggplant", "2", "large", "Produce"),
                    IngredientData("marinara sauce", "2", "cups", "Pantry"),
                    IngredientData("mozzarella cheese", "2", "cups", "Dairy")
                ),
                listOf("Main Dish", "Vegetarian"),
                rating = 4.3f
            ),
            Recipe(
                "Fruit Salad",
                "1. Chop various fruits.\n2. Toss together in a large bowl.",
                6, 15, 0,
                listOf(
                    IngredientData("strawberries", "1", "lb", "Produce"),
                    IngredientData("blueberries", "1", "pint", "Produce"),
                    IngredientData("grapes", "1", "lb", "Produce")
                ),
                listOf("Snack", "Dessert", "Vegetarian", "Vegan", "Quick"),
                rating = 4.0f
            )
        )

        val now = System.currentTimeMillis()
        for (recipe in recipes) {
            val recipeId = UUID.nameUUIDFromBytes(recipe.name.lowercase().toByteArray()).toString()
            db.execSQL(
                """INSERT OR REPLACE INTO recipes (id, name, instructions, servings, prepTimeMinutes, cookTimeMinutes, imageUri, sourceUrl, rating, isKidApproved, notes, isArchived, createdAt, updatedAt)
                   VALUES ('$recipeId', '${recipe.name.esc()}', '${recipe.instructions.esc()}', ${recipe.servings}, ${recipe.prepMin}, ${recipe.cookMin}, NULL, '${recipe.sourceUrl.esc()}', ${recipe.rating}, 0, '${recipe.notes.esc()}', 0, $now, $now)"""
            )
            db.execSQL("DELETE FROM ingredients WHERE recipeId = '$recipeId'")
            for (ing in recipe.ingredients) {
                val ingId = UUID.nameUUIDFromBytes("${recipeId}_${ing.name}".lowercase().toByteArray()).toString()
                db.execSQL(
                    """INSERT INTO ingredients (id, recipeId, name, quantity, unit, category)
                       VALUES ('$ingId', '$recipeId', '${ing.name.esc()}', '${ing.qty.esc()}', '${ing.unit.esc()}', '${ing.category.esc()}')"""
                )
            }
            for (tagName in recipe.tagNames) {
                val existingTagId = UUID.nameUUIDFromBytes(tagName.lowercase().toByteArray()).toString()
                db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('$existingTagId', '$tagName', ${0xFF6750A4})")
                db.execSQL("INSERT OR IGNORE INTO recipe_tag_cross_ref (recipeId, tagId) VALUES ('$recipeId', '$existingTagId')")
            }
        }

        linkRecipesToCategoriesViaTags(db)
        db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }

    private fun linkTagsToCategoriesByMatch(db: SupportSQLiteDatabase) {
        val standardTags = listOf(
            "Main Dish", "Side Dish", "Snack", "Sweet", "Savory",
            "Vegetarian", "Vegan", "Meat", "Fish and Seafood", "Dessert",
            "Family Staple", "Quick", "Prep Ahead", "Batch Cooking",
            "Breakfast", "Beverage", "Pasta", "Spoon Dish", "Favorites"
        )
        
        // Special manual mappings for names that don't match exactly
        val specialMappings = mapOf(
            "Quick" to "Fast",
            "Batch Cooking" to "Batch"
        )

        standardTags.forEach { tagName ->
            val tagId = UUID.nameUUIDFromBytes(tagName.lowercase().toByteArray()).toString()
            val targetCategoryName = specialMappings[tagName] ?: tagName
            
            db.execSQL("""
                UPDATE tags SET categoryId = (SELECT id FROM recipe_categories WHERE name = '$targetCategoryName' LIMIT 1)
                WHERE id = '$tagId'
            """)
        }
    }

    private fun linkRecipesToCategoriesViaTags(db: SupportSQLiteDatabase) {
        // Clear existing automatic links to avoid stale data if re-seeding
        db.execSQL("DELETE FROM recipe_category_cross_ref")
        
        db.execSQL("""
            INSERT OR IGNORE INTO recipe_category_cross_ref (recipeId, categoryId, addedAt)
            SELECT rtr.recipeId, t.categoryId, ${System.currentTimeMillis()} FROM recipe_tag_cross_ref rtr
            INNER JOIN tags t ON rtr.tagId = t.id
            WHERE t.categoryId IS NOT NULL
        """)
    }

    private fun seedStandardTags(db: SupportSQLiteDatabase) {
        val standardTags = listOf(
            "Main Dish", "Side Dish", "Snack", "Sweet", "Savory",
            "Vegetarian", "Vegan", "Meat", "Fish and Seafood", "Dessert",
            "Family Staple", "Quick", "Prep Ahead", "Batch Cooking",
            "Breakfast", "Beverage", "Pasta", "Spoon Dish", "Favorites"
        )
        standardTags.forEach { name ->
            val id = UUID.nameUUIDFromBytes(name.lowercase().toByteArray()).toString()
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('$id', '$name', ${0xFF6750A4})")
        }
    }

    fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF;")
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM recipe_categories")
            
            fun insertCat(name: String, parentId: String? = null, order: Int = 0): String {
            val id = UUID.nameUUIDFromBytes("${parentId ?: "root"}_$name".lowercase().toByteArray()).toString()
            val parentSql = if (parentId == null) "NULL" else "'$parentId'"
            db.execSQL("INSERT INTO recipe_categories (id, name, parentId, sortOrder) VALUES ('$id', '$name', $parentSql, $order)")
            return id
        }

        // 1. By Speed
        val speedId = insertCat("By Speed", null, 0)
        val fastId = insertCat("Fast", speedId, 0)
        insertCat("Prep Ahead", speedId, 1)
        insertCat("Batch", speedId, 2)
        insertCat("Complex", speedId, 3)
        insertCat("Multi Day", speedId, 4)
        val longCookingId = insertCat("Long Cooking", speedId, 5)

        // Seed Default Smart Rules
        // Fast: Total <= 30 OR Prep <= 20
        db.execSQL("""
            INSERT OR REPLACE INTO category_smart_rules (id, categoryId, minStars, kidApprovedOnly, maxTotalTime, minTotalTime, maxPrepTime, includeTagIds, includeCategoryIds, createdAt)
            VALUES ('${UUID.randomUUID()}', '$fastId', 0, 0, 30, NULL, 20, '', '', ${System.currentTimeMillis()})
        """)
        // Long Cooking: Total >= 60
        db.execSQL("""
            INSERT OR REPLACE INTO category_smart_rules (id, categoryId, minStars, kidApprovedOnly, maxTotalTime, minTotalTime, maxPrepTime, includeTagIds, includeCategoryIds, createdAt)
            VALUES ('${UUID.randomUUID()}', '$longCookingId', 0, 0, NULL, 60, NULL, '', '', ${System.currentTimeMillis()})
        """)

        // Match categories to tags by name and link them
        val standardTags = listOf(
            "Main Dish", "Side Dish", "Snack", "Sweet", "Savory",
            "Vegetarian", "Vegan", "Meat", "Fish and Seafood", "Dessert",
            "Family Staple", "Quick", "Prep Ahead", "Batch Cooking",
            "Breakfast", "Beverage", "Pasta", "Spoon Dish", "Favorites"
        )
        standardTags.forEach { tagName ->
            val tagId = UUID.nameUUIDFromBytes(tagName.lowercase().toByteArray()).toString()
            // Direct update using subquery for existing category ID
            db.execSQL("""
                UPDATE tags SET categoryId = (SELECT id FROM recipe_categories WHERE name = '$tagName' LIMIT 1)
                WHERE id = '$tagId'
            """)
        }

        // 2. By Source
        val sourceId = insertCat("By Source", null, 1)
        listOf("Family Recipe", "Book", "Blog", "Website").forEachIndexed { i, n -> insertCat(n, sourceId, i) }

        // 3. By Meal
        val mealId = insertCat("By Meal", null, 2)
        insertCat("Breakfast", mealId, 0)
        val lunchId = insertCat("Lunch", mealId, 1)
        listOf("Main Dish", "Sandwich", "Side Dish", "Salad", "Soup", "Bento").forEachIndexed { i, n -> insertCat(n, lunchId, i) }
        insertCat("Snack", mealId, 2)
        val dinnerId = insertCat("Dinner", mealId, 3)
        listOf("Appetizer", "Spoon Dish", "Main Dish", "Side Dish").forEachIndexed { i, n -> insertCat(n, dinnerId, i) }
        insertCat("Dessert", mealId, 4)
        insertCat("Drinks", mealId, 5)

        // 4. By Food Type
        val typeId = insertCat("By Food Type", null, 3)
        val bakingId = insertCat("Baking", typeId, 0)
        listOf("Bread and Doughs", "Cookies", "Cakes", "Breakfast Sweets", "Crackers and Savory").forEachIndexed { i, n -> insertCat(n, bakingId, i) }
        listOf("Soups and Spoon Dishes", "Vegetarian", "Vegan", "Fish and Seafood", "Meat", "Sweets and Desserts", "Drinks").forEachIndexed { i, n -> insertCat(n, typeId, i + 1) }

        // 5. By Cuisine
        val cuisineId = insertCat("By Cuisine", null, 4)
        insertCat("Family Favorites", cuisineId, 0)
        insertCat("Seasonal", cuisineId, 1)
        val regionalId = insertCat("Regional", cuisineId, 2)
        val americaId = insertCat("America", regionalId, 0)
        listOf("North American", "Argentinian", "Mexican").forEachIndexed { i, n -> insertCat(n, americaId, i) }
        val europeId = insertCat("Europe", regionalId, 1)
        listOf("Spanish", "Italian", "French").forEachIndexed { i, n -> insertCat(n, europeId, i) }
        insertCat("Africa", regionalId, 2)
        val asiaId = insertCat("Asia", regionalId, 3)
        listOf("Japanese", "Thai", "Chinese", "Korean", "Iranian").forEachIndexed { i, n -> insertCat(n, asiaId, i) }
        
        // Match existing tags to these new categories (important for fresh install auto-seed)
        linkTagsToCategoriesByMatch(db)
        linkRecipesToCategoriesViaTags(db)

        db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }

    private fun String.esc() = this.replace("'", "''")
}
