# 🍽️ ComiDia

**ComiDia** (*comida* + *día* — food + day) is an Android app for weekly meal planning, built with Kotlin and Jetpack Compose.

Plan your family's meals, organize your recipe collection, set nutritional goals, and generate your weekly shopping list — never wonder "what's for dinner?" again.

## Features

### 📋 Weekly Menu
- Plan dinners (default), with optional breakfast, lunch, snacks, and other meals
- Per-day 3-dot menu to show/hide meal types (checkmark indicators)
- Add **multiple dishes per meal** (main + sides)
- **Per-day to-do items** (e.g., "defrost chicken", "marinate pork")
- **Whole-week items** for things that apply all week (e.g., "keep fruit bowl stocked")
- **Goal status icon** next to Whole Week title — green ✓ when all met, orange ⚠ when unmet; click to expand details
- Navigate between weeks with arrow buttons

### 📅 Calendar View
- Monthly calendar with dot indicators for planned meals
- Tap any day to see its meal details
- Quick visual overview of your planning coverage

### 🍳 Recipe Library
- Full recipe management: name, ingredients, instructions, prep/cook/total time, servings
- **Star rating** (1-5 stars) for rating your recipes
- **Source URL** to link to original online recipes
- **Notes** field for tips and variations
- **Tag system**: Fish, Vegetarian, Quick, Italian, Mexican, etc.
- **User-defined categories** for custom organization
- **View modes**: Latest, By Tags, By Categories
- **3-dot menu** per recipe: Copy, Archive, Delete
- **Detail view** with full recipe information including ratings, URL links, and notes
- Search and filter by tag
- Pre-loaded with 20 family-sized recipes (12 mains + 8 sides) with ratings, notes, and categorized ingredients

### 🛒 Shopping List (Ingredients)
- Automatically pulls ingredients from all recipes planned for the selected week
- **Sort modes**: By Day, Alphabetical, By Food Type (Produce, Protein, Dairy, Pantry, etc.)
- **Smart quantity combining** — sums amounts when multiple recipes use the same ingredient
- Fraction math support (1/2 + 1/2 = 1)
- Week navigation to view shopping lists for different weeks

### ⚙️ Settings
- **Calendar Configuration**:
  - First day of week (Monday or Sunday)
  - Default visible meal types (which meals show by default on each day)
- **Weekly Balance** (meal variety goals):
  - Choose from existing tags (no manual typing)
  - Comparison: Equal to, At least, At most (default: Equal to)
  - Advanced: per day, per week, or per month period
  - Toggle goals on/off
  - Goals evaluated in real-time with visual indicators on the Menu screen

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository pattern |
| Database | Room (SQLite) |
| DI | Hilt |
| Navigation | Compose Navigation |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Getting Started

### Prerequisites
- **JDK 17** — `brew install openjdk@17`
- **Android SDK** — Platform 34, Build Tools 34.0.0
- **Android Studio** (recommended) or command-line tools

### Build & Run

```bash
# Clone
git clone https://github.com/rubolix/ComiDia.git
cd ComiDia

# Set environment
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"

# Build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

Or just open the project in Android Studio and hit ▶️.

### Running Tests

```bash
# Unit tests (49 tests — entities, goal evaluation, quantity parsing)
./gradlew testDebugUnitTest

# Instrumented tests (45 tests — requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Run all
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

## Testing

### Unit Tests (49 tests)
- **Entity tests**: `RecipeEntity`, `MealPlanGoalEntity`, `IngredientEntity`, `AppSettingsEntity`, `TagEntity`, `MealSlotEntity`, `DailyTodoEntity`, `WeeklyItemEntity`, cross-reference entities
- **Goal evaluation logic**: all comparison types (`eq`, `gte`, `lte`), legacy compatibility (`min`, `max`)
- **Quantity parsing**: whole numbers, decimals, fractions, combining, edge cases

### Instrumented Tests (45 tests)
- **RecipeDao**: CRUD operations, search, archiving, copy, batch queries, full recipe with tags/ingredients
- **MealPlanDao**: meal slot management, multi-dish slots, recipe addition/removal, weekly items, daily todos, date range filtering
- **GoalDao**: insert/retrieve, active filtering, toggle, new fields (comparison, period)
- **SettingsDao**: key-value storage, overwrite, first-day-of-week, default meal types
- **Database integration**: end-to-end workflows (full meal plan, settings, goal tracking, archiving, multi-dish slots, categories)
- **Navigation**: route generation, bottom nav configuration

## Project Structure

```
app/src/main/java/com/rubolix/comidia/
├── ComiDiaApp.kt              # Hilt application
├── MainActivity.kt            # Entry point
├── data/
│   ├── local/
│   │   ├── ComiDiaDatabase.kt # Room database (v5)
│   │   ├── SeedDatabaseCallback.kt # 20 pre-loaded recipes
│   │   ├── dao/               # RecipeDao, MealPlanDao, GoalDao, SettingsDao
│   │   └── entity/            # Room entities + relations
│   └── repository/            # RecipeRepository, MealPlanRepository, GoalRepository, AppSettingsRepository
├── di/                        # Hilt modules
└── ui/
    ├── ComiDiaAppUI.kt        # Root composable + navigation graph
    ├── theme/                  # Material 3 theme (saffron/olive/terracotta)
    ├── navigation/             # Screen routes + bottom nav
    └── screens/
        ├── mealplan/           # Weekly menu + ViewModel
        ├── calendar/           # Monthly calendar
        ├── recipes/            # Recipe list, detail, edit + ViewModels
        ├── ingredients/        # Shopping list + ViewModel
        └── settings/           # Settings hub, Calendar config, Weekly Balance

app/src/test/                   # Unit tests (49 tests)
app/src/androidTest/            # Instrumented tests (45 tests)
```

## Roadmap

- [x] Recipe management with tags, ratings, URLs, notes
- [x] Weekly meal planner with multi-dish support
- [x] Per-day to-do items
- [x] Calendar view
- [x] Weekly Balance goals with comparison types and periods
- [x] Shopping list with smart ingredient combining
- [x] Settings: calendar config + weekly balance
- [x] Comprehensive test suite (94 tests)
- [ ] Recipe image support (camera/gallery)
- [ ] Smart recipe suggestions based on goals
- [ ] OneDrive sync
- [ ] Meal prep instructions
- [ ] Export shopping list (share/copy)

## License

MIT
