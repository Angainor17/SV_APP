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

package org.geometerplus.zlibrary.core.view

import org.geometerplus.zlibrary.core.application.ZLApplication

abstract class ZLView protected constructor(@JvmField val Application: ZLApplication) : ZLViewEnums {
    private var myViewContext: ZLPaintContext = DummyPaintContext()

    fun getContext(): ZLPaintContext = myViewContext

    protected fun setContext(context: ZLPaintContext) {
        myViewContext = context
    }

    fun getContextWidth(): Int = myViewContext.getWidth()

    fun getContextHeight(): Int = myViewContext.getHeight()

    abstract val footerArea: FooterArea

    abstract fun getAnimationType(): ZLViewEnums.Animation

    abstract fun preparePage(context: ZLPaintContext, pageIndex: ZLViewEnums.PageIndex)

    abstract fun paint(context: ZLPaintContext, pageIndex: ZLViewEnums.PageIndex)

    abstract fun onScrollingFinished(pageIndex: ZLViewEnums.PageIndex)

    abstract fun onFingerPress(x: Int, y: Int)

    abstract fun onFingerRelease(x: Int, y: Int)

    abstract fun onFingerMove(x: Int, y: Int)

    abstract fun onFingerLongPress(x: Int, y: Int): Boolean

    abstract fun onFingerReleaseAfterLongPress(x: Int, y: Int)

    abstract fun onFingerMoveAfterLongPress(x: Int, y: Int)

    abstract fun onFingerSingleTap(x: Int, y: Int)

    abstract fun onFingerDoubleTap(x: Int, y: Int)

    abstract fun onFingerEventCancelled()

    open fun isDoubleTapSupported(): Boolean = false

    open fun onTrackballRotated(diffX: Int, diffY: Int): Boolean = false

    abstract fun isScrollbarShown(): Boolean

    abstract fun getScrollbarFullSize(): Int

    abstract fun getScrollbarThumbPosition(pageIndex: ZLViewEnums.PageIndex): Int

    abstract fun getScrollbarThumbLength(pageIndex: ZLViewEnums.PageIndex): Int

    abstract fun canScroll(index: ZLViewEnums.PageIndex): Boolean

    interface FooterArea {
        val height: Int

        fun paint(context: ZLPaintContext)
    }
}
