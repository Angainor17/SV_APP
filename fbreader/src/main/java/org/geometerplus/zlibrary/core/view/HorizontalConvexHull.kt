/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

import android.graphics.Rect
import java.util.LinkedList

class HorizontalConvexHull(rects: MutableCollection<Rect>) : Hull {
    private val myRectangles = LinkedList<Rect>()

    init {
        for (r in rects) {
            addRect(r)
        }
        normalize()
    }

    private fun addRect(rectangle: Rect) {
        if (myRectangles.isEmpty()) {
            myRectangles.add(Rect(rectangle))
            return
        }
        val top = rectangle.top
        val bottom = rectangle.bottom
        val iter = myRectangles.listIterator()
        while (iter.hasNext()) {
            val r = iter.next()
            if (r.bottom <= top) {
                continue
            }
            if (r.top >= bottom) {
                break
            }
            if (r.top < top) {
                val before = Rect(r)
                before.bottom = top
                r.top = top
                iter.previous()
                iter.add(before)
                iter.next()
            }
            if (r.bottom > bottom) {
                val after = Rect(r)
                after.top = bottom
                r.bottom = bottom
                iter.add(after)
            }
            r.left = minOf(r.left, rectangle.left)
            r.right = maxOf(r.right, rectangle.right)
        }

        val first = myRectangles.first()
        if (top < first.top) {
            myRectangles.add(0, Rect(rectangle.left, top, rectangle.right, minOf(bottom, first.top)))
        }

        val last = myRectangles.last()
        if (bottom > last.bottom) {
            myRectangles.add(Rect(rectangle.left, maxOf(top, last.bottom), rectangle.right, bottom))
        }
    }

    private fun normalize() {
        var previous: Rect? = null
        val iter = myRectangles.listIterator()
        while (iter.hasNext()) {
            val current = iter.next()
            if (previous != null) {
                if (previous.left == current.left && previous.right == current.right) {
                    previous.bottom = current.bottom
                    iter.remove()
                    continue
                }
                if (previous.bottom != current.top &&
                    current.left <= previous.right &&
                    previous.left <= current.right
                ) {
                    iter.previous()
                    iter.add(
                        Rect(
                            maxOf(previous.left, current.left),
                            previous.bottom,
                            minOf(previous.right, current.right),
                            current.top
                        )
                    )
                    iter.next()
                }
            }
            previous = current
        }
    }

    override fun distanceTo(x: Int, y: Int): Int {
        var distance = Int.MAX_VALUE
        for (r in myRectangles) {
            val xd = if (r.left > x) r.left - x else if (r.right < x) x - r.right else 0
            val yd = if (r.top > y) r.top - y else if (r.bottom < y) y - r.bottom else 0
            distance = minOf(distance, maxOf(xd, yd))
            if (distance == 0) {
                break
            }
        }
        return distance
    }

    override fun isBefore(x: Int, y: Int): Boolean {
        for (r in myRectangles) {
            if (r.bottom < y || (r.top < y && r.right < x)) {
                return true
            }
        }
        return false
    }

    override fun draw(context: ZLPaintContext, mode: Int) {
        if (mode == Hull.DrawMode.None) {
            return
        }

        val rectangles = LinkedList(myRectangles)
        while (rectangles.isNotEmpty()) {
            val connected = LinkedList<Rect>()
            var previous: Rect? = null
            val rectIter = rectangles.iterator()
            while (rectIter.hasNext()) {
                val current = rectIter.next()
                if (previous != null &&
                    (previous.left > current.right || current.left > previous.right)
                ) {
                    break
                }
                rectIter.remove()
                connected.add(current)
                previous = current
            }

            val xList = LinkedList<Int>()
            val yList = LinkedList<Int>()
            var x = 0
            var xPrev = 0

            val iter = connected.listIterator()
            var r = iter.next()
            x = r.right + 2
            xList.add(x)
            yList.add(r.top)
            while (iter.hasNext()) {
                xPrev = x
                r = iter.next()
                x = r.right + 2
                if (x != xPrev) {
                    val y = if (x < xPrev) r.top + 2 else r.top
                    xList.add(xPrev)
                    yList.add(y)
                    xList.add(x)
                    yList.add(y)
                }
            }
            xList.add(x)
            yList.add(r.bottom + 2)

            r = iter.previous()
            x = r.left - 2
            xList.add(x)
            yList.add(r.bottom + 2)
            while (iter.hasPrevious()) {
                xPrev = x
                r = iter.previous()
                x = r.left - 2
                if (x != xPrev) {
                    val y = if (x > xPrev) r.bottom else r.bottom + 2
                    xList.add(xPrev)
                    yList.add(y)
                    xList.add(x)
                    yList.add(y)
                }
            }
            xList.add(x)
            yList.add(r.top)

            val xs = xList.toIntArray()
            val ys = yList.toIntArray()

            if ((mode and Hull.DrawMode.Fill) == Hull.DrawMode.Fill) {
                context.fillPolygon(xs, ys)
            }
            if ((mode and Hull.DrawMode.Outline) == Hull.DrawMode.Outline) {
                context.drawOutline(xs, ys)
            }
        }
    }
}
