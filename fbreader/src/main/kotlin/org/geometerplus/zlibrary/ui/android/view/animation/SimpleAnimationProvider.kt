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

package org.geometerplus.zlibrary.ui.android.view.animation

import org.geometerplus.zlibrary.core.view.ZLViewEnums
import kotlin.math.pow

abstract class SimpleAnimationProvider(bitmapManager: BitmapManager) :
    AnimationProvider(bitmapManager) {

    private var mySpeedFactor: Float = 0f

    override fun getPageToScrollTo(x: Int, y: Int): ZLViewEnums.PageIndex {
        val direction = myDirection ?: return ZLViewEnums.PageIndex.current

        return when (direction) {
            ZLViewEnums.Direction.rightToLeft ->
                if (myStartX < x) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.leftToRight ->
                if (myStartX < x) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
            ZLViewEnums.Direction.up ->
                if (myStartY < y) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.down ->
                if (myStartY < y) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
        }
    }

    override fun setupAnimatedScrollingStart(x: Int?, y: Int?) {
        var newX = x
        var newY = y
        if (newX == null || newY == null) {
            if (myDirection!!.IsHorizontal) {
                newX = if (mySpeed < 0f) myWidth else 0
                newY = 0
            } else {
                newX = 0
                newY = if (mySpeed < 0f) myHeight else 0
            }
        }
        myEndX = newX!!
        myStartX = newX
        myEndY = newY!!
        myStartY = newY
    }

    override fun startAnimatedScrollingInternal(speed: Int) {
        mySpeedFactor = 1.5.pow(0.25 * speed).toFloat()
        doStep()
    }

    override fun doStep() {
        if (!getMode().Auto) {
            return
        }

        val direction = myDirection!!
        when (direction) {
            ZLViewEnums.Direction.leftToRight -> myEndX -= mySpeed.toInt()
            ZLViewEnums.Direction.rightToLeft -> myEndX += mySpeed.toInt()
            ZLViewEnums.Direction.up -> myEndY += mySpeed.toInt()
            ZLViewEnums.Direction.down -> myEndY -= mySpeed.toInt()
        }
        val bound: Int
        if (getMode() == Mode.AnimatedScrollingForward) {
            bound = if (direction.IsHorizontal) myWidth else myHeight
        } else {
            bound = 0
        }
        if (mySpeed > 0f) {
            if (getScrollingShift() >= bound) {
                if (direction.IsHorizontal) {
                    myEndX = myStartX + bound
                } else {
                    myEndY = myStartY + bound
                }
                terminate()
                return
            }
        } else {
            if (getScrollingShift() <= -bound) {
                if (direction.IsHorizontal) {
                    myEndX = myStartX - bound
                } else {
                    myEndY = myStartY - bound
                }
                terminate()
                return
            }
        }
        mySpeed *= mySpeedFactor
    }
}
