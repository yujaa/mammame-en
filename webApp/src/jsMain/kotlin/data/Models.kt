package data

import kotlin.math.round

// CSV line
data class FoodSafetyEntry(
    val source: String,       // rule_source
    val verdict: String,      // "avoid" | "caution" | "conditional" | "safe"
    val reliability: Double,  // source_reliability
    val note: String? = null, // note
    val url: String? = null   // source_url
)

// food summary
data class FoodSafetySummary(
    val name: String,                 // food_kr
    val entries: List<FoodSafetyEntry>
) {
    private val valid = entries.filter { it.reliability > 0.0 }

    val count: Int get() = valid.size

    val weightedAverage: Double get() {
        val denom = valid.sumOf { it.reliability }
        if (denom <= 0.0) return 0.0
        val num = valid.sumOf { scoreOf(it.verdict) * it.reliability }
        return num / denom
    }
    val scoreRounded: Int get() = round(weightedAverage).toInt()

    private fun scoreOf(v: String): Int = when (v.lowercase()) {
        "avoid" -> 0
        "caution" -> 1
        "conditional" -> 2
        "safe" -> 3
        else -> 0
    }
}
