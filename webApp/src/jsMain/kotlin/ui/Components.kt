package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import data.FoodSafetyEntry
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

// =====================
// Tabs (Vertical → Mobile Horizontal via AppStyles)
// =====================
@Composable
fun VerticalVerdictTabs(
    selectedTab: String,
    onSelect: (String) -> Unit
) {
    val tabs = listOf(
        "all" to "All",
        "safe" to "Safe",
        "conditional" to "Conditional",
        "caution" to "Caution",
        "avoid" to "Avoid"
    )

    Div({ classes(AppStyles.verdictTabs) }) {
        tabs.forEach { (key, label) ->
            val isSelected = selectedTab == key

            Button(attrs = {
                classes(AppStyles.verdictTabButton)
                onClick { onSelect(key) }
                style {
                    if (isSelected) {
                        backgroundColor(verdictColor(key))
                        color(Color.white)
                        border(1.px, LineStyle.Solid, verdictColor(key))
                        fontWeight("700")
                    } else {
                        backgroundColor(Color.white)
                        color(Color("#111827"))
                        border(1.px, LineStyle.Solid, Color("#e5e7eb"))
                    }
                }
            }) { Text(label) }
        }
    }
}

// =====================
// Source List
// =====================
@Composable
fun SourceListTab(entries: List<FoodSafetyEntry>) {
    // 1. State var: default false (collapsed)
    var showAll by remember { mutableStateOf(false) }

    // 2. Decide what to show (all if expanded, else max 5)
    val displayEntries = if (showAll) entries else entries.take(5)

    // Wrapper div so flex items can shrink properly
    Div({
        style {
            property("flex", "1 1 0")
            minWidth(0.px)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        Ul({
            style {
                listStyle("none")
                padding(0.px)
                margin(0.px)
            }
        }) {
            if (entries.isEmpty()) {
                Li({
                    style {
                        color(Color("#6b7280"))
                        fontSize(14.px)
                        padding(6.px, 0.px)
                    }
                }) { Text("No sources available for this category.") }
            } else {
                // Render only limited count
                displayEntries.forEach { entry -> SourceLine(entry) }
            }
        }

        // 3. Show button only when there are more than 5 items
        if (entries.size > 5) {
            Button({
                type(ButtonType.Button)
                onClick { showAll = !showAll }
                style {
                    width(100.percent)
                    padding(10.px)
                    marginTop(8.px)
                    backgroundColor(Color("#f3f4f6")) // light gray background
                    border(1.px, LineStyle.Solid, Color("#e5e7eb"))
                    borderRadius(8.px)
                    cursor("pointer")
                    fontSize(13.px)
                    fontWeight("600")
                    color(Color("#4b5563"))
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    alignItems(AlignItems.Center)
                    gap(4.px)
                }
            }) {
                if (showAll) {
                    Text("Collapse ▲")
                } else {
                    Text("Show all sources (+${entries.size - 5}) ▼")
                }
            }
        }
    }
}

@Composable
fun SourceLine(entry: FoodSafetyEntry) {
    val href = entry.url?.takeIf { it.isNotBlank() }

    Li({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(8.px)
            marginBottom(8.px)
            width(100.percent)
            property("min-width", "0")
        }
    }) {
        // [1] Badge (fixed)
        Span({
            style {
                flexShrink(0)
                backgroundColor(verdictBgRgba(entry.verdict, 0.15f))
                color(verdictColor(entry.verdict))
                padding(2.px, 6.px)
                borderRadius(6.px)
                fontSize(12.px)
                property("white-space", "nowrap")
            }
        }) { Text(verdictLabel(entry.verdict)) }

        // [2] Text + icon area
        if (href != null) {
            A(href = href, attrs = {
                attr("target", "_blank")
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    property("flex", "1 1 0px")
                    property("min-width", "0")
                    textDecoration("none")
                }
            }) {
                // Only the actual text should be ellipsized
                Span({
                    style {
                        color(Color("#2563eb"))
                        fontSize(14.px)
                        fontWeight("bold")
                        property("overflow", "hidden")
                        property("text-overflow", "ellipsis")
                        property("white-space", "nowrap")
                        flexGrow(1)
                    }
                }) { Text(entry.source) }

                // Icon should never shrink / be hidden
                Span({
                    style {
                        marginLeft(4.px)
                        fontSize(12.px)
                        flexShrink(0)
                    }
                }) { Text("🔗") }
            }
        } else {
            // No link (text only)
            Span({
                style {
                    flex(1, 1, 0.px)
                    fontSize(14.px)
                    fontWeight("bold")
                    property("overflow", "hidden")
                    property("text-overflow", "ellipsis")
                    property("white-space", "nowrap")
                }
            }) { Text(entry.source) }
        }

        // [3] Reliability label + bar (fixed to the right)
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                gap(6.px)
                flexShrink(0)
                marginLeft(4.px)
            }
        }) {
            Span({
                style {
                    fontSize(11.px)
                    color(Color("#6b7280"))
                    property("white-space", "nowrap") // prevent wrapping
                }
            }) { Text("Reliability") }

            ReliabilityBar(entry.reliability)
        }
    }
}

