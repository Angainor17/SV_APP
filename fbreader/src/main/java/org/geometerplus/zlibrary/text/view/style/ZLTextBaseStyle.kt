package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle

class ZLTextBaseStyle(prefix: String, fontFamily: String, fontSize: Int) : ZLTextStyle(null, ZLTextHyperlink.NO_LINK) {

    companion object {
        private const val GROUP = "Style"
        private const val OPTIONS = "Options"
    }

    @JvmField val UseCSSTextAlignmentOption = ZLBooleanOption("Style", "css:textAlignment", true)
    @JvmField val UseCSSMarginsOption = ZLBooleanOption("Style", "css:margins", true)
    @JvmField val UseCSSFontSizeOption = ZLBooleanOption("Style", "css:fontSize", true)
    @JvmField val UseCSSFontFamilyOption = ZLBooleanOption("Style", "css:fontFamily", true)

    val useCSSTextAlignmentOption: ZLBooleanOption get() = UseCSSTextAlignmentOption
    val useCSSMarginsOption: ZLBooleanOption get() = UseCSSMarginsOption
    val useCSSFontSizeOption: ZLBooleanOption get() = UseCSSFontSizeOption
    val useCSSFontFamilyOption: ZLBooleanOption get() = UseCSSFontFamilyOption

    @JvmField val AutoHyphenationOption = ZLBooleanOption(OPTIONS, "AutoHyphenation", true)
    val autoHyphenationOption: ZLBooleanOption get() = AutoHyphenationOption

    @JvmField val BoldOption: ZLBooleanOption
    @JvmField val ItalicOption: ZLBooleanOption
    @JvmField val UnderlineOption: ZLBooleanOption
    @JvmField val StrikeThroughOption: ZLBooleanOption
    @JvmField val AlignmentOption: ZLIntegerRangeOption
    @JvmField val LineSpaceOption: ZLIntegerRangeOption

    @JvmField val FontFamilyOption: ZLStringOption
    @JvmField val FontSizeOption: ZLIntegerRangeOption

    val boldOption: ZLBooleanOption get() = BoldOption
    val italicOption: ZLBooleanOption get() = ItalicOption
    val underlineOption: ZLBooleanOption get() = UnderlineOption
    val strikeThroughOption: ZLBooleanOption get() = StrikeThroughOption
    val alignmentOption: ZLIntegerRangeOption get() = AlignmentOption
    val lineSpaceOption: ZLIntegerRangeOption get() = LineSpaceOption
    val fontFamilyOption: ZLStringOption get() = FontFamilyOption
    val fontSizeOption: ZLIntegerRangeOption get() = FontSizeOption

    private var myFontFamily: String? = null
    private var myFontEntries: List<FontEntry>? = null

    init {
        FontFamilyOption = ZLStringOption(GROUP, "$prefix:fontFamily", fontFamily)
        val adjustedFontSize = fontSize * ZLibrary.Instance().displayDPI / 160
        FontSizeOption = ZLIntegerRangeOption(GROUP, "$prefix:fontSize", 5, maxOf(144, adjustedFontSize * 2), adjustedFontSize)
        BoldOption = ZLBooleanOption(GROUP, "$prefix:bold", false)
        ItalicOption = ZLBooleanOption(GROUP, "$prefix:italic", false)
        UnderlineOption = ZLBooleanOption(GROUP, "$prefix:underline", false)
        StrikeThroughOption = ZLBooleanOption(GROUP, "$prefix:strikeThrough", false)
        AlignmentOption = ZLIntegerRangeOption(GROUP, "$prefix:alignment", 1, 4, ZLTextAlignmentType.ALIGN_JUSTIFY.toInt())
        LineSpaceOption = ZLIntegerRangeOption(GROUP, "$prefix:lineSpacing", 5, 20, 12)
    }

    override fun getFontEntries(): List<FontEntry> {
        val family = FontFamilyOption.value
        if (myFontEntries == null || family != myFontFamily) {
            myFontEntries = listOf(FontEntry.systemEntry(family))
            myFontFamily = family
        }
        return myFontEntries!!
    }

    val fontSize: Int
        get() = FontSizeOption.value

    override fun getFontSize(metrics: ZLTextMetrics): Int = fontSize

    override fun isBold(): Boolean = BoldOption.value

    override fun isItalic(): Boolean = ItalicOption.value

    override fun isUnderline(): Boolean = UnderlineOption.value

    override fun isStrikeThrough(): Boolean = StrikeThroughOption.value

    override fun getLeftMargin(metrics: ZLTextMetrics): Int = 0

    override fun getRightMargin(metrics: ZLTextMetrics): Int = 0

    override fun getLeftPadding(metrics: ZLTextMetrics): Int = 0

    override fun getRightPadding(metrics: ZLTextMetrics): Int = 0

    override fun getFirstLineIndent(metrics: ZLTextMetrics): Int = 0

    override fun getLineSpacePercent(): Int = LineSpaceOption.value * 10

    override fun getVerticalAlign(metrics: ZLTextMetrics): Int = 0

    override fun isVerticallyAligned(): Boolean = false

    override fun getSpaceBefore(metrics: ZLTextMetrics): Int = 0

    override fun getSpaceAfter(metrics: ZLTextMetrics): Int = 0

    override fun getAlignment(): Byte = AlignmentOption.value.toByte()

    override fun allowHyphenations(): Boolean = true
}
