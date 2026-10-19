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
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle
import java.util.ArrayList

class ZLTextNGStyle(
    parent: ZLTextStyle,
    private val myDescription: ZLTextNGStyleDescription,
    hyperlink: ZLTextHyperlink?,
) : ZLTextDecoratedStyle(parent, hyperlink) {

    override fun getFontEntriesInternal(): List<FontEntry> {
        val parentEntries = Parent.getFontEntries()
        val decoratedValue = myDescription.FontFamilyOption.getValue()
        if ("" == decoratedValue) {
            return parentEntries
        }
        val e = FontEntry.systemEntry(decoratedValue)
        if (parentEntries.size > 0 && e == parentEntries[0]) {
            return parentEntries
        }
        val entries = ArrayList<FontEntry>(parentEntries.size + 1)
        entries.add(e)
        entries.addAll(parentEntries)
        return entries
    }

    override fun getFontSizeInternal(metrics: ZLTextMetrics): Int =
        myDescription.getFontSize(metrics, Parent.getFontSize(metrics))

    override fun isBoldInternal(): Boolean =
        when (myDescription.isBold()) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isBold()
        }

    override fun isItalicInternal(): Boolean =
        when (myDescription.isItalic()) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isItalic()
        }

    override fun isUnderlineInternal(): Boolean =
        when (myDescription.isUnderlined()) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isUnderline()
        }

    override fun isStrikeThroughInternal(): Boolean =
        when (myDescription.isStrikedThrough()) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.isStrikeThrough()
        }

    override fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getLeftMargin(metrics, Parent.getLeftMargin(metrics), fontSize)

    override fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getRightMargin(metrics, Parent.getRightMargin(metrics), fontSize)

    override fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getLeftPadding(metrics, Parent.getLeftPadding(metrics), fontSize)

    override fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getRightPadding(metrics, Parent.getRightPadding(metrics), fontSize)

    override fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getFirstLineIndent(metrics, Parent.getFirstLineIndent(metrics), fontSize)

    override fun getLineSpacePercentInternal(): Int {
        val lineHeight = myDescription.LineHeightOption.getValue()
        if (!lineHeight.matches(Regex("[1-9][0-9]*%"))) {
            return Parent.getLineSpacePercent()
        }
        return lineHeight.substring(0, lineHeight.length - 1).toInt()
    }

    override fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getVerticalAlign(metrics, Parent.getVerticalAlign(metrics), fontSize)

    override fun isVerticallyAlignedInternal(): Boolean =
        myDescription.hasNonZeroVerticalAlign()

    override fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getSpaceBefore(metrics, Parent.getSpaceBefore(metrics), fontSize)

    override fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int =
        myDescription.getSpaceAfter(metrics, Parent.getSpaceAfter(metrics), fontSize)

    override fun getAlignment(): Byte {
        val defined = myDescription.getAlignment()
        if (defined != ZLTextAlignmentType.ALIGN_UNDEFINED) {
            return defined
        }
        return Parent.getAlignment()
    }

    override fun allowHyphenations(): Boolean =
        when (myDescription.allowHyphenations()) {
            Boolean3.TRUE -> true
            Boolean3.FALSE -> false
            else -> Parent.allowHyphenations()
        }

    override fun toString(): String = "ZLTextNGStyle[" + myDescription.Name + "]"
}
