/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network

class NetworkBookItemComparator : Comparator<NetworkItem> {

    override fun compare(item0: NetworkItem, item1: NetworkItem): Int {
        val item0isABook = item0 is NetworkBookItem
        val item1isABook = item1 is NetworkBookItem

        val title0 = item0.title.toString()
        val title1 = item1.title.toString()

        if (!item0isABook && !item1isABook) {
            return title0.compareTo(title1)
        }
        if (!item0isABook || !item1isABook) {
            return if (item0isABook) 1 else -1
        }

        val book0 = item0 as NetworkBookItem
        val book1 = item1 as NetworkBookItem

        val authors0 = book0.authors
        val authors1 = book1.authors

        val authors0empty = authors0.isEmpty()
        val authors1empty = authors1.isEmpty()

        if (authors0empty && !authors1empty) {
            return -1
        }
        if (authors1empty && !authors0empty) {
            return 1
        }
        if (!authors0empty && !authors1empty) {
            val diff = authors0[0].sortKey.compareTo(authors1[0].sortKey)
            if (diff != 0) {
                return diff
            }
        }

        val book0HasSeriesTitle = book0.seriesTitle != null
        val book1HasSeriesTitle = book1.seriesTitle != null

        if (book0HasSeriesTitle && book1HasSeriesTitle) {
            val comp = book0.seriesTitle!!.compareTo(book1.seriesTitle!!)
            if (comp != 0) {
                return comp
            } else {
                val diff = book0.indexInSeries - book1.indexInSeries
                if (diff != 0f) {
                    return if (diff > 0) 1 else -1
                }
            }
            return title0.compareTo(title1)
        }

        val book0Key = if (book0HasSeriesTitle) book0.seriesTitle else title0
        val book1Key = if (book1HasSeriesTitle) book1.seriesTitle else title1
        val comp = book0Key!!.compareTo(book1Key!!)
        if (comp != 0) {
            return comp
        }
        return if (book1HasSeriesTitle) -1 else 1
    }
}
