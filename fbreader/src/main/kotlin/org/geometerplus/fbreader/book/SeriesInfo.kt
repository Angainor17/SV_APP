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

class SeriesInfo(title: String, index: BigDecimal?) : Comparable<SeriesInfo> {

    @JvmField
    val Series: Series = Series(title)

    @JvmField
    val Index: BigDecimal? = index

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SeriesInfo) {
            return false
        }
        return ComparisonUtil.equal(Series, other.Series) && ComparisonUtil.equal(Index, other.Index)
    }

    override fun hashCode(): Int =
        23 * ComparisonUtil.hashCode(Series) + 31 * ComparisonUtil.hashCode(Index)

    override fun compareTo(other: SeriesInfo): Int {
        val i0 = Index ?: BigDecimal.ZERO
        val i1 = other.Index ?: BigDecimal.ZERO
        return i0.compareTo(i1)
    }

    companion object {
        @JvmStatic
        fun createSeriesInfo(title: String?, index: String?): SeriesInfo? {
            if (title == null) {
                return null
            }
            return SeriesInfo(title, createIndex(index))
        }

        @JvmStatic
        fun createIndex(index: String?): BigDecimal? {
            return try {
                if (index != null) BigDecimal(index).stripTrailingZeros() else null
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
