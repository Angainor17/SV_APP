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

class RationalNumber private constructor(numerator: Long, denominator: Long) {

    @JvmField
    val Numerator: Long

    @JvmField
    val Denominator: Long

    init {
        var n = numerator
        var d = denominator
        val g = gcd(n, d)
        if (g > 1) {
            n /= g
            d /= g
        }
        if (d < 0) {
            n = -n
            d = -d
        }
        Numerator = n
        Denominator = d
    }

    fun toFloat(): Float = 1.0f * Numerator / Denominator

    private fun gcd(a: Long, b: Long): Long {
        var x = if (a < 0) -a else a
        var y = if (b < 0) -b else b
        while (x != 0L && y != 0L) {
            if (x > y) {
                x %= y
            } else {
                y %= x
            }
        }
        return x + y
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RationalNumber) {
            return false
        }
        return Numerator == other.Numerator && Denominator == other.Denominator
    }

    override fun hashCode(): Int = (37 * Numerator + Denominator).toInt()

    companion object {
        @JvmStatic
        fun create(numerator: Long, denominator: Long): RationalNumber? {
            if (denominator == 0L) {
                return null
            }
            return RationalNumber(numerator, denominator)
        }
    }
}
