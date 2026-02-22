package com.rubolix.comidia.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class RecipeWithTags(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<IngredientEntity>
)

data class RecipeFull(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<IngredientEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecipeCategoryCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<RecipeCategoryEntity>
)

data class RecipeWithTagsAndCategories(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecipeCategoryCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<RecipeCategoryEntity>
)

data class RecipeWithLeftoverInfo(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val crossRef: MealSlotRecipeCrossRef
)

data class MealSlotWithRecipes(
    @Embedded val mealSlot: MealSlotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            MealSlotRecipeCrossRef::class,
            parentColumn = "mealSlotId",
            entityColumn = "recipeId"
        )
    )
    val recipes: List<RecipeEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealSlotId"
    )
    val recipeRefs: List<MealSlotRecipeCrossRef>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealSlotId"
    )
    val customEntries: List<MealSlotCustomEntry>
)