// =====================
// Tri-group recommendation (3 → 2 → 1 columns via AppStyles.triGroupGrid)
// =====================
private data class Signal(val icon: String, val label: String, val bg: String, val fg: String)

private fun decideSignal(
    safePct: Double,
    conditionalPct: Double,
    cautionPct: Double,
    avoidPct: Double
): Signal = when {
    avoidPct >= 40.0 ->
        Signal("✖️", "Better to avoid", "#fee2e2", "#b91c1c")
    cautionPct >= 40.0 ->
        Signal("⚠️", "Use caution", "#fef3c7", "#92400e")
    conditionalPct >= 40.0 ->
        Signal("🟡", "Likely okay with care", "#e0f2fe", "#075985")
    else ->
        Signal("💚", "Generally okay", "#dcfce7", "#166534")
}

private fun isHigh(e: FoodSafetyEntry) = e.reliability >= 3.0
private fun isMid(e: FoodSafetyEntry) = e.reliability >= 2.0 && e.reliability < 3.0
private fun isLow(e: FoodSafetyEntry) = e.reliability >= 1.0 && e.reliability < 2.0

@Composable
private fun GroupRecommendationCell(
    reliability: String,
    title: String,
    entries: List<FoodSafetyEntry>
) {
    val total = entries.size

    // Branch without return (avoid return errors)
    if (total == 0) {
        Div({
            classes(AppStyles.triGroupItem)
            style { backgroundColor(Color("#f9fafb")) }
        }) {
            Span({ classes(AppStyles.triBadge); style { color(Color("#6b7280")) } }) { Text(reliability) }
            Span({ classes(AppStyles.triGroupTitle) }) { Text(title) }

            Div({ classes(AppStyles.triGroupIcon) }) { Text("？") }
            Span({ classes(AppStyles.triGroupMainLine); style { color(Color("#9ca3af")) } }) { Text("No data") }
        }
    } else {
        val safe = entries.count { it.verdict.equals("safe", true) }
        val conditional = entries.count { it.verdict.equals("conditional", true) }
        val caution = entries.count { it.verdict.equals("caution", true) }
        val avoid = entries.count { it.verdict.equals("avoid", true) }

        fun pct(n: Int) = n * 100.0 / total
        val signal = decideSignal(
            safePct = pct(safe),
            conditionalPct = pct(conditional),
            cautionPct = pct(caution),
            avoidPct = pct(avoid)
        )

        Div({
            classes(AppStyles.triGroupItem)
            style { backgroundColor(Color(signal.bg)) }
        }) {
            // Reliability label
            Span({
                classes(AppStyles.triBadge)
                style {
                    color(
                        when (reliability) {
                            "High reliability" -> Color("#166534")
                            "Medium reliability" -> Color("#92400e")
                            else -> Color("#991b1b")
                        }
                    )
                }
            }) { Text(reliability) }

            // Title
            Span({ classes(AppStyles.triGroupTitle) }) { Text(title) }

            // Icon
            Div({
                classes(AppStyles.triGroupIcon)
                style { color(Color(signal.fg)) }
                title(signal.label)
            }) { Text(signal.icon) }

            // Main line
            Span({
                classes(AppStyles.triGroupMainLine)
                style { color(Color(signal.fg)) }
            }) { Text(signal.label) }

            // Detail line (hidden on mobile via Styles.kt)
            Span({ classes(AppStyles.triGroupDetailLine) }) {
                Text("Safe $safe · Conditional $conditional · Caution $caution · Avoid $avoid")
            }
        }
    }
}

@Composable
fun TriGroupRecommendationRow(entries: List<FoodSafetyEntry>) {
    Div({ classes(AppStyles.triGroupGrid) }) {
        val high = entries.filter(::isHigh)
        val mid = entries.filter(::isMid)
        val low = entries.filter(::isLow)

        GroupRecommendationCell("High reliability", "Clinicians / Institutions", high)
        GroupRecommendationCell("Medium reliability", "Companies / Press releases", mid)
        GroupRecommendationCell("Low reliability", "Individuals / Communities", low)
    }
}

// =====================
// Reliability-aware summary
// =====================
private enum class Tier { HIGH, MID, LOW }

private data class VerdictCounts(
    val safe: Int,
    val conditional: Int,
    val caution: Int,
    val avoid: Int
) {
    val total get() = safe + conditional + caution + avoid
    fun pct(n: Int) = if (total == 0) 0.0 else n * 100.0 / total
}

private fun tierOf(rel: Double): Tier = when {
    rel >= 3.0 -> Tier.HIGH
    rel >= 2.0 -> Tier.MID
    else -> Tier.LOW
}

