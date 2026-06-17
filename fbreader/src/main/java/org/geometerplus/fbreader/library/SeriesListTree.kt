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

package org.geometerplus.fbreader.library

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.Series
import org.geometerplus.fbreader.book.SeriesInfo

import java.util.Collections

class SeriesListTree internal constructor(root: RootTree) : FirstLevelTree(root, ROOT_BY_SERIES) {

    override val openingStatus: Status get() {
        return if (!collection.hasSeries()) {
            Status.CANNOT_OPEN
        } else {
            Status.ALWAYS_RELOAD_BEFORE_OPENING
        }
    }

    override val openingStatusMessage: String? get() =
        if (openingStatus == Status.CANNOT_OPEN) "noSeries" else super.openingStatusMessage

    override fun waitForOpening() {
        clear()
        for (s in collection.series()) {
            createSeriesSubtree(s)
        }
    }

    override fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added, BookEvent.Updated -> {
                val info: SeriesInfo? = book.getSeriesInfo()
                info != null && createSeriesSubtree(info.series.getTitle())
            }
            BookEvent.Removed -> false
            else -> false
        }
    }

    private fun createSeriesSubtree(seriesTitle: String): Boolean {
        val series = Series(seriesTitle)
        val temp = SeriesTree(collection, pluginCollection, series, null)
        val position = Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            SeriesTree(this, series, null, -position - 1)
            true
        }
    }
}
