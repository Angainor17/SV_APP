package org.geometerplus.zlibrary.text.view.style

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextCSSStyleEntry
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry
import org.geometerplus.zlibrary.text.view.ZLTextStyle

class ZLTextExplicitlyDecoratedStyle(parent: ZLTextStyle, private val entry: ZLTextStyleEntry) :
    ZLTextDecoratedStyle(parent, parent.hyperlink) {

    private var treeParent: ZLTextStyle? = null

    override fun getFontEntriesInternal(): List<FontEntry> {
        val parentEntries = parent?.getFontEntries() ?: emptyList()
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSFontFamilyOption.value) {
            return parentEntries
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.FONT_FAMILY)) {
            return parentEntries
        }

        val entries = entry.fontEntries
        val lSize = entries.size
        if (lSize == 0) {
            return parentEntries
        }

        val pSize = parentEntries.size
        if (pSize > lSize && entries == parentEntries.subList(0, lSize)) {
            return parentEntries
        }

        val allEntries = ArrayList<FontEntry>(pSize + lSize)
        allEntries.addAll(entries)
        allEntries.addAll(parentEntries)
        return allEntries
    }

    private fun computeTreeParent(): ZLTextStyle {
        if (entry.Depth == 0.toShort()) {
            return parent?.parent ?: parent!!
        }
        var count = 0
        var p: ZLTextStyle? = parent
        while (p != p?.parent) {
            if (p is ZLTextExplicitlyDecoratedStyle) {
                if (p.entry.Depth != entry.Depth) {
                    return p
                }
            } else {
                if (++count > 1) {
                    return p!!
                }
            }
            p = p?.parent
        }
        return p!!
    }

    private fun getTreeParent(): ZLTextStyle {
        if (treeParent == null) {
            treeParent = computeTreeParent()
        }
        return treeParent!!
    }

    override fun getFontSizeInternal(metrics: ZLTextMetrics): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSFontSizeOption.value) {
            return parent?.getFontSize(metrics) ?: 0
        }

        val baseFontSize = getTreeParent().getFontSize(metrics)
        if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.FONT_STYLE_MODIFIER)) {
            if (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_INHERIT) == Boolean3.TRUE) {
                return baseFontSize
            }
            if (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_LARGER) == Boolean3.TRUE) {
                return baseFontSize * 120 / 100
            }
            if (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_SMALLER) == Boolean3.TRUE) {
                return baseFontSize * 100 / 120
            }
        }
        if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE)) {
            return entry.getLength(ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE, metrics, baseFontSize)
        }
        return parent?.getFontSize(metrics) ?: 0
    }

    override fun isBoldInternal(): Boolean = when (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_BOLD)) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isBold() ?: false
    }

    override fun isItalicInternal(): Boolean = when (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_ITALIC)) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isItalic() ?: false
    }

    override fun isUnderlineInternal(): Boolean = when (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_UNDERLINED)) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isUnderline() ?: false
    }

    override fun isStrikeThroughInternal(): Boolean = when (entry.getFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_STRIKEDTHROUGH)) {
        Boolean3.TRUE -> true
        Boolean3.FALSE -> false
        else -> parent?.isStrikeThrough() ?: false
    }

    override fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getLeftMargin(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_MARGIN_LEFT)) {
            return parent?.getLeftMargin(metrics) ?: 0
        }
        return getTreeParent().getLeftMargin(metrics) + entry.getLength(ZLTextStyleEntry.Feature.LENGTH_MARGIN_LEFT, metrics, fontSize)
    }

    override fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getRightMargin(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_MARGIN_RIGHT)) {
            return parent?.getRightMargin(metrics) ?: 0
        }
        return getTreeParent().getRightMargin(metrics) + entry.getLength(ZLTextStyleEntry.Feature.LENGTH_MARGIN_RIGHT, metrics, fontSize)
    }

    override fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getLeftPadding(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_PADDING_LEFT)) {
            return parent?.getLeftPadding(metrics) ?: 0
        }
        return getTreeParent().getLeftPadding(metrics) + entry.getLength(ZLTextStyleEntry.Feature.LENGTH_PADDING_LEFT, metrics, fontSize)
    }

    override fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getRightPadding(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_PADDING_RIGHT)) {
            return parent?.getRightPadding(metrics) ?: 0
        }
        return getTreeParent().getRightPadding(metrics) + entry.getLength(ZLTextStyleEntry.Feature.LENGTH_PADDING_RIGHT, metrics, fontSize)
    }

    override fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getFirstLineIndent(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_FIRST_LINE_INDENT)) {
            return parent?.getFirstLineIndent(metrics) ?: 0
        }
        return entry.getLength(ZLTextStyleEntry.Feature.LENGTH_FIRST_LINE_INDENT, metrics, fontSize)
    }

    override fun getLineSpacePercentInternal(): Int = parent?.getLineSpacePercent() ?: 100

    override fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN)) {
            return entry.getLength(ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN, metrics, fontSize)
        } else if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.NON_LENGTH_VERTICAL_ALIGN)) {
            return when (entry.verticalAlignCode) {
                0.toByte() -> ZLTextStyleEntry.compute(
                    ZLTextStyleEntry.Length((-50).toShort(), ZLTextStyleEntry.SizeUnit.EM_100),
                    metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN
                )
                1.toByte() -> ZLTextStyleEntry.compute(
                    ZLTextStyleEntry.Length(50.toShort(), ZLTextStyleEntry.SizeUnit.EM_100),
                    metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN
                )
                else -> parent?.getVerticalAlign(metrics) ?: 0
            }
        } else {
            return parent?.getVerticalAlign(metrics) ?: 0
        }
    }

    override fun isVerticallyAlignedInternal(): Boolean {
        if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN)) {
            return entry.hasNonZeroLength(ZLTextStyleEntry.Feature.LENGTH_VERTICAL_ALIGN)
        } else if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.NON_LENGTH_VERTICAL_ALIGN)) {
            return when (entry.verticalAlignCode) {
                0.toByte(), 1.toByte() -> true
                else -> false
            }
        } else {
            return false
        }
    }

    override fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getSpaceBefore(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_SPACE_BEFORE)) {
            return parent?.getSpaceBefore(metrics) ?: 0
        }
        return entry.getLength(ZLTextStyleEntry.Feature.LENGTH_SPACE_BEFORE, metrics, fontSize)
    }

    override fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSMarginsOption.value) {
            return parent?.getSpaceAfter(metrics) ?: 0
        }

        if (!entry.isFeatureSupported(ZLTextStyleEntry.Feature.LENGTH_SPACE_AFTER)) {
            return parent?.getSpaceAfter(metrics) ?: 0
        }
        return entry.getLength(ZLTextStyleEntry.Feature.LENGTH_SPACE_AFTER, metrics, fontSize)
    }

    override fun getAlignment(): Byte {
        if (entry is ZLTextCSSStyleEntry && !baseStyle.useCSSTextAlignmentOption.value) {
            return parent?.getAlignment() ?: 0
        }
        return if (entry.isFeatureSupported(ZLTextStyleEntry.Feature.ALIGNMENT_TYPE)) entry.alignmentType else parent?.getAlignment() ?: 0
    }

    override fun allowHyphenations(): Boolean = parent?.allowHyphenations() ?: true
}
