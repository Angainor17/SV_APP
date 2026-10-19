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

package org.geometerplus.zlibrary.core.util

/**
 * Color is presented as the triple of short's (Red, Green, Blue components).
 * Each component should be in the range 0..255
 */
class ZLColor(r: Int, g: Int, b: Int) {

    @JvmField
    val Red: Short = (r and 0xFF).toShort()

    @JvmField
    val Green: Short = (g and 0xFF).toShort()

    @JvmField
    val Blue: Short = (b and 0xFF).toShort()

    constructor(intValue: Int) : this(
        (intValue shr 16) and 0xFF,
        (intValue shr 8) and 0xFF,
        intValue and 0xFF
    )

    fun intValue(): Int = (Red.toInt() shl 16) + (Green.toInt() shl 8) + Blue.toInt()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ZLColor) {
            return false
        }
        return Red == other.Red && Green == other.Green && Blue == other.Blue
    }

    override fun hashCode(): Int = intValue()

    override fun toString(): String = "ZLColor($Red, $Green, $Blue)"
}
