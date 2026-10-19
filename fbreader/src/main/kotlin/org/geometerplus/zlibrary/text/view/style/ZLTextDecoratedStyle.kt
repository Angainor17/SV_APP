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
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle

abstract class ZLTextDecoratedStyle(
    base: ZLTextStyle,
    hyperlink: ZLTextHyperlink?,
) : ZLTextStyle(base, hyperlink ?: base.Hyperlink) {

    @JvmField
    protected val BaseStyle: ZLTextBaseStyle =
        if (base is ZLTextBaseStyle) base else (base as ZLTextDecoratedStyle).BaseStyle

    private var myFontEntries: List<FontEntry>? = null
    private var myIsItalic = false
    private var myIsBold = false
    private var myIsUnderline = false
    private var myIsStrikeThrough = false
    private var myLineSpacePercent = 0

    private var myIsNotCached = true

    private var myFontSize = 0
    private var mySpaceBefore = 0
    private var mySpaceAfter = 0
    private var myVerticalAlign = 0
    private var myIsVerticallyAligned: Boolean? = null
    private var myLeftMargin = 0
    private var myRightMargin = 0
    private var myLeftPadding = 0
    private var myRightPadding = 0
    private var myFirstLineIndent = 0
    private var myMetrics: ZLTextMetrics? = null

    private fun initCache() {
        myFontEntries = getFontEntriesInternal()
        myIsItalic = isItalicInternal()
        myIsBold = isBoldInternal()
        myIsUnderline = isUnderlineInternal()
        myIsStrikeThrough = isStrikeThroughInternal()
        myLineSpacePercent = getLineSpacePercentInternal()

        myIsNotCached = false
    }

    private fun initMetricsCache(metrics: ZLTextMetrics) {
        myMetrics = metrics
        myFontSize = getFontSizeInternal(metrics)
        mySpaceBefore = getSpaceBeforeInternal(metrics, myFontSize)
        mySpaceAfter = getSpaceAfterInternal(metrics, myFontSize)
        myVerticalAlign = getVerticalAlignInternal(metrics, myFontSize)
        myLeftMargin = getLeftMarginInternal(metrics, myFontSize)
        myRightMargin = getRightMarginInternal(metrics, myFontSize)
        myLeftPadding = getLeftPaddingInternal(metrics, myFontSize)
        myRightPadding = getRightPaddingInternal(metrics, myFontSize)
        myFirstLineIndent = getFirstLineIndentInternal(metrics, myFontSize)
    }

    final override fun getFontEntries(): List<FontEntry> {
        if (myIsNotCached) {
            initCache()
        }
        return myFontEntries!!
    }

    protected abstract fun getFontEntriesInternal(): List<FontEntry>

    final override fun getFontSize(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myFontSize
    }

    protected abstract fun getFontSizeInternal(metrics: ZLTextMetrics): Int

    final override fun getSpaceBefore(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return mySpaceBefore
    }

    protected abstract fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getSpaceAfter(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return mySpaceAfter
    }

    protected abstract fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun isItalic(): Boolean {
        if (myIsNotCached) {
            initCache()
        }
        return myIsItalic
    }

    protected abstract fun isItalicInternal(): Boolean

    final override fun isBold(): Boolean {
        if (myIsNotCached) {
            initCache()
        }
        return myIsBold
    }

    protected abstract fun isBoldInternal(): Boolean

    final override fun isUnderline(): Boolean {
        if (myIsNotCached) {
            initCache()
        }
        return myIsUnderline
    }

    protected abstract fun isUnderlineInternal(): Boolean

    final override fun isStrikeThrough(): Boolean {
        if (myIsNotCached) {
            initCache()
        }
        return myIsStrikeThrough
    }

    protected abstract fun isStrikeThroughInternal(): Boolean

    final override fun getVerticalAlign(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myVerticalAlign
    }

    protected abstract fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun isVerticallyAligned(): Boolean {
        if (myIsVerticallyAligned == null) {
            myIsVerticallyAligned = Parent.isVerticallyAligned() || isVerticallyAlignedInternal()
        }
        return myIsVerticallyAligned!!
    }

    protected abstract fun isVerticallyAlignedInternal(): Boolean

    final override fun getLeftMargin(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myLeftMargin
    }

    protected abstract fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getRightMargin(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myRightMargin
    }

    protected abstract fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getLeftPadding(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myLeftPadding
    }

    protected abstract fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getRightPadding(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myRightPadding
    }

    protected abstract fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getFirstLineIndent(metrics: ZLTextMetrics): Int {
        if (metrics != myMetrics) {
            initMetricsCache(metrics)
        }
        return myFirstLineIndent
    }

    protected abstract fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    final override fun getLineSpacePercent(): Int {
        if (myIsNotCached) {
            initCache()
        }
        return myLineSpacePercent
    }

    protected abstract fun getLineSpacePercentInternal(): Int
}
