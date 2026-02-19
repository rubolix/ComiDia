# 🍽️ ComiDia

**ComiDia** (*comida* + *día* — food + day) is an Android app for weekly meal planning, built with Kotlin and Jetpack Compose.

Plan your family's meals, organize your recipe collection, set nutritional goals, and never wonder "what's for dinner?" again.

## Features

### 📋 Weekly Menu
- Plan dinners (default), with optional breakfast, lunch, snacks, and other meals
- Add **multiple dishes per meal** (main + sides)
- **Whole-week items** for things that apply all week (e.g., "keep fruit bowl stocked")
- **Goal compliance indicators** showing whether weekly nutrition targets are met
- Navigate between weeks

### 📅 Calendar View
- Monthly calendar with dot indicators for planned meals
- Tap any day to see its meal details
- Quick visual overview of your planning coverage

### 🍳 Recipe Library
- Full recipe management: name, ingredients, instructions, prep/cook time, servings
- **Tag system**: Fish, Vegetarian, Quick, Italian, Mexican, etc.
- **User-defined categories** for custom organization
- **View modes**: Latest, By Tags, By Categories
- **3-dot menu** per recipe: Copy, Archive, Delete
- **Detail view** with full recipe information
- Search and filter by tag
- Pre-loaded with 20 family-sized recipes (12 mains + 8 sides)

### 🥕 Ingredients
- Browse all ingredients across your recipe collection
- Grouped by category

### ⚙️ Settings & Goals
- Define weekly meal plan goals:
  - *"At least 1 fish meal per week"*
  - *"At most 2 pasta dishes per week"*
  - *"Vegetarian meals at least 2 per week"*
- Toggle goals on/off
- Goals are evaluated in real-time on the Menu screen

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

## Project Structure

```
app/src/main/java/com/rubolix/comidia/
├── ComiDiaApp.kt              # Hilt application
├── MainActivity.kt            # Entry point
├── data/
│   ├── local/
│   │   ├── ComiDiaDatabase.kt # Room database
│   │   ├── SeedDatabaseCallback.kt # 20 pre-loaded recipes
│   │   ├── dao/               # Data access objects
│   │   └── entity/            # Room entities
│   └── repository/            # Repository pattern
├── di/                        # Hilt modules
└── ui/
    ├── ComiDiaAppUI.kt        # Root composable + navigation
    ├── theme/                  # Material 3 theme (saffron/olive/terracotta)
    ├── navigation/             # Screen routes + bottom nav
    └── screens/
        ├── mealplan/           # Weekly menu
        ├── calendar/           # Monthly calendar
        ├── recipes/            # Recipe list, detail, edit
        ├── ingredients/        # Ingredient browser
        └── settings/           # Goals management
```

## Roadmap

- [x] Recipe management with tags
- [x] Weekly meal planner with multi-dish support
- [x] Calendar view
- [x] Weekly meal plan goals
- [ ] Shopping list generation (aggregate ingredients for the week)
- [ ] Smart recipe suggestions based on goals
- [ ] OneDrive sync
- [ ] Meal prep instructions

## License

MIT
