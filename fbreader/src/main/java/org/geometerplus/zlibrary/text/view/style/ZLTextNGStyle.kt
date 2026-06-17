package org.geometerplus.zlibrary.text.view.style

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle

class ZLTextNGStyle(
    parent: ZLTextStyle,
    private val description: ZLTextNGStyleDescription,
    hyperlink: ZLTextHyperlink?
) : ZLTextDecoratedStyle(parent, hyperlink) {

    override fun getFontEntriesInternal(): List<FontEntry> {
        val parentEntries = parent?.getFontEntries() ?: emptyList()
        val decoratedValue = description.fontFamilyOption.value
        if (decoratedValue.isEmpty()) {
            return parentEntries
        }
        val e = FontEntry.systemEntry(decoratedValue)
        if (parentEntries.isNotEmpty() && e == parentEntries[0]) {
            return parentEntries
        }
        val entries = ArrayList<FontEntry>(parentEntries.size + 1)
        entries.add(e)
        entries.addAll(parentEntries)
        return entries
    }

    override fun getFontSizeInternal(metrics: ZLTextMetrics): Int =
        description.getFontSize(metrics, parent?.getFontSize(metrics) ?: 0)

    override fun isBoldInternal(): Boolean = when (description.isBold()) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isBold() ?: false
    }

    override fun isItalicInternal(): Boolean = when (description.isItalic()) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isItalic() ?: false
    }

    override fun isUnderlineInternal(): Boolean = when (description.isUnderlined()) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isUnderline() ?: false
    }

    override fun isStrikeThroughInternal(): Boolean = when (description.isStrikedThrough()) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isStrikeThrough() ?: false
    }

    override fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getLeftMargin(metrics, parent?.getLeftMargin(metrics) ?: 0, fontSize)

    override fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getRightMargin(metrics, parent?.getRightMargin(metrics) ?: 0, fontSize)

    override fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getLeftPadding(metrics, parent?.getLeftPadding(metrics) ?: 0, fontSize)

    override fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getRightPadding(metrics, parent?.getRightPadding(metrics) ?: 0, fontSize)

    override fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getFirstLineIndent(metrics, parent?.getFirstLineIndent(metrics) ?: 0, fontSize)

    override fun getLineSpacePercentInternal(): Int {
        val lineHeight = description.lineHeightOption.value
        if (!lineHeight.matches(Regex("[1-9][0-9]*%"))) {
            return parent?.getLineSpacePercent() ?: 100
        }
        return lineHeight.substring(0, lineHeight.length - 1).toInt()
    }

    override fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getVerticalAlign(metrics, parent?.getVerticalAlign(metrics) ?: 0, fontSize)

    override fun isVerticallyAlignedInternal(): Boolean = description.hasNonZeroVerticalAlign()

    override fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getSpaceBefore(metrics, parent?.getSpaceBefore(metrics) ?: 0, fontSize)

    override fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        description.getSpaceAfter(metrics, parent?.getSpaceAfter(metrics) ?: 0, fontSize)

    override fun getAlignment(): Byte {
        val defined = description.alignment
        return if (defined != ZLTextAlignmentType.ALIGN_UNDEFINED) defined else parent?.getAlignment() ?: 0
    }

    override fun allowHyphenations(): Boolean = when (description.allowHyphenations()) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.allowHyphenations() ?: true
    }

    override fun toString(): String = "ZLTextNGStyle[${description.name}]"
}
