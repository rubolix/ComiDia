package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class AppSettingsEntityTest {

    @Test
    fun constants_areCorrect() {
        assertEquals("first_day_of_week", AppSettingsEntity.FIRST_DAY_OF_WEEK)
        assertEquals("default_meal_types", AppSettingsEntity.DEFAULT_MEAL_TYPES)
    }

    @Test
    fun keyValue_constructionWorks() {
        val setting = AppSettingsEntity("test_key", "test_value")
        assertEquals("test_key", setting.key)
        assertEquals("test_value", setting.value)
    }
}
