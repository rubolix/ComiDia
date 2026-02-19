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
            seedDatabase(db)
        }
    }

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        // Create tags
        val tagMeat = uuid()
        val tagVegetarian = uuid()
        val tagFish = uuid()
        val tagChicken = uuid()
        val tagPasta = uuid()
        val tagQuick = uuid()
        val tagSide = uuid()
        val tagMain = uuid()
        val tagMexican = uuid()
        val tagAsian = uuid()
        val tagItalian = uuid()
        val tagComfort = uuid()

        val tags = listOf(
            tagMeat to "Meat", tagVegetarian to "Vegetarian", tagFish to "Fish",
            tagChicken to "Chicken", tagPasta to "Pasta", tagQuick to "Quick (< 30 min)",
            tagSide to "Side Dish", tagMain to "Main Dish", tagMexican to "Mexican",
            tagAsian to "Asian", tagItalian to "Italian", tagComfort to "Comfort Food"
        )
        tags.forEach { (id, name) ->
            db.execSQL("INSERT INTO tags (id, name, color) VALUES ('$id', '$name', ${0xFF6750A4})")
        }

        data class Recipe(
            val name: String,
            val instructions: String,
            val servings: Int,
            val prepMin: Int,
            val cookMin: Int,
            val ingredients: List<Triple<String, String, String>>, // name, qty, unit
            val tagIds: List<String>
        )

        val recipes = listOf(
            // === MAIN DISHES ===
            Recipe(
                "Honey Garlic Chicken Thighs",
                "1. Season chicken thighs with salt and pepper.\n2. Sear in a hot skillet until golden, about 4 min per side.\n3. Mix honey, soy sauce, garlic, and ginger. Pour over chicken.\n4. Bake at 400°F for 25 minutes until internal temp reaches 165°F.\n5. Garnish with sesame seeds and green onions.",
                5, 10, 25,
                listOf(
                    Triple("chicken thighs", "10", "pieces"), Triple("honey", "1/3", "cup"),
                    Triple("soy sauce", "1/4", "cup"), Triple("garlic cloves, minced", "4", ""),
                    Triple("fresh ginger, grated", "1", "tbsp"), Triple("sesame seeds", "1", "tbsp"),
                    Triple("green onions, sliced", "3", ""), Triple("olive oil", "2", "tbsp")
                ),
                listOf(tagChicken, tagMain, tagAsian)
            ),
            Recipe(
                "One-Pot Beef Chili",
                "1. Brown ground beef in a large pot over medium-high heat.\n2. Add diced onion, bell pepper, and garlic. Cook 5 minutes.\n3. Stir in crushed tomatoes, beans, chili powder, cumin, and paprika.\n4. Simmer for 30 minutes, stirring occasionally.\n5. Serve with shredded cheese and sour cream.",
                5, 15, 35,
                listOf(
                    Triple("ground beef", "2", "lbs"), Triple("kidney beans, drained", "2", "cans"),
                    Triple("crushed tomatoes", "28", "oz"), Triple("yellow onion, diced", "1", ""),
                    Triple("bell pepper, diced", "1", ""), Triple("garlic cloves, minced", "3", ""),
                    Triple("chili powder", "2", "tbsp"), Triple("cumin", "1", "tbsp"),
                    Triple("paprika", "1", "tsp"), Triple("shredded cheddar", "1", "cup"),
                    Triple("sour cream", "1/2", "cup")
                ),
                listOf(tagMeat, tagMain, tagComfort, tagMexican)
            ),
            Recipe(
                "Lemon Herb Baked Salmon",
                "1. Preheat oven to 375°F.\n2. Place salmon fillets on a lined baking sheet.\n3. Drizzle with olive oil, lemon juice, and season with dill, salt, and pepper.\n4. Bake for 15-18 minutes until fish flakes easily.\n5. Serve with lemon wedges.",
                5, 10, 18,
                listOf(
                    Triple("salmon fillets", "5", "pieces"), Triple("lemon, juiced", "2", ""),
                    Triple("fresh dill, chopped", "2", "tbsp"), Triple("olive oil", "3", "tbsp"),
                    Triple("garlic powder", "1", "tsp"), Triple("salt", "1", "tsp"),
                    Triple("black pepper", "1/2", "tsp")
                ),
                listOf(tagFish, tagMain, tagQuick)
            ),
            Recipe(
                "Black Bean & Sweet Potato Tacos",
                "1. Dice sweet potatoes and roast at 400°F for 20 minutes.\n2. Heat black beans with cumin, chili powder, and lime juice.\n3. Warm tortillas.\n4. Assemble tacos with sweet potato, beans, avocado, and cilantro.\n5. Top with pickled onion and crumbled queso fresco.",
                5, 15, 20,
                listOf(
                    Triple("sweet potatoes, diced", "3", ""), Triple("black beans, drained", "2", "cans"),
                    Triple("corn tortillas", "15", ""), Triple("avocado, sliced", "2", ""),
                    Triple("cilantro, chopped", "1/2", "cup"), Triple("lime", "2", ""),
                    Triple("cumin", "1", "tsp"), Triple("chili powder", "1", "tsp"),
                    Triple("queso fresco", "1/2", "cup")
                ),
                listOf(tagVegetarian, tagMain, tagMexican)
            ),
            Recipe(
                "Sheet Pan Italian Sausage & Veggies",
                "1. Preheat oven to 425°F.\n2. Slice sausages, bell peppers, zucchini, and red onion.\n3. Toss everything with olive oil, Italian seasoning, salt, and pepper.\n4. Spread on a sheet pan in a single layer.\n5. Roast for 25-30 minutes, tossing halfway through.",
                5, 10, 30,
                listOf(
                    Triple("Italian sausage links", "5", ""), Triple("bell peppers, sliced", "3", ""),
                    Triple("zucchini, sliced", "2", ""), Triple("red onion, wedged", "1", ""),
                    Triple("olive oil", "3", "tbsp"), Triple("Italian seasoning", "1", "tbsp"),
                    Triple("garlic powder", "1", "tsp")
                ),
                listOf(tagMeat, tagMain, tagItalian, tagQuick)
            ),
            Recipe(
                "Vegetable Stir-Fry with Tofu",
                "1. Press and cube tofu. Pan-fry until golden on all sides.\n2. Stir-fry broccoli, snap peas, carrots, and bell pepper in sesame oil.\n3. Mix soy sauce, rice vinegar, ginger, garlic, and cornstarch for sauce.\n4. Add sauce and tofu to vegetables. Toss until coated.\n5. Serve over steamed rice.",
                5, 15, 15,
                listOf(
                    Triple("firm tofu", "2", "blocks"), Triple("broccoli florets", "3", "cups"),
                    Triple("snap peas", "2", "cups"), Triple("carrots, julienned", "2", ""),
                    Triple("bell pepper, sliced", "1", ""), Triple("soy sauce", "1/4", "cup"),
                    Triple("rice vinegar", "2", "tbsp"), Triple("sesame oil", "2", "tbsp"),
                    Triple("cornstarch", "1", "tbsp"), Triple("fresh ginger", "1", "tbsp"),
                    Triple("garlic cloves", "3", ""), Triple("jasmine rice", "2", "cups")
                ),
                listOf(tagVegetarian, tagMain, tagAsian, tagQuick)
            ),
            Recipe(
                "Baked Ziti",
                "1. Cook ziti according to package directions.\n2. Brown Italian sausage with garlic and onion.\n3. Mix cooked pasta with marinara, ricotta, and half the mozzarella.\n4. Spread into a baking dish, top with remaining mozzarella and Parmesan.\n5. Bake at 375°F for 25 minutes until bubbly and golden.",
                5, 15, 25,
                listOf(
                    Triple("ziti pasta", "1", "lb"), Triple("Italian sausage", "1", "lb"),
                    Triple("marinara sauce", "24", "oz"), Triple("ricotta cheese", "15", "oz"),
                    Triple("mozzarella, shredded", "2", "cups"), Triple("Parmesan, grated", "1/2", "cup"),
                    Triple("garlic cloves, minced", "3", ""), Triple("yellow onion, diced", "1", "")
                ),
                listOf(tagMeat, tagMain, tagPasta, tagItalian, tagComfort)
            ),
            Recipe(
                "Teriyaki Salmon Bowls",
                "1. Marinate salmon in teriyaki sauce for 15 minutes.\n2. Bake salmon at 400°F for 12-15 minutes.\n3. Cook sushi rice according to package.\n4. Prepare toppings: edamame, cucumber, avocado, pickled ginger.\n5. Assemble bowls and drizzle with extra teriyaki and sriracha mayo.",
                5, 20, 15,
                listOf(
                    Triple("salmon fillets", "5", "pieces"), Triple("teriyaki sauce", "1/2", "cup"),
                    Triple("sushi rice", "2", "cups"), Triple("edamame, shelled", "1", "cup"),
                    Triple("cucumber, sliced", "1", ""), Triple("avocado, sliced", "2", ""),
                    Triple("pickled ginger", "1/4", "cup"), Triple("sesame seeds", "2", "tbsp"),
                    Triple("sriracha mayo", "1/4", "cup")
                ),
                listOf(tagFish, tagMain, tagAsian)
            ),
            Recipe(
                "Creamy Mushroom & Spinach Pasta",
                "1. Cook penne according to package.\n2. Sauté sliced mushrooms and garlic in butter until golden.\n3. Add spinach and cook until wilted.\n4. Pour in heavy cream and Parmesan. Simmer until thickened.\n5. Toss with pasta, season with nutmeg, salt, and pepper.",
                5, 10, 20,
                listOf(
                    Triple("penne pasta", "1", "lb"), Triple("cremini mushrooms, sliced", "16", "oz"),
                    Triple("fresh spinach", "6", "oz"), Triple("heavy cream", "1", "cup"),
                    Triple("Parmesan, grated", "3/4", "cup"), Triple("butter", "3", "tbsp"),
                    Triple("garlic cloves, minced", "4", ""), Triple("nutmeg", "1/4", "tsp")
                ),
                listOf(tagVegetarian, tagMain, tagPasta, tagItalian, tagComfort, tagQuick)
            ),
            Recipe(
                "BBQ Pulled Chicken Sandwiches",
                "1. Place chicken breasts in slow cooker with BBQ sauce, onion, and garlic.\n2. Cook on low for 6 hours or high for 3 hours.\n3. Shred chicken with two forks and stir back into sauce.\n4. Toast brioche buns lightly.\n5. Serve with coleslaw on top.",
                5, 10, 180,
                listOf(
                    Triple("chicken breasts", "3", "lbs"), Triple("BBQ sauce", "1.5", "cups"),
                    Triple("yellow onion, sliced", "1", ""), Triple("garlic cloves", "3", ""),
                    Triple("brioche buns", "8", ""), Triple("apple cider vinegar", "2", "tbsp"),
                    Triple("brown sugar", "1", "tbsp"), Triple("smoked paprika", "1", "tsp")
                ),
                listOf(tagChicken, tagMain, tagComfort)
            ),
            Recipe(
                "Chickpea Coconut Curry",
                "1. Sauté onion, garlic, and ginger in coconut oil.\n2. Add curry powder, turmeric, and cumin. Toast spices 1 minute.\n3. Add chickpeas, diced tomatoes, and coconut milk.\n4. Simmer 20 minutes until thickened.\n5. Stir in spinach, season to taste. Serve over basmati rice.",
                5, 10, 25,
                listOf(
                    Triple("chickpeas, drained", "2", "cans"), Triple("coconut milk", "14", "oz"),
                    Triple("diced tomatoes", "14", "oz"), Triple("fresh spinach", "4", "cups"),
                    Triple("yellow onion, diced", "1", ""), Triple("garlic cloves", "3", ""),
                    Triple("fresh ginger", "1", "tbsp"), Triple("curry powder", "2", "tbsp"),
                    Triple("turmeric", "1", "tsp"), Triple("cumin", "1", "tsp"),
                    Triple("basmati rice", "2", "cups"), Triple("coconut oil", "2", "tbsp")
                ),
                listOf(tagVegetarian, tagMain, tagAsian, tagComfort)
            ),
            Recipe(
                "Garlic Butter Shrimp Scampi",
                "1. Cook linguine according to package.\n2. Sauté shrimp in butter and olive oil until pink, about 2 min per side.\n3. Add garlic, red pepper flakes, and white wine. Simmer 3 minutes.\n4. Toss in pasta with lemon juice and parsley.\n5. Top with Parmesan.",
                5, 10, 15,
                listOf(
                    Triple("large shrimp, peeled", "2", "lbs"), Triple("linguine", "1", "lb"),
                    Triple("butter", "4", "tbsp"), Triple("garlic cloves, minced", "6", ""),
                    Triple("white wine", "1/2", "cup"), Triple("lemon, juiced", "2", ""),
                    Triple("red pepper flakes", "1/2", "tsp"), Triple("fresh parsley", "1/4", "cup"),
                    Triple("Parmesan, grated", "1/2", "cup"), Triple("olive oil", "2", "tbsp")
                ),
                listOf(tagFish, tagMain, tagPasta, tagItalian, tagQuick)
            ),

            // === SIDE DISHES ===
            Recipe(
                "Roasted Garlic Parmesan Broccoli",
                "1. Cut broccoli into florets.\n2. Toss with olive oil, minced garlic, salt, and pepper.\n3. Spread on baking sheet in single layer.\n4. Roast at 425°F for 20 minutes until edges are crispy.\n5. Toss with grated Parmesan immediately after removing from oven.",
                5, 5, 20,
                listOf(
                    Triple("broccoli crowns", "3", ""), Triple("olive oil", "3", "tbsp"),
                    Triple("garlic cloves, minced", "4", ""), Triple("Parmesan, grated", "1/2", "cup"),
                    Triple("salt", "1", "tsp"), Triple("black pepper", "1/2", "tsp")
                ),
                listOf(tagVegetarian, tagSide, tagQuick)
            ),
            Recipe(
                "Mexican Street Corn Salad",
                "1. Char corn kernels in a hot skillet or grill corn on the cob.\n2. Mix with mayo, sour cream, lime juice, and chili powder.\n3. Add crumbled cotija cheese and chopped cilantro.\n4. Season with salt and cayenne to taste.\n5. Serve warm or cold.",
                5, 10, 10,
                listOf(
                    Triple("corn kernels (fresh or frozen)", "6", "cups"), Triple("mayonnaise", "1/4", "cup"),
                    Triple("sour cream", "1/4", "cup"), Triple("lime, juiced", "2", ""),
                    Triple("chili powder", "1", "tsp"), Triple("cotija cheese", "1/2", "cup"),
                    Triple("cilantro, chopped", "1/4", "cup"), Triple("cayenne pepper", "1/4", "tsp")
                ),
                listOf(tagVegetarian, tagSide, tagMexican, tagQuick)
            ),
            Recipe(
                "Garlic Mashed Potatoes",
                "1. Peel and cube potatoes. Boil in salted water until fork-tender, about 15 min.\n2. Roast garlic cloves in olive oil at 400°F for 20 min (or sauté in butter).\n3. Drain potatoes. Add butter, warm cream, and roasted garlic.\n4. Mash to desired consistency.\n5. Season with salt, pepper, and chives.",
                5, 10, 20,
                listOf(
                    Triple("russet potatoes", "5", "lbs"), Triple("butter", "1/2", "cup"),
                    Triple("heavy cream", "1/2", "cup"), Triple("garlic cloves", "8", ""),
                    Triple("chives, chopped", "2", "tbsp"), Triple("salt", "2", "tsp")
                ),
                listOf(tagVegetarian, tagSide, tagComfort)
            ),
            Recipe(
                "Asian Cucumber Salad",
                "1. Slice cucumbers thinly (use a mandoline if available).\n2. Whisk together rice vinegar, sesame oil, soy sauce, sugar, and chili flakes.\n3. Toss cucumbers in dressing.\n4. Garnish with sesame seeds and sliced green onion.\n5. Let marinate 10 minutes before serving.",
                5, 10, 0,
                listOf(
                    Triple("English cucumbers", "3", ""), Triple("rice vinegar", "3", "tbsp"),
                    Triple("sesame oil", "2", "tbsp"), Triple("soy sauce", "1", "tbsp"),
                    Triple("sugar", "1", "tbsp"), Triple("red pepper flakes", "1/2", "tsp"),
                    Triple("sesame seeds", "1", "tbsp"), Triple("green onions, sliced", "2", "")
                ),
                listOf(tagVegetarian, tagSide, tagAsian, tagQuick)
            ),
            Recipe(
                "Cilantro Lime Rice",
                "1. Rinse rice until water runs clear.\n2. Cook rice in water with a bay leaf and a drizzle of oil.\n3. Fluff with a fork when done.\n4. Stir in lime juice, lime zest, and chopped cilantro.\n5. Season with salt. Serve immediately.",
                5, 5, 20,
                listOf(
                    Triple("long grain white rice", "2", "cups"), Triple("water", "3.5", "cups"),
                    Triple("lime, juiced and zested", "2", ""), Triple("cilantro, chopped", "3/4", "cup"),
                    Triple("bay leaf", "1", ""), Triple("olive oil", "1", "tbsp"),
                    Triple("salt", "1", "tsp")
                ),
                listOf(tagVegetarian, tagSide, tagMexican, tagQuick)
            ),
            Recipe(
                "Caprese Salad",
                "1. Slice tomatoes and fresh mozzarella into 1/4-inch rounds.\n2. Arrange alternating slices on a platter.\n3. Tuck fresh basil leaves between slices.\n4. Drizzle generously with extra virgin olive oil and balsamic glaze.\n5. Season with flaky sea salt and cracked pepper.",
                5, 10, 0,
                listOf(
                    Triple("large tomatoes", "4", ""), Triple("fresh mozzarella", "16", "oz"),
                    Triple("fresh basil leaves", "1", "bunch"), Triple("extra virgin olive oil", "3", "tbsp"),
                    Triple("balsamic glaze", "2", "tbsp"), Triple("flaky sea salt", "1", "tsp"),
                    Triple("cracked black pepper", "1/2", "tsp")
                ),
                listOf(tagVegetarian, tagSide, tagItalian, tagQuick)
            ),
            Recipe(
                "Honey Roasted Carrots",
                "1. Peel and cut carrots into sticks or halve lengthwise.\n2. Toss with olive oil, honey, thyme, salt, and pepper.\n3. Spread on a baking sheet in a single layer.\n4. Roast at 400°F for 25-30 minutes, flipping halfway.\n5. Finish with a squeeze of lemon and fresh thyme.",
                5, 10, 30,
                listOf(
                    Triple("large carrots", "2", "lbs"), Triple("honey", "2", "tbsp"),
                    Triple("olive oil", "2", "tbsp"), Triple("fresh thyme", "4", "sprigs"),
                    Triple("lemon", "1", ""), Triple("salt", "1", "tsp"),
                    Triple("black pepper", "1/2", "tsp")
                ),
                listOf(tagVegetarian, tagSide)
            )
        )

        val now = System.currentTimeMillis()

        for (recipe in recipes) {
            val recipeId = uuid()

            db.execSQL(
                """INSERT INTO recipes (id, name, instructions, servings, prepTimeMinutes, cookTimeMinutes, isArchived, createdAt, updatedAt)
                   VALUES ('$recipeId', '${recipe.name.esc()}', '${recipe.instructions.esc()}', ${recipe.servings}, ${recipe.prepMin}, ${recipe.cookMin}, 0, $now, $now)"""
            )

            for ((ingName, qty, unit) in recipe.ingredients) {
                val ingId = uuid()
                db.execSQL(
                    """INSERT INTO ingredients (id, recipeId, name, quantity, unit, category)
                       VALUES ('$ingId', '$recipeId', '${ingName.esc()}', '${qty.esc()}', '${unit.esc()}', '')"""
                )
            }

            for (tagId in recipe.tagIds) {
                db.execSQL(
                    """INSERT OR IGNORE INTO recipe_tag_cross_ref (recipeId, tagId)
                       VALUES ('$recipeId', '$tagId')"""
                )
            }
        }
    }

    private fun uuid() = UUID.randomUUID().toString()
    private fun String.esc() = this.replace("'", "''")
}
