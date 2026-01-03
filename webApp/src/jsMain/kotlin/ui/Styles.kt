package ui

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto

object AppStyles : StyleSheet() {

    init {
        // border-box on all elements
        "*" style {
            property("box-sizing", "border-box")
        }
    }

    // ==============
    // Page / Layout
    // ==============
    val container by style {
        padding(24.px)
        maxWidth(760.px)
        property("margin", "0 auto")
        property("width", "100%")
        fontFamily("Pretendard", "system-ui", "sans-serif")
    }

    val resultsPanel by style {
        marginTop(16.px)
        borderRadius(14.px)
        border(1.px, LineStyle.Solid, Color("#e5e7eb"))
        backgroundColor(Color.white)
        padding(18.px)
        property("box-shadow", "0 6px 18px rgba(0,0,0,.04)")
    }

    val resultCard by style {
        border(1.px, LineStyle.Solid, Color.lightgray)
        borderRadius(8.px)
        padding(16.px)
        marginBottom(16.px)
        backgroundColor(Color.white)
        property("box-shadow", "0px 4px 12px rgba(0, 0, 0, 0.05)")
    }

    // ====================
    // Header (logo + title)
    // ====================
    val headerCard by style {
        backgroundColor(Color.white)
        border(1.px, LineStyle.Solid, Color("#e5e7eb"))
        borderRadius(16.px)
        padding(22.px)
        property("box-shadow", "0 8px 24px rgba(0,0,0,.06)")
    }