private enum class Dominance { SAFE, CONDITIONAL, CAUTION, BAN, MIXED }

private fun dominanceOf(c: VerdictCounts): Dominance {
    if (c.total == 0) return Dominance.MIXED

    val safePct = c.pct(c.safe)
    val condPct = c.pct(c.conditional)
    val cautionPct = c.pct(c.caution)
    val avoidPct = c.pct(c.avoid)

    return when {
        avoidPct >= 50.0 -> Dominance.BAN
        cautionPct >= 40.0 || (avoidPct >= 30.0 && (cautionPct + avoidPct) >= 50.0) -> Dominance.CAUTION
        condPct >= 40.0 -> Dominance.CONDITIONAL
        safePct >= 60.0 -> Dominance.SAFE
        else -> Dominance.MIXED
    }
}

fun buildReliabilityAwareSummary(entries: List<FoodSafetyEntry>): String {
    if (entries.isEmpty()) return ""

    val countsByTier: Map<Tier, VerdictCounts> = Tier.values().associateWith { tier ->
        val list = entries.filter { tierOf(it.reliability) == tier }
        VerdictCounts(
            safe = list.count { it.verdict.equals("safe", true) },
            conditional = list.count { it.verdict.equals("conditional", true) },
            caution = list.count { it.verdict.equals("caution", true) },
            avoid = list.count { it.verdict.equals("avoid", true) }
        )
    }

    val primary: Tier? = listOf(Tier.HIGH, Tier.MID, Tier.LOW).firstOrNull { countsByTier[it]!!.total > 0 }
    val pCounts = primary?.let { countsByTier[it] } ?: VerdictCounts(0, 0, 0, 0)

    fun tierLabel(t: Tier) = when (t) {
        Tier.HIGH -> "According to high-reliability sources,"
        Tier.MID -> "According to mid-reliability sources,"
        Tier.LOW -> "According to lower-reliability sources,"
    }

    val dom = dominanceOf(pCounts)

    val head = when (dom) {
        Dominance.SAFE -> "${tierLabel(primary!!)} most opinions say it's generally okay."
        Dominance.CONDITIONAL -> "${tierLabel(primary!!)} many say it's okay if you're careful."
        Dominance.CAUTION -> "${tierLabel(primary!!)} caution is the dominant opinion."
        Dominance.BAN -> "${tierLabel(primary!!)} avoiding it is the dominant opinion."
        Dominance.MIXED -> "${tierLabel(primary!!)} opinions are mixed."
    }

    val others = Tier.values().filter { it != primary }
    val otherTotal = others.sumOf { countsByTier[it]!!.total }.coerceAtLeast(1)
    val otherSafe = others.sumOf { countsByTier[it]!!.safe }
    val otherCond = others.sumOf { countsByTier[it]!!.conditional }
    val otherCau = others.sumOf { countsByTier[it]!!.caution }
    val otherAvoid = others.sumOf { countsByTier[it]!!.avoid }

    fun notable(n: Int) = n >= 2 || (n * 100.0 / otherTotal) >= 30.0

    val tail = when (dom) {
        Dominance.BAN -> when {
            notable(otherSafe) || notable(otherCond) -> " Still, some other sources say it's conditional/okay."
            else -> ""
        }
        Dominance.CAUTION -> when {
            notable(otherSafe) -> " Still, some sources say it's okay."
            notable(otherCond) -> " Still, some sources say it's okay with conditions."
            else -> ""
        }
        Dominance.CONDITIONAL -> when {
            notable(otherCau) || notable(otherAvoid) -> " Still, some sources warn caution is needed."
            notable(otherSafe) -> " Also, some sources say it's okay."
            else -> ""
        }
        Dominance.SAFE -> when {
            notable(otherAvoid) || notable(otherCau) -> " Still, some sources recommend caution/avoidance."
            notable(otherCond) -> " Still, some sources consider it conditional."
            else -> ""
        }
        Dominance.MIXED -> ""
    }
    return head + tail
}

// =====================
// Reliability bar + Disclaimer
// =====================
@Composable
fun ReliabilityBar(rel: Double) {
    val level = when {
        rel >= 3.0 -> 3
        rel >= 2.0 -> 2
        else -> 1
    }
    val fillWidth = (level * 10)

    val fillColor = when (level) {
        3 -> "#16a34a"
        2 -> "#d97706"
        else -> "#dc2626"
    }

    Div({
        style {
            width(30.px)
            height(8.px)
            borderRadius(999.px)
            backgroundColor(Color("#e5e7eb"))
            position(Position.Relative)
            property("flex-shrink", "0")
        }
        title("Reliability ${fmt1(rel)}")
    }) {
        Div({
            style {
                width(fillWidth.px)
                height(8.px)
                borderRadius(999.px)
                backgroundColor(Color(fillColor))
                position(Position.Absolute)
                left(0.px)
                top(0.px)
            }
        })
    }
}
