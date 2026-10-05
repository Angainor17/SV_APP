/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.text.view.style

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextCSSStyleEntry
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry.Feature
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry.FontModifier
import org.geometerplus.zlibrary.text.view.ZLTextStyle
import java.util.ArrayList

class ZLTextExplicitlyDecoratedStyle(
    parent: ZLTextStyle,
    private val myEntry: ZLTextStyleEntry,
) : ZLTextDecoratedStyle(parent, parent.Hyperlink), Feature, FontModifier {

    private var myTreeParent: ZLTextStyle? = null

    override fun getFontEntriesInternal(): List<FontEntry> {
        val parentEntries = Parent.getFontEntries()
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSFontFamilyOption.getValue()) {
            return parentEntries
        }

        if (!myEntry.isFeatureSupported(Feature.FONT_FAMILY)) {
            return parentEntries
        }

        val entries = myEntry.getFontEntries()!!
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
        if (myEntry.Depth.toInt() == 0) {
            return Parent.Parent
        }
        var count = 0
        var p = Parent
        while (p !== p.Parent) {
            if (p is ZLTextExplicitlyDecoratedStyle) {
                if (p.myEntry.Depth != myEntry.Depth) {
                    return p
                }
            } else {
                if (++count > 1) {
                    return p
                }
            }
            p = p.Parent
        }
        return p
    }

    private fun getTreeParent(): ZLTextStyle {
        val treeParent = myTreeParent
        if (treeParent != null) {
            return treeParent
        }
        val computed = computeTreeParent()
        myTreeParent = computed
        return computed
    }

    override fun getFontSizeInternal(metrics: ZLTextMetrics): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSFontSizeOption.getValue()) {
            return Parent.getFontSize(metrics)
        }

        val baseFontSize = getTreeParent().getFontSize(metrics)
        if (myEntry.isFeatureSupported(Feature.FONT_STYLE_MODIFIER)) {
            if (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_INHERIT) == Boolean3.TRUE) {
                return baseFontSize
            }
            if (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_LARGER) == Boolean3.TRUE) {
                return baseFontSize * 120 / 100
            }
            if (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_SMALLER) == Boolean3.TRUE) {
                return baseFontSize * 100 / 120
            }
        }
        if (myEntry.isFeatureSupported(Feature.LENGTH_FONT_SIZE)) {
            return myEntry.getLength(Feature.LENGTH_FONT_SIZE, metrics, baseFontSize)
        }
        return Parent.getFontSize(metrics)
    }

    override fun isBoldInternal(): Boolean =
        when (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_BOLD)) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isBold()
        }

    override fun isItalicInternal(): Boolean =
        when (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_ITALIC)) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isItalic()
        }

    override fun isUnderlineInternal(): Boolean =
        when (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_UNDERLINED)) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isUnderline()
        }

    override fun isStrikeThroughInternal(): Boolean =
        when (myEntry.getFontModifier(FontModifier.FONT_MODIFIER_STRIKEDTHROUGH)) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isStrikeThrough()
        }

    override fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getLeftMargin(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_MARGIN_LEFT)) {
            return Parent.getLeftMargin(metrics)
        }
        return getTreeParent().getLeftMargin(metrics) + myEntry.getLength(Feature.LENGTH_MARGIN_LEFT, metrics, fontSize)
    }

    override fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getRightMargin(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_MARGIN_RIGHT)) {
            return Parent.getRightMargin(metrics)
        }
        return getTreeParent().getRightMargin(metrics) + myEntry.getLength(Feature.LENGTH_MARGIN_RIGHT, metrics, fontSize)
    }

    override fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getLeftPadding(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_PADDING_LEFT)) {
            return Parent.getLeftPadding(metrics)
        }
        return getTreeParent().getLeftPadding(metrics) + myEntry.getLength(Feature.LENGTH_PADDING_LEFT, metrics, fontSize)
    }

    override fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getRightPadding(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_PADDING_RIGHT)) {
            return Parent.getRightPadding(metrics)
        }
        return getTreeParent().getRightPadding(metrics) + myEntry.getLength(Feature.LENGTH_PADDING_RIGHT, metrics, fontSize)
    }

    override fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getFirstLineIndent(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_FIRST_LINE_INDENT)) {
            return Parent.getFirstLineIndent(metrics)
        }
        return myEntry.getLength(Feature.LENGTH_FIRST_LINE_INDENT, metrics, fontSize)
    }

    override fun getLineSpacePercentInternal(): Int =
        Parent.getLineSpacePercent()

    override fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry.isFeatureSupported(Feature.LENGTH_VERTICAL_ALIGN)) {
            return myEntry.getLength(Feature.LENGTH_VERTICAL_ALIGN, metrics, fontSize)
        } else if (myEntry.isFeatureSupported(Feature.NON_LENGTH_VERTICAL_ALIGN)) {
            return when (myEntry.getVerticalAlignCode().toInt()) {
                0 -> ZLTextStyleEntry.compute(
                    ZLTextStyleEntry.Length((-50).toShort(), ZLTextStyleEntry.SizeUnit.EM_100),
                    metrics, fontSize, Feature.LENGTH_VERTICAL_ALIGN,
                )
                1 -> ZLTextStyleEntry.compute(
                    ZLTextStyleEntry.Length(50.toShort(), ZLTextStyleEntry.SizeUnit.EM_100),
                    metrics, fontSize, Feature.LENGTH_VERTICAL_ALIGN,
                )
                else -> Parent.getVerticalAlign(metrics)
            }
        } else {
            return Parent.getVerticalAlign(metrics)
        }
    }

    override fun isVerticallyAlignedInternal(): Boolean {
        if (myEntry.isFeatureSupported(Feature.LENGTH_VERTICAL_ALIGN)) {
            return myEntry.hasNonZeroLength(Feature.LENGTH_VERTICAL_ALIGN)
        } else if (myEntry.isFeatureSupported(Feature.NON_LENGTH_VERTICAL_ALIGN)) {
            return when (myEntry.getVerticalAlignCode().toInt()) {
                0, 1 -> true
                else -> false
            }
        } else {
            return false
        }
    }

    override fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getSpaceBefore(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_SPACE_BEFORE)) {
            return Parent.getSpaceBefore(metrics)
        }
        return myEntry.getLength(Feature.LENGTH_SPACE_BEFORE, metrics, fontSize)
    }

    override fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int {
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSMarginsOption.getValue()) {
            return Parent.getSpaceAfter(metrics)
        }

        if (!myEntry.isFeatureSupported(Feature.LENGTH_SPACE_AFTER)) {
            return Parent.getSpaceAfter(metrics)
        }
        return myEntry.getLength(Feature.LENGTH_SPACE_AFTER, metrics, fontSize)
    }

    override fun getAlignment(): Byte =
        if (myEntry is ZLTextCSSStyleEntry && !BaseStyle.UseCSSTextAlignmentOption.getValue()) {
            Parent.getAlignment()
        } else {
            if (myEntry.isFeatureSupported(Feature.ALIGNMENT_TYPE)) myEntry.getAlignmentType() else Parent.getAlignment()
        }

    override fun allowHyphenations(): Boolean =
        Parent.allowHyphenations()
}
