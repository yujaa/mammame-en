package data

import kotlinx.browser.window
import kotlinx.coroutines.await

// ───────────────── CSV Parser ─────────────────

private fun splitCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        when (val ch = line[i]) {
            '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                else inQuotes = !inQuotes
            }
            ',' -> if (inQuotes) sb.append(ch) else { out.add(sb.toString()); sb.clear() }
            else -> sb.append(ch)
        }
        i++
    }
    out.add(sb.toString())
    return out
}

private fun parseCsv(csv: String): List<Map<String, String>> {
    val lines = csv.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()
    val header = splitCsvLine(lines.first()).map { it.trim() }
    return lines.drop(1).map { line ->
        val cells = splitCsvLine(line)
        header.zip(cells + List(maxOf(0, header.size - cells.size)) { "" })
            .associate { (h, v) -> h.trim() to v.trim() }
    }
}

// ─────────────── Util Parser ────────────────

private fun parseReliability(raw: String?): Double {
    val s = (raw ?: "").trim().lowercase()
    if (s.isBlank()) return 1.0
    s.toDoubleOrNull()?.let { return it.coerceIn(0.5, 5.0) }
    return when (s) {
        "high", "gov", "medical" -> 3.0
        "medium", "mid", "news"  -> 2.0
        "low", "blog"            -> 1.0
        else -> 1.0
    }
}

private fun isTrue(raw: String?): Boolean {
    val s = (raw ?: "").trim().lowercase()
    return s in setOf("true", "1", "y", "yes", "있음")
}

private fun normalizedVerdict(v: String?): String? = when (v?.trim()?.lowercase()) {
    "avoid", "피해야 함", "피함" -> "avoid"
    "caution", "주의" -> "caution"
    "conditional", "조건부" -> "conditional"
    "safe", "안전", "none", "없음" -> "safe"
    else -> null
}

private fun verdictRank(v: String?): Int = when (v) {
    "avoid" -> 3
    "caution" -> 2
    "conditional" -> 1
    "safe" -> 0
    else -> -1
}

// ────── Main Loader: foods.csv → List<FoodSafetySummary> ──────

suspend fun loadFoodsFromCsv(path: String = "foods.csv"): List<FoodSafetySummary> {
    val response = window.fetch(path).await()
    val text = response.text().await()
    val rows = parseCsv(text)

    // CSV column names (adjust if your CSV differs)
    val FOOD = "food_kr"            // Food name in Korean (group key)
    val REL  = "source_reliability" // Reliability (number/label)
    val SRC  = "source_name"        // Display source name
    val URL  = "rule_source"        // Source URL
    val P_HAS = "임산부_정보유무"      // TRUE/FALSE
    val P_V   = "임산부_주의"          // avoid/caution/conditional/safe (Korean also allowed)

    // [1] Group by food name
    val grouped = rows.groupBy { it[FOOD].orEmpty() }.filterKeys { it.isNotBlank() }

    return grouped.map { (foodName, items) ->

        // [2] Convert rows to valid entries
        val allEntries = items.mapNotNull { r ->
            if (!isTrue(r[P_HAS])) return@mapNotNull null
            val verdict = normalizedVerdict(r[P_V]) ?: return@mapNotNull null

            FoodSafetyEntry(
                source = r[SRC].orEmpty(),
                verdict = verdict,                      // "avoid"/"caution"/"conditional"/"safe"
                reliability = parseReliability(r[REL]), // Double
                note = r[P_V],                          // Keep original label
                url = r[URL]
            )
        }

        // [3] Deduplicate by URL (or source if URL is missing):
        // Keep only one per group: strongest warning + highest reliability
        val deduped = allEntries
            .groupBy { (it.url?.takeIf { u -> u.isNotBlank() } ?: it.source).trim() }
            .values
            .map { group ->
                group.maxWith(
                    compareBy<FoodSafetyEntry> { verdictRank(it.verdict) }
                        .thenByDescending { it.reliability }
                )
            }

        FoodSafetySummary(
            name = foodName,
            entries = deduped.sortedByDescending { it.reliability } // Sort for readability
        )
    }.sortedBy { it.name }
}

// ────── synonyms.csv → Map<String, List<String>> ──────
suspend fun loadSynonymsFromCsv(path: String = "synonyms.csv"): Map<String, List<String>> {
    val response = window.fetch(path).await()
    val text = response.text().await()

    val lines = text.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyMap()

    // header: key,aliases
    val header = splitCsvLine(lines.first()).map { it.trim() }
    val keyIdx = header.indexOf("key")
    val aliasIdx = header.indexOf("aliases")
    if (keyIdx < 0 || aliasIdx < 0) return emptyMap()

    val map = mutableMapOf<String, List<String>>()

    for (line in lines.drop(1)) {
        val cells = splitCsvLine(line)
        if (cells.size <= maxOf(keyIdx, aliasIdx)) continue

        val key = cells[keyIdx].trim()
        val aliasesRaw = cells[aliasIdx].trim().trim('"')

        if (key.isBlank()) continue

        val aliases = aliasesRaw
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != key }

        map[key] = aliases
    }

    return map
}

//----------- token --------------------//

// Build meaningful tokens (1~4 chars) based on DB: only short names that actually exist in foods.csv
fun buildMeaningfulTokens1to4(summaries: List<FoodSafetySummary>): Set<String> =
    summaries
        .map { it.name.trim() }
        .filter { it.isNotBlank() }
        .filter { it.length in 1..4 }
        .filter { it.all { ch -> ch in '\uAC00'..'\uD7A3' } } // Hangul only (relax if needed)
        .toSet()

// Split query into 1~4 substrings, but use only tokens that exist in DB as hints
fun extractQueryHintsFromDb(query: String, meaningful: Set<String>): Set<String> {
    val q = query.trim().lowercase().replace(Regex("\\s+"), "")
    if (q.isEmpty()) return emptySet()

    val out = mutableSetOf<String>()
    val L = q.length

    for (n in 1..4) {
        if (L < n) continue
        for (i in 0..(L - n)) {
            val sub = q.substring(i, i + n)
            if (sub in meaningful) out.add(sub)
        }
    }

    // If there is any hint of length >=2, allow 1-char hints too (e.g., 팥빙수 → 팥)
    val has2plus = out.any { it.length >= 2 }
    return if (has2plus) out else out.filter { it.length == 1 }.toSet()
}
