package org.geometerplus.zlibrary.text.view.style

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry

class ZLTextNGStyleDescription(selector: String, valueMap: Map<String, String>) {

    companion object {
        private val cache = HashMap<String, Any>()
        private val nullObject = Any()

        private fun parseLength(value: String?): ZLTextStyleEntry.Length? {
            if (value.isNullOrEmpty()) return null

            val cached = cache[value]
            if (cached != null) {
                return if (cached === nullObject) null else cached as ZLTextStyleEntry.Length
            }

            var length: ZLTextStyleEntry.Length? = null
            try {
                when {
                    value.endsWith("%") -> length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 1).toShort(),
                        ZLTextStyleEntry.SizeUnit.PERCENT
                    )
                    value.endsWith("rem") -> length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.REM_100
                    )
                    value.endsWith("em") -> length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.EM_100
                    )
                    value.endsWith("ex") -> length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.EX_100
                    )
                    value.endsWith("px") -> length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 2).toShort(),
                        ZLTextStyleEntry.SizeUnit.PIXEL
                    )
                    value.endsWith("pt") -> length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 2).toShort(),
                        ZLTextStyleEntry.SizeUnit.POINT
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
            cache[value] = length ?: nullObject
            return length
        }

        private fun createOption(selector: String, name: String, valueMap: Map<String, String>): ZLStringOption =
            ZLStringOption("Style", "$selector::$name", valueMap[name])
    }

    @JvmField val Name: String? = valueMap["fbreader-name"]
    val name: String? get() = Name

    @JvmField val FontFamilyOption = createOption(selector, "font-family", valueMap)
    @JvmField val FontSizeOption = createOption(selector, "font-size", valueMap)
    @JvmField val FontWeightOption = createOption(selector, "font-weight", valueMap)
    @JvmField val FontStyleOption = createOption(selector, "font-style", valueMap)
    @JvmField val TextDecorationOption = createOption(selector, "text-decoration", valueMap)
    @JvmField val HyphenationOption = createOption(selector, "hyphens", valueMap)
    @JvmField val MarginTopOption = createOption(selector, "margin-top", valueMap)
    @JvmField val MarginBottomOption = createOption(selector, "margin-bottom", valueMap)
    @JvmField val MarginLeftOption = createOption(selector, "margin-left", valueMap)
    @JvmField val MarginRightOption = createOption(selector, "margin-right", valueMap)
    @JvmField val TextIndentOption = createOption(selector, "text-indent", valueMap)
    @JvmField val AlignmentOption = createOption(selector, "text-align", valueMap)
    @JvmField val VerticalAlignOption = createOption(selector, "vertical-align", valueMap)
    @JvmField val LineHeightOption = createOption(selector, "line-height", valueMap)

    val fontFamilyOption get() = FontFamilyOption
    val fontSizeOption get() = FontSizeOption
    val fontWeightOption get() = FontWeightOption
    val fontStyleOption get() = FontStyleOption
    val textDecorationOption get() = TextDecorationOption
    val hyphenationOption get() = HyphenationOption
    val marginTopOption get() = MarginTopOption
    val marginBottomOption get() = MarginBottomOption
    val marginLeftOption get() = MarginLeftOption
    val marginRightOption get() = MarginRightOption
    val textIndentOption get() = TextIndentOption
    val alignmentOption get() = AlignmentOption
    val verticalAlignOption get() = VerticalAlignOption
    val lineHeightOption get() = LineHeightOption

    fun getFontSize(metrics: ZLTextMetrics, parentFontSize: Int): Int {
        val length = parseLength(fontSizeOption.value) ?: return parentFontSize
        return ZLTextStyleEntry.compute(length, metrics, parentFontSize, ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE)
    }

    fun getVerticalAlign(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(verticalAlignOption.value) ?: return base
        return ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE)
    }

    fun hasNonZeroVerticalAlign(): Boolean {
        val length = parseLength(verticalAlignOption.value) ?: return false
        return length.Size != 0.toShort()
    }

    fun getLeftMargin(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(marginLeftOption.value) ?: return base
        return base + ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_MARGIN_LEFT)
    }

    fun getRightMargin(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(marginRightOption.value) ?: return base
        return base + ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_MARGIN_RIGHT)
    }

    fun getLeftPadding(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int = base

    fun getRightPadding(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int = base

    fun getFirstLineIndent(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(textIndentOption.value) ?: return base
        return ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_FIRST_LINE_INDENT)
    }

    fun getSpaceBefore(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(marginTopOption.value) ?: return base
        return ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_SPACE_BEFORE)
    }

    fun getSpaceAfter(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(marginBottomOption.value) ?: return base
        return ZLTextStyleEntry.compute(length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_SPACE_AFTER)
    }

    fun isBold(): Boolean3 = when (fontWeightOption.value) {
        "bold" -> Boolean3.TRUE
        "normal" -> Boolean3.FALSE
        else -> Boolean3.UNDEFINED
    }

    fun isItalic(): Boolean3 = when (fontStyleOption.value) {
        "italic", "oblique" -> Boolean3.TRUE
        "normal" -> Boolean3.FALSE
        else -> Boolean3.UNDEFINED
    }

    fun isUnderlined(): Boolean3 = when (textDecorationOption.value) {
        "underline" -> Boolean3.TRUE
        "", "inherit" -> Boolean3.UNDEFINED
        else -> Boolean3.FALSE
    }

    fun isStrikedThrough(): Boolean3 = when (textDecorationOption.value) {
        "line-through" -> Boolean3.TRUE
        "", "inherit" -> Boolean3.UNDEFINED
        else -> Boolean3.FALSE
    }

    val alignment: Byte
        get() = when (alignmentOption.value) {
            "center" -> ZLTextAlignmentType.ALIGN_CENTER
            "left" -> ZLTextAlignmentType.ALIGN_LEFT
            "right" -> ZLTextAlignmentType.ALIGN_RIGHT
            "justify" -> ZLTextAlignmentType.ALIGN_JUSTIFY
            else -> ZLTextAlignmentType.ALIGN_UNDEFINED
        }

    fun allowHyphenations(): Boolean3 = when (hyphenationOption.value) {
        "auto" -> Boolean3.TRUE
        "none" -> Boolean3.FALSE
        else -> Boolean3.UNDEFINED
    }
}
