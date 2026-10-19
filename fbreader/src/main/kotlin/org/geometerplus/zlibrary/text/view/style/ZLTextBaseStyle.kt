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

import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle

class ZLTextBaseStyle(prefix: String, fontFamily: String, fontSize: Int) :
    ZLTextStyle(null, ZLTextHyperlink.NO_LINK) {

    @JvmField
    val UseCSSTextAlignmentOption =
        ZLBooleanOption("Style", "css:textAlignment", true)

    @JvmField
    val UseCSSMarginsOption =
        ZLBooleanOption("Style", "css:margins", true)

    @JvmField
    val UseCSSFontSizeOption =
        ZLBooleanOption("Style", "css:fontSize", true)

    @JvmField
    val UseCSSFontFamilyOption =
        ZLBooleanOption("Style", "css:fontFamily", true)

    @JvmField
    val AutoHyphenationOption =
        ZLBooleanOption(OPTIONS, "AutoHyphenation", true)

    @JvmField
    val BoldOption: ZLBooleanOption

    @JvmField
    val ItalicOption: ZLBooleanOption

    @JvmField
    val UnderlineOption: ZLBooleanOption

    @JvmField
    val StrikeThroughOption: ZLBooleanOption

    @JvmField
    val AlignmentOption: ZLIntegerRangeOption

    @JvmField
    val LineSpaceOption: ZLIntegerRangeOption

    @JvmField
    val FontFamilyOption: ZLStringOption

    @JvmField
    val FontSizeOption: ZLIntegerRangeOption

    private var myFontFamily: String? = null
    private var myFontEntries: List<FontEntry>? = null

    init {
        FontFamilyOption = ZLStringOption(GROUP, "$prefix:fontFamily", fontFamily)
        val scaledFontSize = fontSize * ZLibrary.Instance().getDisplayDPI() / 160
        FontSizeOption = ZLIntegerRangeOption(
            GROUP, "$prefix:fontSize", 5, maxOf(144, scaledFontSize * 2), scaledFontSize,
        )
        BoldOption = ZLBooleanOption(GROUP, "$prefix:bold", false)
        ItalicOption = ZLBooleanOption(GROUP, "$prefix:italic", false)
        UnderlineOption = ZLBooleanOption(GROUP, "$prefix:underline", false)
        StrikeThroughOption = ZLBooleanOption(GROUP, "$prefix:strikeThrough", false)
        AlignmentOption = ZLIntegerRangeOption(
            GROUP, "$prefix:alignment", 1, 4, ZLTextAlignmentType.ALIGN_JUSTIFY.toInt(),
        )
        LineSpaceOption = ZLIntegerRangeOption(GROUP, "$prefix:lineSpacing", 5, 20, 12)
    }

    override fun getFontEntries(): List<FontEntry> {
        val family = FontFamilyOption.getValue()
        if (myFontEntries == null || family != myFontFamily) {
            myFontEntries = listOf(FontEntry.systemEntry(family))
        }
        return myFontEntries!!
    }

    fun getFontSize(): Int = FontSizeOption.getValue()

    override fun getFontSize(metrics: ZLTextMetrics): Int = getFontSize()

    override fun isBold(): Boolean = BoldOption.getValue()

    override fun isItalic(): Boolean = ItalicOption.getValue()

    override fun isUnderline(): Boolean = UnderlineOption.getValue()

    override fun isStrikeThrough(): Boolean = StrikeThroughOption.getValue()

    override fun getLeftMargin(metrics: ZLTextMetrics): Int = 0

    override fun getRightMargin(metrics: ZLTextMetrics): Int = 0

    override fun getLeftPadding(metrics: ZLTextMetrics): Int = 0

    override fun getRightPadding(metrics: ZLTextMetrics): Int = 0

    override fun getFirstLineIndent(metrics: ZLTextMetrics): Int = 0

    override fun getLineSpacePercent(): Int = LineSpaceOption.getValue() * 10

    override fun getVerticalAlign(metrics: ZLTextMetrics): Int = 0

    override fun isVerticallyAligned(): Boolean = false

    override fun getSpaceBefore(metrics: ZLTextMetrics): Int = 0

    override fun getSpaceAfter(metrics: ZLTextMetrics): Int = 0

    override fun getAlignment(): Byte = AlignmentOption.getValue().toByte()

    override fun allowHyphenations(): Boolean = true

    companion object {
        private const val GROUP = "Style"
        private const val OPTIONS = "Options"
    }
}
