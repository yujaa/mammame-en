package ui

import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.rgba // <-- ✅ [추가] rgba import
import kotlin.math.roundToInt

fun verdictLabel(v: String) = when (v.lowercase()) {
    "avoid" -> "Avoid"
    "caution" -> "Caution"
    "conditional" -> "Conditional"
    "safe" -> "Safe"
    else -> v
}

fun verdictColor(v: String) = when (v.lowercase()) {
    "avoid" -> Color("#ef4444")       // red
    "caution" -> Color("#f59e0b")     // orange
    "conditional" -> Color("#06b6d4") // teal
    "safe" -> Color("#22c55e")        // green
    else -> Color.lightgray
}

fun verdictBgRgba(v: String, alpha: Float = 0.15f) = when (v.lowercase()) {
    "avoid" -> rgba(239, 68, 68, alpha)
    "caution" -> rgba(245, 158, 11, alpha)
    "conditional" -> rgba(6, 182, 212, alpha)
    "safe" -> rgba(34, 197, 94, alpha)
    else -> rgba(211, 211, 211, alpha)
}
fun allVerdictTabs(): List<Pair<String, String>> {
    return listOf(
        "all" to "전체",
        "safe" to verdictLabel("safe"),
        "conditional" to verdictLabel("conditional"),
        "caution" to verdictLabel("caution"),
        "avoid" to verdictLabel("avoid")
    )
}

fun fmt1(n: Double): String = (kotlin.math.round(n * 10) / 10.0).toString()



