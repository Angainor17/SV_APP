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

package org.geometerplus.fbreader.book

import org.fbreader.util.ComparisonUtil
import java.math.BigDecimal

class SeriesInfo internal constructor(
    @JvmField val series: Series,
    @JvmField val index: BigDecimal?
) : Comparable<SeriesInfo> {

    companion object {
        @JvmStatic
        fun createSeriesInfo(title: String?, index: String?): SeriesInfo? {
            if (title == null) {
                return null
            }
            return SeriesInfo(Series(title), createIndex(index))
        }

        @JvmStatic
        fun createIndex(index: String?): BigDecimal? {
            return try {
                index?.let { BigDecimal(it).stripTrailingZeros() }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SeriesInfo) return false
        return ComparisonUtil.equal(series, other.series) && ComparisonUtil.equal(index, other.index)
    }

    override fun hashCode(): Int = 23 * ComparisonUtil.hashCode(series) + 31 * ComparisonUtil.hashCode(index)

    override fun compareTo(other: SeriesInfo): Int {
        val i0 = index ?: BigDecimal.ZERO
        val i1 = other.index ?: BigDecimal.ZERO
        return i0.compareTo(i1)
    }
}
