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
        // Create standard tags
        val standardTags = listOf(
            "Main Dish", "Side Dish", "Snack", "Sweet", "Savory",
            "Vegetarian", "Vegan", "Meat", "Fish and Seafood", "Dessert",
            "Family Staple", "Quick", "Prep Ahead", "Batch Cooking",
            "Breakfast", "Beverage", "Pasta", "Spoon Dish", "Favorites"
        )
        
        val tagIds = mutableMapOf<String, String>()
        
        standardTags.forEach { name ->
            val id = UUID.randomUUID().toString()
            tagIds[name] = id
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('$id', '$name', ${0xFF6750A4})")
        }

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
            // === MAIN DISHES ===
            Recipe(
                "Honey Garlic Chicken Thighs",
                "1. Season chicken thighs with salt and pepper.\n2. Sear in a hot skillet until golden, about 4 min per side.\n3. Mix honey, soy sauce, garlic, and ginger. Pour over chicken.\n4. Bake at 400°F for 25 minutes until internal temp reaches 165°F.\n5. Garnish with sesame seeds and green onions.",
                5, 10, 25,
                listOf(
                    IngredientData("chicken thighs", "10", "pieces", "Protein"),
                    IngredientData("honey", "1/3", "cup", "Pantry"),
                    IngredientData("soy sauce", "1/4", "cup", "Pantry"),
                    IngredientData("garlic cloves, minced", "4", "", "Produce"),
                    IngredientData("fresh ginger, grated", "1", "tbsp", "Produce"),
                    IngredientData("sesame seeds", "1", "tbsp", "Pantry"),
                    IngredientData("green onions, sliced", "3", "", "Produce"),
                    IngredientData("olive oil", "2", "tbsp", "Pantry")
                ),
                listOf("Chicken", "Main Dish", "Asian"),
                rating = 4.5f,
                notes = "Family favorite. Can substitute drumsticks."
            ),
            Recipe(
                "One-Pot Beef Chili",
                "1. Brown ground beef in a large pot over medium-high heat.\n2. Add diced onion, bell pepper, and garlic. Cook 5 minutes.\n3. Stir in crushed tomatoes, beans, chili powder, cumin, and paprika.\n4. Simmer for 30 minutes, stirring occasionally.\n5. Serve with shredded cheese and sour cream.",
                5, 15, 35,
                listOf(
                    IngredientData("ground beef", "2", "lbs", "Protein"),
                    IngredientData("kidney beans, drained", "2", "cans", "Canned"),
                    IngredientData("crushed tomatoes", "28", "oz", "Canned"),
                    IngredientData("yellow onion, diced", "1", "", "Produce"),
                    IngredientData("bell pepper, diced", "1", "", "Produce"),
                    IngredientData("garlic cloves, minced", "3", "", "Produce"),
                    IngredientData("chili powder", "2", "tbsp", "Spices"),
                    IngredientData("cumin", "1", "tbsp", "Spices"),
                    IngredientData("paprika", "1", "tsp", "Spices"),
                    IngredientData("shredded cheddar", "1", "cup", "Dairy"),
                    IngredientData("sour cream", "1/2", "cup", "Dairy")
                ),
                listOf("Meat", "Main Dish", "Comfort Food", "Mexican"),
                rating = 4f,
                notes = "Freezes well. Great for batch cooking."
            ),
            Recipe(
                "Lemon Herb Baked Salmon",
                "1. Preheat oven to 375°F.\n2. Place salmon fillets on a lined baking sheet.\n3. Drizzle with olive oil, lemon juice, and season with dill, salt, and pepper.\n4. Bake for 15-18 minutes until fish flakes easily.\n5. Serve with lemon wedges.",
                5, 10, 18,
                listOf(
                    IngredientData("salmon fillets", "5", "pieces", "Protein"),
                    IngredientData("lemon, juiced", "2", "", "Produce"),
                    IngredientData("fresh dill, chopped", "2", "tbsp", "Produce"),
                    IngredientData("olive oil", "3", "tbsp", "Pantry"),
                    IngredientData("garlic powder", "1", "tsp", "Spices"),
                    IngredientData("salt", "1", "tsp", "Spices"),
                    IngredientData("black pepper", "1/2", "tsp", "Spices")
                ),
                listOf("Fish and Seafood", "Main Dish", "Quick"),
                rating = 4.5f,
                notes = "Quick weeknight dinner. Kids prefer it with teriyaki glaze."
            ),
            Recipe(
                "Black Bean & Sweet Potato Tacos",
                "1. Dice sweet potatoes and roast at 400°F for 20 minutes.\n2. Heat black beans with cumin, chili powder, and lime juice.\n3. Warm tortillas.\n4. Assemble tacos with sweet potato, beans, avocado, and cilantro.\n5. Top with pickled onion and crumbled queso fresco.",
                5, 15, 20,
                listOf(
                    IngredientData("sweet potatoes, diced", "3", "", "Produce"),
                    IngredientData("black beans, drained", "2", "cans", "Canned"),
                    IngredientData("corn tortillas", "15", "", "Bakery"),
                    IngredientData("avocado, sliced", "2", "", "Produce"),
                    IngredientData("cilantro, chopped", "1/2", "cup", "Produce"),
                    IngredientData("lime", "2", "", "Produce"),
                    IngredientData("cumin", "1", "tsp", "Spices"),
                    IngredientData("chili powder", "1", "tsp", "Spices"),
                    IngredientData("queso fresco", "1/2", "cup", "Dairy")
                ),
                listOf("Vegetarian", "Main Dish", "Mexican"),
                rating = 4f,
                notes = "Meatless Monday staple."
            ),
            Recipe(
                "Sheet Pan Italian Sausage & Veggies",
                "1. Preheat oven to 425°F.\n2. Slice sausages, bell peppers, zucchini, and red onion.\n3. Toss everything with olive oil, Italian seasoning, salt, and pepper.\n4. Spread on a sheet pan in a single layer.\n5. Roast for 25-30 minutes, tossing halfway through.",
                5, 10, 30,
                listOf(
                    IngredientData("Italian sausage links", "5", "", "Protein"),
                    IngredientData("bell peppers, sliced", "3", "", "Produce"),
                    IngredientData("zucchini, sliced", "2", "", "Produce"),
                    IngredientData("red onion, wedged", "1", "", "Produce"),
                    IngredientData("olive oil", "3", "tbsp", "Pantry"),
                    IngredientData("Italian seasoning", "1", "tbsp", "Spices"),
                    IngredientData("garlic powder", "1", "tsp", "Spices")
                ),
                listOf("Meat", "Main Dish", "Italian", "Quick"),
                rating = 3.5f,
                notes = "Easy cleanup. Use hot or mild sausage."
            ),
            Recipe(
                "Vegetable Stir-Fry with Tofu",
                "1. Press and cube tofu. Pan-fry until golden on all sides.\n2. Stir-fry broccoli, snap peas, carrots, and bell pepper in sesame oil.\n3. Mix soy sauce, rice vinegar, ginger, garlic, and cornstarch for sauce.\n4. Add sauce and tofu to vegetables. Toss until coated.\n5. Serve over steamed rice.",
                5, 15, 15,
                listOf(
                    IngredientData("firm tofu", "2", "blocks", "Protein"),
                    IngredientData("broccoli florets", "3", "cups", "Produce"),
                    IngredientData("snap peas", "2", "cups", "Produce"),
                    IngredientData("carrots, julienned", "2", "", "Produce"),
                    IngredientData("bell pepper, sliced", "1", "", "Produce"),
                    IngredientData("soy sauce", "1/4", "cup", "Pantry"),
                    IngredientData("rice vinegar", "2", "tbsp", "Pantry"),
                    IngredientData("sesame oil", "2", "tbsp", "Pantry"),
                    IngredientData("cornstarch", "1", "tbsp", "Pantry"),
                    IngredientData("fresh ginger", "1", "tbsp", "Produce"),
                    IngredientData("garlic cloves", "3", "", "Produce"),
                    IngredientData("jasmine rice", "2", "cups", "Pantry")
                ),
                listOf("Vegetarian", "Main Dish", "Asian", "Quick"),
                rating = 3.5f,
                notes = "Press tofu well for best texture."
            ),
            Recipe(
                "Baked Ziti",
                "1. Cook ziti according to package directions.\n2. Brown Italian sausage with garlic and onion.\n3. Mix cooked pasta with marinara, ricotta, and half the mozzarella.\n4. Spread into a baking dish, top with remaining mozzarella and Parmesan.\n5. Bake at 375°F for 25 minutes until bubbly and golden.",
                5, 15, 25,
                listOf(
                    IngredientData("ziti pasta", "1", "lb", "Pantry"),
                    IngredientData("Italian sausage", "1", "lb", "Protein"),
                    IngredientData("marinara sauce", "24", "oz", "Canned"),
                    IngredientData("ricotta cheese", "15", "oz", "Dairy"),
                    IngredientData("mozzarella, shredded", "2", "cups", "Dairy"),
                    IngredientData("Parmesan, grated", "1/2", "cup", "Dairy"),
                    IngredientData("garlic cloves, minced", "3", "", "Produce"),
                    IngredientData("yellow onion, diced", "1", "", "Produce")
                ),
                listOf("Meat", "Main Dish", "Pasta", "Italian", "Comfort Food"),
                rating = 4.5f,
                notes = "Can assemble ahead and refrigerate before baking."
            ),
            Recipe(
                "Teriyaki Salmon Bowls",
                "1. Marinate salmon in teriyaki sauce for 15 minutes.\n2. Bake salmon at 400°F for 12-15 minutes.\n3. Cook sushi rice according to package.\n4. Prepare toppings: edamame, cucumber, avocado, pickled ginger.\n5. Assemble bowls and drizzle with extra teriyaki and sriracha mayor.",
                5, 20, 15,
                listOf(
                    IngredientData("salmon fillets", "5", "pieces", "Protein"),
                    IngredientData("teriyaki sauce", "1/2", "cup", "Pantry"),
                    IngredientData("sushi rice", "2", "cups", "Pantry"),
                    IngredientData("edamame, shelled", "1", "cup", "Frozen"),
                    IngredientData("cucumber, sliced", "1", "", "Produce"),
                    IngredientData("avocado, sliced", "2", "", "Produce"),
                    IngredientData("pickled ginger", "1/4", "cup", "Pantry"),
                    IngredientData("sesame seeds", "2", "tbsp", "Pantry"),
                    IngredientData("sriracha mayo", "1/4", "cup", "Pantry")
                ),
                listOf("Fish and Seafood", "Main Dish", "Asian"),
                rating = 4f,
                notes = "Kids love building their own bowls."
            ),
            Recipe(
                "Creamy Mushroom & Spinach Pasta",
                "1. Cook penne according to package.\n2. Sauté sliced mushrooms and garlic in butter until golden.\n3. Add spinach and cook until wilted.\n4. Pour in heavy cream and Parmesan. Simmer until thickened.\n5. Toss with pasta, season with nutmeg, salt, and pepper.",
                5, 10, 20,
                listOf(
                    IngredientData("penne pasta", "1", "lb", "Pantry"),
                    IngredientData("cremini mushrooms, sliced", "16", "oz", "Produce"),
                    IngredientData("fresh spinach", "6", "oz", "Produce"),
                    IngredientData("heavy cream", "1", "cup", "Dairy"),
                    IngredientData("Parmesan, grated", "3/4", "cup", "Dairy"),
                    IngredientData("butter", "3", "tbsp", "Dairy"),
                    IngredientData("garlic cloves, minced", "4", "", "Produce"),
                    IngredientData("nutmeg", "1/4", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Main Dish", "Pasta", "Italian", "Comfort Food", "Quick"),
                rating = 4f,
                notes = "Add grilled chicken for a non-vegetarian version."
            ),
            Recipe(
                "BBQ Pulled Chicken Sandwiches",
                "1. Place chicken breasts in slow cooker with BBQ sauce, onion, and garlic.\n2. Cook on low for 6 hours or high for 3 hours.\n3. Shred chicken with two forks and stir back into sauce.\n4. Toast brioche buns lightly.\n5. Serve with coleslaw on top.",
                5, 10, 180,
                listOf(
                    IngredientData("chicken breasts", "3", "lbs", "Protein"),
                    IngredientData("BBQ sauce", "1.5", "cups", "Pantry"),
                    IngredientData("yellow onion, sliced", "1", "", "Produce"),
                    IngredientData("garlic cloves", "3", "", "Produce"),
                    IngredientData("brioche buns", "8", "", "Bakery"),
                    IngredientData("apple cider vinegar", "2", "tbsp", "Pantry"),
                    IngredientData("brown sugar", "1", "tbsp", "Pantry"),
                    IngredientData("smoked paprika", "1", "tsp", "Spices")
                ),
                listOf("Chicken", "Main Dish", "Comfort Food"),
                rating = 4f,
                notes = "Set it and forget it. Great for busy days."
            ),
            Recipe(
                "Chickpea Coconut Curry",
                "1. Sauté onion, garlic, and ginger in coconut oil.\n2. Add curry powder, turmeric, and cumin. Toast spices 1 minute.\n3. Add chickpeas, diced tomatoes, and coconut milk.\n4. Simmer 20 minutes until thickened.\n5. Stir in spinach, season to taste. Serve over basmati rice.",
                5, 10, 25,
                listOf(
                    IngredientData("chickpeas, drained", "2", "cans", "Canned"),
                    IngredientData("coconut milk", "14", "oz", "Canned"),
                    IngredientData("diced tomatoes", "14", "oz", "Canned"),
                    IngredientData("fresh spinach", "4", "cups", "Produce"),
                    IngredientData("yellow onion, diced", "1", "", "Produce"),
                    IngredientData("garlic cloves, minced", "3", "", "Produce"),
                    IngredientData("fresh ginger", "1", "tbsp", "Produce"),
                    IngredientData("curry powder", "2", "tbsp", "Spices"),
                    IngredientData("turmeric", "1", "tsp", "Spices"),
                    IngredientData("cumin", "1", "tsp", "Spices"),
                    IngredientData("basmati rice", "2", "cups", "Pantry"),
                    IngredientData("coconut oil", "2", "tbsp", "Pantry")
                ),
                listOf("Vegetarian", "Main Dish", "Asian", "Comfort Food"),
                rating = 4.5f,
                notes = "One of our go-to vegetarian meals. Double the curry powder for extra heat."
            ),
            Recipe(
                "Garlic Butter Shrimp Scampi",
                "1. Cook linguine according to package.\n2. Sauté shrimp in butter and olive oil until pink, about 2 min per side.\n3. Add garlic, red pepper flakes, and white wine. Simmer 3 minutes.\n4. Toss in pasta with lemon juice and parsley.\n5. Top with Parmesan.",
                5, 10, 15,
                listOf(
                    IngredientData("large shrimp, peeled", "2", "lbs", "Protein"),
                    IngredientData("linguine", "1", "lb", "Pantry"),
                    IngredientData("butter", "4", "tbsp", "Dairy"),
                    IngredientData("garlic cloves, minced", "6", "", "Produce"),
                    IngredientData("white wine", "1/2", "cup", "Pantry"),
                    IngredientData("lemon, juiced", "2", "", "Produce"),
                    IngredientData("red pepper flakes", "1/2", "tsp", "Spices"),
                    IngredientData("fresh parsley", "1/4", "cup", "Produce"),
                    IngredientData("Parmesan, grated", "1/2", "cup", "Dairy"),
                    IngredientData("olive oil", "2", "tbsp", "Pantry")
                ),
                listOf("Fish and Seafood", "Main Dish", "Pasta", "Italian", "Quick"),
                rating = 4.5f,
                notes = "Use dry white wine like Pinot Grigio."
            ),

            // === SIDE DISHES ===
            Recipe(
                "Roasted Garlic Parmesan Broccoli",
                "1. Cut broccoli into florets.\n2. Toss with olive oil, minced garlic, salt, and pepper.\n3. Spread on baking sheet in single layer.\n4. Roast at 425°F for 20 minutes until edges are crispy.\n5. Toss with grated Parmesan immediately after removing from oven.",
                5, 5, 20,
                listOf(
                    IngredientData("broccoli crowns", "3", "", "Produce"),
                    IngredientData("olive oil", "3", "tbsp", "Pantry"),
                    IngredientData("garlic cloves, minced", "4", "", "Produce"),
                    IngredientData("Parmesan, grated", "1/2", "cup", "Dairy"),
                    IngredientData("salt", "1", "tsp", "Spices"),
                    IngredientData("black pepper", "1/2", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish", "Quick"),
                rating = 4f,
                notes = "Don''t overcrowd the pan for best crispiness."
            ),
            Recipe(
                "Mexican Street Corn Salad",
                "1. Char corn kernels in a hot skillet or grill corn on the cob.\n2. Mix with mayo, sour cream, lime juice, and chili powder.\n3. Add crumbled cotija cheese and chopped cilantro.\n4. Season with salt and cayenne to taste.\n5. Serve warm or cold.",
                5, 10, 10,
                listOf(
                    IngredientData("corn kernels (fresh or frozen)", "6", "cups", "Produce"),
                    IngredientData("mayonnaise", "1/4", "cup", "Pantry"),
                    IngredientData("sour cream", "1/4", "cup", "Dairy"),
                    IngredientData("lime, juiced", "2", "", "Produce"),
                    IngredientData("chili powder", "1", "tsp", "Spices"),
                    IngredientData("cotija cheese", "1/2", "cup", "Dairy"),
                    IngredientData("cilantro, chopped", "1/4", "cup", "Produce"),
                    IngredientData("cayenne pepper", "1/4", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish", "Mexican", "Quick"),
                rating = 4.5f,
                notes = "Great summer side dish. Grilling the corn adds smokiness."
            ),
            Recipe(
                "Garlic Mashed Potatoes",
                "1. Peel and cube potatoes. Boil in salted water until fork-tender, about 15 min.\n2. Roast garlic cloves in olive oil at 400°F for 20 min (or sauté in butter).\n3. Drain potatoes. Add butter, warm cream, and roasted garlic.\n4. Mash to desired consistency.\n5. Season with salt, pepper, and chives.",
                5, 10, 20,
                listOf(
                    IngredientData("russet potatoes", "5", "lbs", "Produce"),
                    IngredientData("butter", "1/2", "cup", "Dairy"),
                    IngredientData("heavy cream", "1/2", "cup", "Dairy"),
                    IngredientData("garlic cloves", "8", "", "Produce"),
                    IngredientData("chives, chopped", "2", "tbsp", "Produce"),
                    IngredientData("salt", "2", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish", "Comfort Food"),
                rating = 4f,
                notes = "Yukon Gold also works well. Keep cream warm to avoid lumps."
            ),
            Recipe(
                "Asian Cucumber Salad",
                "1. Slice cucumbers thinly (use a mandoline if available).\n2. Whisk together rice vinegar, sesame oil, soy sauce, sugar, and chili flakes.\n3. Toss cucumbers in dressing.\n4. Garnish with sesame seeds and sliced green onion.\n5. Let marinate 10 minutes before serving.",
                5, 10, 0,
                listOf(
                    IngredientData("English cucumbers", "3", "", "Produce"),
                    IngredientData("rice vinegar", "3", "tbsp", "Pantry"),
                    IngredientData("sesame oil", "2", "tbsp", "Pantry"),
                    IngredientData("soy sauce", "1", "tbsp", "Pantry"),
                    IngredientData("sugar", "1", "tbsp", "Pantry"),
                    IngredientData("red pepper flakes", "1/2", "tsp", "Spices"),
                    IngredientData("sesame seeds", "1", "tbsp", "Pantry"),
                    IngredientData("green onions, sliced", "2", "", "Produce")
                ),
                listOf("Vegetarian", "Side Dish", "Asian", "Quick"),
                rating = 3.5f,
                notes = "Refreshing side. Best served within an hour."
            ),
            Recipe(
                "Cilantro Lime Rice",
                "1. Rinse rice until water runs clear.\n2. Cook rice in water with a bay leaf and a drizzle of oil.\n3. Fluff with a fork when done.\n4. Stir in lime juice, lime zest, and chopped cilantro.\n5. Season with salt. Serve immediately.",
                5, 5, 20,
                listOf(
                    IngredientData("long grain white rice", "2", "cups", "Pantry"),
                    IngredientData("water", "3.5", "cups", ""),
                    IngredientData("lime, juiced and zested", "2", "", "Produce"),
                    IngredientData("cilantro, chopped", "3/4", "cup", "Produce"),
                    IngredientData("bay leaf", "1", "", "Spices"),
                    IngredientData("olive oil", "1", "tbsp", "Pantry"),
                    IngredientData("salt", "1", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish", "Mexican", "Quick"),
                rating = 4f,
                notes = "Chipotle copycat. Goes with any Mexican main."
            ),
            Recipe(
                "Caprese Salad",
                "1. Slice tomatoes and fresh mozzarella into 1/4-inch rounds.\n2. Arrange alternating slices on a platter.\n3. Tuck fresh basil leaves between slices.\n4. Drizzle generously with extra virgin olive oil and balsamic glaze.\n5. Season with flaky sea salt and cracked pepper.",
                5, 10, 0,
                listOf(
                    IngredientData("large tomatoes", "4", "", "Produce"),
                    IngredientData("fresh mozzarella", "16", "oz", "Dairy"),
                    IngredientData("fresh basil leaves", "1", "bunch", "Produce"),
                    IngredientData("extra virgin olive oil", "3", "tbsp", "Pantry"),
                    IngredientData("balsamic glaze", "2", "tbsp", "Pantry"),
                    IngredientData("flaky sea salt", "1", "tsp", "Spices"),
                    IngredientData("cracked black pepper", "1/2", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish", "Italian", "Quick"),
                rating = 4f,
                notes = "Use the ripest tomatoes you can find. Burrata is a great substitute."
            ),
            Recipe(
                "Honey Roasted Carrots",
                "1. Peel and cut carrots into sticks or halve lengthwise.\n2. Toss with olive oil, honey, thyme, salt, and pepper.\n3. Spread on a baking sheet in a single layer.\n4. Roast at 400°F for 25-30 minutes, flipping halfway.\n5. Finish with a squeeze of lemon and fresh thyme.",
                5, 10, 30,
                listOf(
                    IngredientData("large carrots", "2", "lbs", "Produce"),
                    IngredientData("honey", "2", "tbsp", "Pantry"),
                    IngredientData("olive oil", "2", "tbsp", "Pantry"),
                    IngredientData("fresh thyme", "4", "sprigs", "Produce"),
                    IngredientData("lemon", "1", "", "Produce"),
                    IngredientData("salt", "1", "tsp", "Spices"),
                    IngredientData("black pepper", "1/2", "tsp", "Spices")
                ),
                listOf("Vegetarian", "Side Dish"),
                rating = 3.5f,
                notes = "Rainbow carrots make this dish extra colorful."
            )
        )

        val now = System.currentTimeMillis()

        for (recipe in recipes) {
            val recipeId = UUID.randomUUID().toString()

            db.execSQL(
                """INSERT INTO recipes (id, name, instructions, servings, prepTimeMinutes, cookTimeMinutes, imageUri, sourceUrl, rating, isKidApproved, notes, isArchived, createdAt, updatedAt)
                   VALUES ('$recipeId', '${recipe.name.esc()}', '${recipe.instructions.esc()}', ${recipe.servings}, ${recipe.prepMin}, ${recipe.cookMin}, NULL, '${recipe.sourceUrl.esc()}', ${recipe.rating}, 0, '${recipe.notes.esc()}', 0, $now, $now)"""
            )

            for (ing in recipe.ingredients) {
                val ingId = UUID.randomUUID().toString()
                db.execSQL(
                    """INSERT INTO ingredients (id, recipeId, name, quantity, unit, category)
                       VALUES ('$ingId', '$recipeId', '${ing.name.esc()}', '${ing.qty.esc()}', '${ing.unit.esc()}', '${ing.category.esc()}')"""
                )
            }

            for (tagName in recipe.tagNames) {
                // Find or create the tag ID
                val existingTagId = tagIds[tagName] ?: UUID.randomUUID().toString().also { tagIds[tagName] = it }
                db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('$existingTagId', '$tagName', ${0xFF6750A4})")
                
                db.execSQL(
                    """INSERT OR IGNORE INTO recipe_tag_cross_ref (recipeId, tagId)
                       VALUES ('$recipeId', '$existingTagId')"""
                )
            }
        }
    }

    private fun String.esc() = this.replace("'", "''")
}
