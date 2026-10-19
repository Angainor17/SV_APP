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

package org.geometerplus.zlibrary.text.model

class ZLTextMark : Comparable<ZLTextMark> {

    @JvmField
    val paragraphIndex: Int

    @JvmField
    val offset: Int

    @JvmField
    val length: Int

    constructor(paragraphIndex: Int, offset: Int, length: Int) {
        this.paragraphIndex = paragraphIndex
        this.offset = offset
        this.length = length
    }

    constructor(mark: ZLTextMark) {
        paragraphIndex = mark.paragraphIndex
        offset = mark.offset
        length = mark.length
    }

    override fun compareTo(other: ZLTextMark): Int {
        val diff = paragraphIndex - other.paragraphIndex
        return if (diff != 0) diff else offset - other.offset
    }

    override fun toString(): String = "$paragraphIndex $offset $length"
}
