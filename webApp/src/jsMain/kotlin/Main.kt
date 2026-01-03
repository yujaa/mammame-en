import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import data.FoodSafetyEntry
import data.FoodSafetySummary
import data.loadFoodsFromCsv
import data.loadSynonymsFromCsv
import org.jetbrains.compose.web.attributes.InputType
import ui.SourceListTab
import ui.VerticalVerdictTabs
import ui.TriGroupRecommendationRow
import ui.buildReliabilityAwareSummary
import ui.verdictLabel
import kotlin.math.max
import org.w3c.dom.HTMLInputElement
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay

import org.jetbrains.compose.web.css.Style
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import ui.AppStyles
import androidx.compose.runtime.NoLiveLiterals
import kotlinx.coroutines.await


// -------------------
// GA4 Integration Utilities
// -------------------
external fun gtag(command: String, eventName: String, parameters: dynamic)

/**
 * Logs the search term to Google Analytics 4
 */
private fun logSearchEvent(searchTerm: String) {
    if (searchTerm.isBlank()) return
    val params = js("{}")
    params["search_term"] = searchTerm

    try {
        gtag("event", "search", params)
        println("GA4 Log: Search term sent -> $searchTerm")
    } catch (e: Throwable) {
        println("GA4 Error: ${e.message}")
    }
}

// -------------------
// Normalization & String Utilities
// -------------------

/**
 * Normalizes a string by converting to lowercase and removing all whitespace
 */
private fun norm(s: String) = s.lowercase().replace(Regex("\\s+"), "")

private fun tokensBySpace(s: String): List<String> =
    s.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

/**
 * Checks if a character is a Korean Hangul syllable
 */
private fun isHangul(c: Char): Boolean = c in '\uAC00'..'\uD7A3'

/**
 * Calculates the length of consecutive Hangul characters immediately following the match
 */
private fun trailingHangulRunLen(haystack: String, needle: String): Int {
    val h = haystack.lowercase()
    val n = needle.lowercase()
    val idx = h.indexOf(n)
    if (idx < 0) return 0
    var j = idx + n.length
    var cnt = 0
    while (j < h.length && isHangul(h[j])) {
        cnt++
        j++
    }
    return cnt
}

/**
 * Checks if the search term matches at a Korean word boundary (not in the middle of a word)
 */
private fun containsHangulBoundary(haystack: String, needle: String): Boolean {
    if (needle.isBlank()) return false
    val h = haystack.lowercase()
    val n = needle.lowercase()

    var idx = h.indexOf(n)
    while (idx >= 0) {
        val beforeOk = (idx == 0) || !isHangul(h[idx - 1])
        val afterIdx = idx + n.length
        val afterOk = (afterIdx >= h.length) || !isHangul(h[afterIdx])
        if (beforeOk && afterOk) return true
        idx = h.indexOf(n, startIndex = idx + 1)
    }
    return false
}

/**
 * Calculates a relevance score for ranking search results
 */
private fun relevance(name: String, query: String, datasetScore: Double): Double {
    val n = norm(name)
    val q = norm(query)
    if (n == q) return 1000.0 + datasetScore
    var score = 0.0
    if (n.startsWith(q)) score += 200.0
    if (n.contains(q)) score += 120.0

    val nt = tokensBySpace(name).map { it.lowercase() }.toSet()
    val qt = tokensBySpace(query).map { it.lowercase() }.toSet()
    if (qt.isNotEmpty()) {
        val inter = nt.intersect(qt).size.toDouble()
        val ratio = inter / max(1, qt.size)
        score += 100.0 * ratio
    }

    val extraLen = (n.length - q.length).coerceAtLeast(0)
    score += 80.0 / (1 + extraLen)
    score += datasetScore * 10.0
    return score
}

private fun norm2(s: String) = s.lowercase().replace(Regex("\\s+"), "")

/**
 * Expands the query using a synonym map
 */