    val headerRow by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(12.px)
        marginBottom(14.px)
        property("min-width", "0")
    }

    val headerTexts by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        property("min-width", "0")
    }

    val headerTitle by style {
        margin(0.px)
        fontSize(26.px)
        color(Color("#0f172a"))
        property("white-space", "nowrap")
        property("overflow", "hidden")
        property("text-overflow", "ellipsis")
    }

    val headerSubtitle by style {
        marginTop(4.px)
        color(Color("#64748b"))
        fontSize(14.px)
        property("white-space", "normal")
        property("overflow", "visible")
    }

    val headerLogo by style {
        width(80.px)
        height(80.px)
        property("object-fit", "contain")
        property("flex-shrink", "0")
    }

    val disclaimerText by style {
        marginTop(10.px)
        marginBottom(0.px)
        color(Color("#64748b"))
        fontSize(15.px)
        lineHeight("1.5")
    }

    // ======================
    // TriGroup
    // ======================
    val triGroupGrid by style {
        display(DisplayStyle.Grid)
        property("grid-template-columns", "repeat(3, minmax(0, 1fr))") // single line
        gap(10.px)
        marginTop(10.px)
        width(100.percent)
    }

    val triGroupItem by style {
        position(Position.Relative)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        property("justify-content", "center")
        padding(14.px, 10.px)
        borderRadius(12.px)
        property("border", "1px solid rgba(0,0,0,.06)")
        property("min-width", "0")
        textAlign("center")
        minHeight(120.px)
    }

    val triBadge by style {
        position(Position.Absolute)
        top(8.px)
        left(8.px)
        padding(3.px, 7.px)
        borderRadius(999.px)
        fontSize(11.px)
        property("font-weight", "700")
        backgroundColor(Color.white)
        property("border", "1px solid rgba(0,0,0,.08)")
        property("white-space", "nowrap")
    }

    val triGroupTitle by style {
        fontSize(12.px)
        fontWeight("600")
        marginTop(18.px) // ✅ 배지 공간 확보
        property("white-space", "normal")
        property("overflow", "visible")
        maxWidth(100.percent)
    }

    val triGroupIcon by style {
        width(34.px)
        height(34.px)
        borderRadius(999.px)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        fontSize(18.px)
        backgroundColor(Color.white)
        property("box-shadow", "0 1px 2px rgba(0,0,0,.06)")
        property("border", "1px solid rgba(0,0,0,.05)")
        marginTop(8.px)
        marginBottom(6.px)
        property("flex-shrink", "0")
    }

    val triGroupMainLine by style {
        fontSize(12.px)
        fontWeight("600")
        property("white-space", "normal")
        property("overflow", "visible")
        maxWidth(100.percent)
    }

    val triGroupDetailLine by style {
        fontSize(12.px)
        color(Color("#4b5563"))
        property("white-space", "nowrap")
        property("overflow", "hidden")
        property("text-overflow", "ellipsis")
        maxWidth(100.percent)
        marginTop(2.px)
    }

    // =========================
    // Tabs area (left tabs + list)
    // =========================
    val tabsArea by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.FlexStart)
        marginTop(10.px)
        paddingTop(15.px)
        property("border-top", "1px solid #e5e7eb")
        property("border-top", "1px solid #e5e7eb")
        property("width", "100%")
        property("min-width", "0")
    }

    // VerticalVerdictTabs
    val verdictTabs by style {
        width(120.px)
        paddingRight(10.px)
        marginRight(15.px)
        property("border-right", "1px solid #e5e7eb")
        property("flex-shrink", "0")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(8.px)
    }

    // Tab buttons
    val verdictTabButton by style {
        textAlign("left")
        padding(8.px, 10.px)
        borderRadius(10.px)
        border(1.px, LineStyle.Solid, Color("#e5e7eb"))
        backgroundColor(Color.white)
        cursor("pointer")
        fontSize(13.px)
        property("white-space", "nowrap")
    }

    // tab contents on the right side
    val tabContent by style {
        property("flex-grow", "1")
        property("min-width", "0")
    }


    // ======================
    // MOBILE PAGE
    // ======================
    init {
        media("(max-width: 600px)") {
            // narrow paddings
            ".$container" style { padding(5.px) }
            ".$resultsPanel" style { padding(10.px) }
            ".$resultCard" style { padding(10.px) }

            ".$headerRow" style { gap(10.px) }
            ".$headerTitle" style { fontSize(16.px) }
            ".$headerSubtitle" style { fontSize(12.px) }

            ".$headerLogo" style {
                width(40.px)
                height(40.px)
            }

            ".$disclaimerText" style {
                fontSize(12.px)
                lineHeight("1.4")
                marginTop(8.px)

            }

            ".$triGroupGrid" style { gap(6.px) }

            ".$triGroupItem" style {
                padding(10.px, 6.px)
                borderRadius(10.px)
                minHeight(108.px)
            }

            ".$triBadge" style {
                top(6.px); left(6.px)
                fontSize(10.px)
                padding(2.px, 6.px)
            }

            ".$triGroupTitle" style {
                fontSize(11.px)
                marginTop(16.px)
            }

            ".$triGroupIcon" style {
                width(30.px)
                height(30.px)
                fontSize(16.px)
                marginTop(6.px)
                marginBottom(4.px)
            }

            ".$triGroupMainLine" style { fontSize(11.px) }
            ".$triGroupDetailLine" style { display(DisplayStyle.None) }
        }

        // vertical to horizontal tab
        media("(max-width: 640px)") {
            ".$tabsArea" style {
                flexDirection(FlexDirection.Column)
                width(100.percent)
                property("min-width", "0")
                property("overflow", "hidden")
            }

            ".$verdictTabs" style {
                width(100.percent)
                paddingRight(0.px)
                marginRight(0.px)
                property("border-right", "none")
                property("border-bottom", "1px solid #e5e7eb")
                paddingBottom(8.px)
                marginBottom(12.px)

                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Row)
                property("overflow-x", "auto")
                property("white-space", "nowrap")
                gap(8.px)
            }

            ".$verdictTabButton" style{
                padding(8.px, 7.px)
                borderRadius(10.px)
                border(1.px, LineStyle.Solid, Color("#e5e7eb"))
                backgroundColor(Color.white)
                cursor("pointer")
                fontSize(13.px)
                property("white-space", "nowrap")
            }

            ".$tabContent" style {
                width(100.percent)
                property("min-width", "0")
            }
        }
    }
}
