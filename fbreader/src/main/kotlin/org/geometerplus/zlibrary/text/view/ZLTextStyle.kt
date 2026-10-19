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

package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextMetrics

abstract class ZLTextStyle protected constructor(
    parent: ZLTextStyle?,
    @JvmField val Hyperlink: ZLTextHyperlink?,
) {
    @JvmField
    val Parent: ZLTextStyle = parent ?: this

    abstract fun getFontEntries(): List<FontEntry>

    abstract fun getFontSize(metrics: ZLTextMetrics): Int

    abstract fun isBold(): Boolean

    abstract fun isItalic(): Boolean

    abstract fun isUnderline(): Boolean

    abstract fun isStrikeThrough(): Boolean

    fun getLeftIndent(metrics: ZLTextMetrics): Int =
        getLeftMargin(metrics) + getLeftPadding(metrics)

    fun getRightIndent(metrics: ZLTextMetrics): Int =
        getRightMargin(metrics) + getRightPadding(metrics)

    abstract fun getLeftMargin(metrics: ZLTextMetrics): Int

    abstract fun getRightMargin(metrics: ZLTextMetrics): Int

    abstract fun getLeftPadding(metrics: ZLTextMetrics): Int

    abstract fun getRightPadding(metrics: ZLTextMetrics): Int

    abstract fun getFirstLineIndent(metrics: ZLTextMetrics): Int

    abstract fun getLineSpacePercent(): Int

    abstract fun getVerticalAlign(metrics: ZLTextMetrics): Int

    abstract fun isVerticallyAligned(): Boolean

    abstract fun getSpaceBefore(metrics: ZLTextMetrics): Int

    abstract fun getSpaceAfter(metrics: ZLTextMetrics): Int

    abstract fun getAlignment(): Byte

    abstract fun allowHyphenations(): Boolean
}
