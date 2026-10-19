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

open class ZLTextFixedPosition(
    @JvmField val ParagraphIndex: Int,
    @JvmField val ElementIndex: Int,
    @JvmField val CharIndex: Int,
) : ZLTextPosition() {

    constructor(position: ZLTextPosition) : this(
        position.paragraphIndex,
        position.elementIndex,
        position.charIndex,
    )

    override val paragraphIndex: Int
        get() = ParagraphIndex

    override val elementIndex: Int
        get() = ElementIndex

    override val charIndex: Int
        get() = CharIndex

    class WithTimestamp(
        paragraphIndex: Int,
        elementIndex: Int,
        charIndex: Int,
        stamp: Long?,
    ) : ZLTextFixedPosition(paragraphIndex, elementIndex, charIndex) {
        @JvmField
        val Timestamp: Long = stamp ?: -1L

        override fun toString(): String = "${super.toString()}; timestamp = $Timestamp"
    }
}
