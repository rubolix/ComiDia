package com.rubolix.comidia.util

object IngredientUtils {

    fun normalizeIngredientName(name: String): String {
        val prepWords = setOf(
            "chopped", "sliced", "diced", "minced", "grated", "peeled", "melted", 
            "crushed", "shredded", "dried", "fresh", "frozen", "thawed", "large", 
            "medium", "small", "thinly", "finely", "roughly", "softened"
        )
        
        return name.lowercase()
            .split(Regex("[\\s,]+"))
            .filter { it.isNotBlank() && it !in prepWords }
            .joinToString(" ")
            .trim()
            .removeSuffix("s")
    }

    fun isWater(name: String): Boolean {
        val norm = normalizeIngredientName(name)
        return norm == "water" || norm == "agua"
    }

    fun combineQuantities(quantities: List<String>, scaleFactors: List<Double>): String {
        val nums = quantities.indices.mapNotNull { i ->
            parseFraction(quantities[i])?.let { it * scaleFactors[i] }
        }
        return if (nums.size == quantities.size && nums.isNotEmpty()) {
            val sum = nums.sum()
            if (sum == sum.toInt().toDouble()) sum.toInt().toString()
            else String.format("%.1f", sum)
        } else {
            quantities.indices.mapNotNull { i ->
                if (quantities[i].isBlank()) null
                else if (scaleFactors[i] == 1.0) quantities[i]
                else quantities[i] + " (scaled)"
            }.joinToString(" + ")
        }
    }

    fun parseFraction(s: String): Double? {
        val trimmed = s.trim()
        if (trimmed.isBlank()) return null
        trimmed.toDoubleOrNull()?.let { return it }
        val parts = trimmed.split("/")
        if (parts.size == 2) {
            val num = parts[0].trim().toDoubleOrNull() ?: return null
            val den = parts[1].trim().toDoubleOrNull() ?: return null
            if (den != 0.0) return num / den
        }
        return null
    }
}