private fun expandQuery(q: String, synonyms: Map<String, List<String>>): List<String> {
    val base = q.trim()
    if (base.isBlank()) return emptyList()
    val n = norm2(base)

    return buildList {
        add(base)
        if (n != base) add(n)
        synonyms[base]?.let { addAll(it) }
        synonyms[n]?.let { addAll(it) }
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

/**
 * Builds a set of all 2-4 character substrings from the food database for hint extraction
 */
private fun buildMeaningfulTokens2to4FromAllNames(summaries: List<FoodSafetySummary>): Set<String> {
    val out = mutableSetOf<String>()
    summaries.asSequence()
        .map { norm(it.name.trim()) }
        .filter { it.isNotBlank() }
        .forEach { n ->
            val L = n.length
            for (len in 2..4) {
                if (L < len) continue
                for (i in 0..(L - len)) {
                    val sub = n.substring(i, i + len)
                    out.add(sub)
                }
            }
        }
    return out
}

private fun buildMeaningfulWhole1gFromDb(summaries: List<FoodSafetySummary>): Set<String> =
    summaries
        .map { it.name.trim() }
        .filter { it.length == 1 && it.isNotBlank() }
        .map { norm(it) }
        .toSet()

/**
 * Extracts possible food keywords (hints) from a long user query
 */
private fun extractHintsFromDbTokens(
    query: String,
    meaningful2to4: Set<String>,
    meaningfulWhole1g: Set<String>
): List<String> {
    val q = norm(query)
    if (q.isBlank()) return emptyList()

    val found2to4 = mutableSetOf<String>()
    val L = q.length
    for (len in 2..4) {
        if (L < len) continue
        for (i in 0..(L - len)) {
            val sub = q.substring(i, i + len)
            if (sub in meaningful2to4) found2to4.add(sub)
        }
    }

    val found1 = mutableSetOf<String>()
    for (ch in q) {
        val s = ch.toString()
        if (s in meaningfulWhole1g) found1.add(s)
    }

    val filtered1 = if (found2to4.isNotEmpty()) {
        found1.filterNot { one ->
            found2to4.any { it.contains(one) }
        }.toSet()
    } else {
        found1
    }

    val combined = (found2to4 + filtered1)
    if (combined.isEmpty()) return emptyList()

    return combined.sortedWith(
        compareBy<String> { q.indexOf(it) }.thenByDescending { it.length }
    )
}

/**
 * Bucket-based ranking key to prioritize exact matches and boundary matches
 */
private data class RankKey( val bucket: Int, val score: Double, val nameLenDesc: Int)

private fun rankKey(
    s: FoodSafetySummary,
    query: String,
    expanded: List<String>,
    orderedHints: List<String>
): RankKey {
    val name = s.name
    val nName = norm(name)
    val nQuery = norm(query)

    if (nName == nQuery) {
        return RankKey(bucket = 0, score = 1_000_000.0 + s.scoreRounded, nameLenDesc = -name.length)
    }

    // Bucket 1: Name is exactly one of the extracted hints
    if (orderedHints.isNotEmpty()) {
        val idx = orderedHints.indexOfFirst { h -> norm(h) == nName }
        if (idx >= 0) {
            val hint = orderedHints[idx]
            val bonus = (10_000 - idx * 500) + hint.length * 50
            return RankKey(bucket = 1, score = 500_000.0 + bonus + s.scoreRounded, nameLenDesc = -name.length)
        }
    }

    // Bucket 2: Name contains an extracted hint
    if (orderedHints.isNotEmpty()) {
        val hitInfos = orderedHints.mapNotNull { h ->
            if (nName.contains(norm(h))) {
                val pos = nName.indexOf(norm(h))
                Triple(h, pos, h.length)
            } else null
        }

        if (hitInfos.isNotEmpty()) {
            val bestHint = hitInfos.minWith(compareBy<Triple<String, Int, Int>> {
                orderedHints.indexOf(it.first)
            }.thenBy { it.second })

            val hintOrder = orderedHints.indexOf(bestHint.first).coerceAtLeast(0)
            val hintLen = bestHint.third
            val boundaryBonus = if (containsHangulBoundary(name, bestHint.first)) 2_000.0 else 0.0
            val bonus = (5_000 - hintOrder * 300) + hintLen * 200 + boundaryBonus
            return RankKey(bucket = 2, score = 200_000.0 + bonus + relevance(name, query, s.scoreRounded.toDouble()), nameLenDesc = -name.length)
        }
    }

    // Bucket 3: Matches via synonym expansion with word boundary
    val expOnly = expanded.filter { norm2(it) != nQuery }
    if (expOnly.any { containsHangulBoundary(name, it) }) {
        return RankKey(bucket = 3, score = 80_000.0 + relevance(name, query, s.scoreRounded.toDouble()), nameLenDesc = -name.length)
    }

    // Bucket 4: Partial substring match with penalty for long trailing characters
    if (name.contains(query.trim(), ignoreCase = true)) {
        val tail = trailingHangulRunLen(name, query.trim())
        val penalty = 120.0 * tail
        return RankKey(bucket = 4, score = 10_000.0 + relevance(name, query, s.scoreRounded.toDouble()) - penalty, nameLenDesc = -name.length)
    }

    return RankKey(bucket = 9, score = relevance(name, query, s.scoreRounded.toDouble()), nameLenDesc = -name.length)
}

private fun verdictRank(v: String?): Int = when (v?.lowercase()) {
    "avoid" -> 3
    "caution" -> 2
    "conditional" -> 1
    "safe" -> 0
    else -> -1
}

// -------------------
// Entry Point
// -------------------
fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
@NoLiveLiterals
fun App() {
    Style(AppStyles)   // Inject global CSS styles

    var query by remember { mutableStateOf("") }
    var summaries by remember { mutableStateOf<List<FoodSafetySummary>>(emptyList()) }
    var synonyms by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var manualAiTrigger by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    var delayedShowAiButton by remember { mutableStateOf(false) }

    // Initial Data Loading
    LaunchedEffect(Unit) {
        try {
            summaries = loadFoodsFromCsv("foods.csv")
            synonyms = loadSynonymsFromCsv("synonyms.csv")
            error = null
        } catch (e: Throwable) {
            error = e.message ?: "Failed to load data"
        } finally {
            loading = false
        }
    }

    // Search GA Logging with 0.4s debouncing
    LaunchedEffect(query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length >= 2) {
            delay(400)
            logSearchEvent(trimmedQuery)
        }

        // Reset AI states when a new search starts
        manualAiTrigger = false
        aiResponse = null
        isAiLoading = false

    }

    // Pre-calculate meaningful tokens for optimized searching
    val meaningful2to4 =
        remember(summaries) { buildMeaningfulTokens2to4FromAllNames(summaries) }
    val meaningfulWhole1g = remember(summaries) { buildMeaningfulWhole1gFromDb(summaries) }

    // Core Filtering and Ranking Logic
    val ranked = remember(query, summaries, synonyms, meaningful2to4, meaningfulWhole1g) {
        if (query.isBlank()) emptyList()
        else {
            val expanded = expandQuery(query, synonyms)
            val orderedHints =
                extractHintsFromDbTokens(query, meaningful2to4, meaningfulWhole1g)

            val candidates = summaries.filter { s ->
                val nName = norm(s.name)
                val nQuery = norm(query)
                (nName == nQuery) ||
                        expanded.any { qx -> nName.contains(norm(qx)) } ||
                        orderedHints.any { h -> nName.contains(norm(h)) }
            }

            candidates.sortedWith(
                compareBy<FoodSafetySummary> { rankKey(it, query, expanded, orderedHints).bucket }
                    .thenByDescending { rankKey(it, query, expanded, orderedHints).score }
                    .thenBy { rankKey(it, query, expanded, orderedHints).nameLenDesc }
                    .thenByDescending { it.scoreRounded }
            )
        }
    }

    // AI Fallback Request Logic
    LaunchedEffect(manualAiTrigger, query) {
        if (manualAiTrigger && query.trim().isNotEmpty()) {
            isAiLoading = true
            aiResponse = null
            try {
                val options = js("{}")
                options.method = "POST"
                options.headers = js("{}")
                options.headers["Content-Type"] = "application/json; charset=utf-8"

                val bodyData = js("{}")
                bodyData.query = query.trim()
                options.body = kotlin.js.JSON.stringify(bodyData)

                val response = window.fetch("http://localhost:8080/ai/fallback", options).await()
                if (response.ok) {
                    val json = response.json().await()
                    aiResponse = json.asDynamic().text.toString()
                } else {
                    aiResponse = "Could not fetch AI response."
                }
            } catch (e: Exception) {
                aiResponse = "Connection Error: ${e.message}"
            } finally {
                isAiLoading = false
            }
        }
    }

    val visibleRanked = remember(ranked) { ranked.filter { it.entries.isNotEmpty() } }

    // Logic to determine if the search result is a "Strong Match"
    val topMatch = visibleRanked.firstOrNull()
    val isStrongMatch = remember(query, visibleRanked) {
        val top = visibleRanked.firstOrNull()
        if (top == null) false
        else {
            val nName = norm(top.name)
            val nQuery = norm(query)
            nName == nQuery || nName.startsWith(nQuery) // Strong match if exact name or name starts with query
        }
    }

    val showAiSuggestion = query.trim().isNotEmpty() && (!isStrongMatch || visibleRanked.isEmpty())

    // Delay AI button visibility to avoid flickering during typing
    LaunchedEffect(query) {
        val q = query.trim()

        delayedShowAiButton = false
        manualAiTrigger = false
        aiResponse = null
        isAiLoading = false

        if (q.length >= 2) {
            delay(500)
            // Show AI button only if no strong match found or results are empt
            if (!isStrongMatch || visibleRanked.isEmpty()) {
                delayedShowAiButton = true
            }
        }
    }


    Div({ classes(AppStyles.container) }) {
        SearchHeaderCard(
            title = "Mamma-Me: Pregnancy Food Safety Search",
            subtitle =  "Summarized information based on reliability from multiple sources.",
            query = query,
            onChange = { query = it },
            onClear = { query = "" }
        )

        // 1. AI Loading State
        if (isAiLoading) {
            Div({
                style {
                    padding(30.px); marginBottom(24.px); borderRadius(20.px)
                    backgroundColor(Color("#f8fafc")); border(1.px, LineStyle.Solid, Color("#e2e8f0"))
                    textAlign("center")
                }
            }) {
                Span({ style { fontSize(24.px); display(DisplayStyle.Block); marginBottom(12.px) } }) { Text("⏳") }
                P({ style { margin(0.px); color(Color("#64748b")); fontWeight("600") } }) { Text("AI is generating a response...") }
            }
        }

        // 2. AI Response Display
        if (aiResponse != null && !isAiLoading) {
            Div({
                style {
                    padding(24.px); marginBottom(32.px); borderRadius(20.px)
                    backgroundColor(Color("#ffffff")); border(1.px, LineStyle.Solid, Color("#e2e8f0"))
                    property("box-shadow", "0 10px 15px -3px rgba(0, 0, 0, 0.04)")
                }
            }) {
                Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); marginBottom(16.px); gap(8.px) } }) {
                    Span({ style { fontSize(20.px) } }) { Text("🤖") }
                    B({ style { color(Color("#1e293b")); fontSize(16.px) } }) { Text("AI Recommendation") }
                }
                P({
                    style {
                        margin(0.px); color(Color("#334155")); fontSize(15.px); lineHeight("1.8")
                        property("white-space", "pre-wrap")
                    }
                }) { Text(aiResponse!!) }

                Div({
                    style {
                        marginTop(20.px); paddingTop(16.px)
                        property("border-top", "1px solid #f1f5f9")
                    }
                }) {
                    P({ style { fontSize(12.px); color(Color("#94a3b8")); margin(0.px) } }) {
                        Text("※ AI responses are for reference only. Please consult a professional.")
                    }
                }
            }
        }

        // 3. AI Suggestion Button (appears when search result is poor)
        if (delayedShowAiButton && aiResponse == null && !isAiLoading) {
            Div({
                style {
                    padding(20.px); marginBottom(24.px); borderRadius(16.px)
                    backgroundColor(Color("#f0fdfa")); border(1.px, LineStyle.Solid, Color("#ccfbf1")); textAlign("center")
                    property("animation", "fadeIn 0.4s ease-in-out")
                }
            }) {
                P({ style { margin(0.px); fontSize(15.px); color(Color("#134e4a")); fontWeight("600"); marginBottom(12.px) } }) {
                    Text("Can't find what you're looking for? \uD83E\uDD14")
                }
                Button({
                    style {
                        padding(10.px, 20.px); borderRadius(10.px); border(0.px)
                        backgroundColor(Color("#0d9488")); color(Color.white); cursor("pointer"); fontWeight("600")
                    }
                    onClick { manualAiTrigger = true }
                }) { Text("View AI Guidelines") }
            }
        }

        // Results Panel
        Div({classes(AppStyles.resultsPanel)  }) {
            when {
                loading -> P { Text("Loading...") }
                error != null -> P({ style { color(Color.red) } }) { Text("Error: $error") }

                query.isNotBlank() && visibleRanked.isEmpty() ->
                    P({ style { color(Color.red) } }) { Text("No results found.") }

                query.isNotBlank() -> visibleRanked.forEach { summary ->
                    var selectedTab by remember(query, summary.name) { mutableStateOf("all") }

                    // Food Result Card
                    Div({ classes(AppStyles.resultCard) }) {
                        H2({
                            style {
                                marginTop(0.px)
                                color(Color("#1f2937"))
                                fontSize(24.px)
                            }
                        }) { Text("${summary.name}") }

                        // Reliability-aware Summary Text
                        val summaryText = buildReliabilityAwareSummary(summary.entries).trim()
                        val entries = summary.entries
                        val total = entries.size.coerceAtLeast(1)
                        val avoid = entries.count { it.verdict.equals("avoid", true) }
                        val caution = entries.count { it.verdict.equals("caution", true) }
                        val conditional = entries.count { it.verdict.equals("conditional", true) }

                        fun pct(n: Int) = n * 100.0 / total
                        val (bgColor, fgColor) = when {
                            pct(avoid) >= 40.0 -> Pair("#fee2e2", "#b91c1c")
                            pct(caution) >= 40.0 -> Pair("#fef3c7", "#92400e")
                            pct(conditional) >= 40.0 -> Pair("#e0f2fe", "#075985")
                            else -> Pair("#dcfce7", "#166534")
                        }

                        if (summaryText.isNotBlank()) {
                            Div({
                                style {
                                    backgroundColor(Color(bgColor)); borderRadius(10.px)
                                    padding(14.px); marginTop(10.px); marginBottom(12.px)
                                    property("border", "1px solid rgba(0,0,0,.04)")
                                }
                            }) {
                                P({
                                    style {
                                        margin(0.px); color(Color(fgColor)); fontSize(15.px)
                                        lineHeight("1.5"); fontWeight("600")
                                    }
                                }) { Text(summaryText) }
                            }
                        }

                        // Recommendation Row (High/Mid/Low reliability groups)
                        TriGroupRecommendationRow(summary.entries)

                        // Tabs and Source List Filtering
                        val sortedEntries = remember(summary.entries) {
                            summary.entries.sortedWith(
                                compareByDescending<FoodSafetyEntry> { verdictRank(it.verdict) }
                                    .thenByDescending { it.reliability }
                            )
                        }
                        val filteredEntries = remember(selectedTab, sortedEntries) {
                            if (selectedTab == "all") sortedEntries
                            else sortedEntries.filter { it.verdict.lowercase() == selectedTab }
                        }

                        // Tab Layout
                        Div({ classes(AppStyles.tabsArea) }) {
                            VerticalVerdictTabs(selectedTab) { tab -> selectedTab = tab }

                            Div({ classes(AppStyles.tabContent) }) {
                                P({
                                    style {
                                        property("font-weight", "bold")
                                        color(Color("#374151"))
                                        marginBottom(8.px)
                                        //cut the long title
                                        property("white-space", "nowrap")
                                        property("overflow", "hidden")
                                        property("text-overflow", "ellipsis")
                                    }
                                }) {
                                    val label = if (selectedTab == "all") "All Sources"
                                    else "${verdictLabel(selectedTab)} Sources"
                                    Text("🔍 $label (${filteredEntries.size})")
                                }

                                SourceListTab(filteredEntries)
                            }
                        }
                    }
                }

                else -> {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            flexWrap(FlexWrap.Wrap)
                            gap(20.px)
                            maxWidth(700.px)
                            width(100.percent)
                        }
                    }) {
                        Div({ style { textAlign("center") } }) {
                            P({
                                style {
                                    margin(0.px)
                                    marginBottom(6.px)
                                    color(Color("#0f172a"))
                                    fontSize(18.px)
                                    fontWeight("600")
                                }
                            }) { Text("Is this food safe to eat?") }

                            val foodsCount = summaries
                                .map { it.name.trim() }
                                .distinct()
                                .size

                            val sourcesCount = summaries
                                .flatMap { it.entries }
                                .mapNotNull { it.url?.trim()?.lowercase() }
                                .distinct()
                                .size

                            P({
                                style {
                                    // text
                                    marginTop(10.px)
                                    margin(0.px)
                                    fontSize(13.px)
                                    color(Color("#64748b"))
                                    textAlign("center")
                                }
                            }) {
                                Span({
                                    style {
                                        //number
                                        color(Color("#2563eb"))
                                        fontWeight("600")
                                    }
                                }) {
                                    Text("${sourcesCount} sources")
                                }

                                Text(" summarized for ")

                                Span({
                                    style {
                                        color(Color("#2563eb"))
                                        fontWeight("600")
                                    }
                                }) {
                                    Text("${foodsCount} foods")
                                }

                                Text(".")
                            }
                        }
                        Img(src = "main.png", attrs = {
                            style { width(180.px); height(180.px) }
                        })
                    }
                }
            }
            IntroContentSection()
        }
    }

    // Auto-blur search input on scroll to hide mobile keyboard
    DisposableEffect(Unit) {
        val handler: (Event) -> Unit = {
            val active = document.activeElement as? HTMLElement
            if (active?.id == "search-input") {
                (document.getElementById("search-input") as? HTMLInputElement)?.blur()
            }
        }

        window.addEventListener("scroll", handler)

        onDispose {
            window.removeEventListener("scroll", handler)
        }
    }
}
@Composable
private fun SearchHeaderCard(
    title: String,
    subtitle: String,
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Div({ classes(AppStyles.headerCard) }) {
        Div({ classes(AppStyles.headerRow) }) {
            Img(src = "logo.png", attrs = {
                classes(AppStyles.headerLogo)
            })

            Div({ classes(AppStyles.headerTexts) }) {
                H1({ classes(AppStyles.headerTitle) }) { Text(title) }
                Span({ classes(AppStyles.headerSubtitle) }) { Text(subtitle) }
            }
        }

        SearchBar(
            value = query,
            onChange = onChange,
            onClear = onClear,
            placeholder = "\"Enter food name (e.g., Sushi, Coffee, Tuna)"
        )

        P({ classes(AppStyles.disclaimerText) }) {
            Text("Mamma-Me does not provide medical advice. " +
                    "This is a summary service based on public information. " +
                    "Consult a healthcare professional for your specific condition.")
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String
) {
    Div({
        style {
            position(Position.Relative)
            marginTop(4.px)
            marginBottom(6.px)
        }
    }) {
        Span({
            style {
                position(Position.Absolute)
                left(12.px); top(0.px); bottom(0.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                color(Color("#94a3b8"))
            }
        }) { Text("🔍") }

        Input(type = InputType.Text, attrs = {
            id("search-input")
            value(value)
            onInput { onChange(it.value) }
            attr("placeholder", placeholder)
            if (value.isBlank()) attr("autofocus", "true")

            style {
                width(100.percent)
                fontSize(16.px)
                padding(14.px, 44.px, 14.px, 40.px)
                borderRadius(14.px)
                border(1.px, LineStyle.Solid, Color("#cbd5e1"))
                backgroundColor(Color("#f8fafc"))
                outline("none")
                property("box-sizing", "border-box")
            }

            onFocus {
                val el = it.target as HTMLInputElement
                el.style.border = "1px solid #60a5fa"
                el.style.boxShadow = "0 0 0 4px rgba(96,165,250,.2)"
                el.style.backgroundColor = "#ffffff"
            }
            onBlur {
                val el = it.target as HTMLInputElement
                el.style.border = "1px solid #cbd5e1"
                el.style.boxShadow = ""
                el.style.backgroundColor = "#f8fafc"
            }
        })

        if (value.isNotBlank()) {
            Button({
                onClick {
                    onClear()
                    (document.getElementById("search-input") as? HTMLInputElement)?.focus()
                }
                style {
                    position(Position.Absolute)
                    right(8.px); top(0.px); bottom(0.px)
                    border(0.px); backgroundColor(Color.transparent); cursor("pointer")
                    padding(0.px, 10.px); color(Color("#94a3b8")); fontSize(18.px)
                }
            }) { Text("✕") }
        }
    }
}

@Composable
private fun IntroContentSection() {
    Div({
        style {
            maxWidth(760.px)
            width(100.percent)
            marginTop(22.px)
            padding(18.px)
            borderRadius(16.px)
            backgroundColor(Color("#ffffff"))
            border(1.px, LineStyle.Solid, Color("#e5e7eb"))
            property("box-shadow", "0 1px 8px rgba(15,23,42,.04)")
        }
    }) {
        H2({
            style {
                marginTop(0.px)
                marginBottom(10.px)
                fontSize(18.px)
                color(Color("#0f172a"))
            }
        }) { Text("What is Mamma-Me?") }

        P({
            style {
                marginTop(0.px)
                marginBottom(12.px)
                fontSize(14.px)
                color(Color("#374151"))
                lineHeight("1.6")
            }
        }) {
            Text("It is a search and summary service that gathers food safety information " +
                    "from various public sources for pregnant women and parents. ")
            Text("Mamma-Me categorizes source reliability into 3 levels to help you make informed decisions.")

        }

        // Usage Guide
        Div({
            style {
                padding(14.px); borderRadius(14.px); backgroundColor(Color("#f8fafc"))
                border(1.px, LineStyle.Solid, Color("#e2e8f0")); marginBottom(12.px)
            }
        }) {
            P({
                style { margin(0.px); marginBottom(8.px); fontSize(14.px); fontWeight("700"); color(Color("#0f172a")) }
            }) { Text("How to Use") }

            Ul({
                style { margin(0.px); paddingLeft(18.px); color(Color("#374151")); fontSize(14.px); lineHeight("1.65") }
            }) {
                Li { Text("Enter a food name in the search bar.") }
                Li { Text("Use tabs to filter opinions by Safe, Caution, etc.") }
                Li { Text("Click the link icon (🔗) to view the original source.") }
                Li { Text("Reliability levels are indicators; final decisions depend on individual health status.")}
            }
        }

        // Footer Disclaimer & Links
        Div({
            style {
                padding(12.px); borderRadius(12.px); backgroundColor(Color("#fff7ed")); border(1.px, LineStyle.Solid, Color("#fed7aa"))
            }
        }) {
            P({
                style { margin(0.px); fontSize(13.px); color(Color("#7c2d12")); lineHeight("1.6") }
            }) {
                Text("※ All information provided is for general reference and is not medical advice. ")
                Text("Users are responsible for their own decisions. Consult a doctor if necessary.")
            }

            Div({
                style { marginTop(10.px); display(DisplayStyle.Flex); gap(10.px); flexWrap(FlexWrap.Wrap) }
            }) {
                A("/privacy.html", attrs = { style { fontSize(13.px); color(Color("#9a3412")); textDecoration("underline") } }) { Text("Privacy Policy") }
                A("/terms.html", attrs = { style { fontSize(13.px); color(Color("#9a3412")); textDecoration("underline") } }) { Text("Terms of Service") }
                A("/disclaimer.html", attrs = { style { fontSize(13.px); color(Color("#9a3412")); textDecoration("underline") } }) { Text("Disclaimer") }
            }
        }
    }
}

