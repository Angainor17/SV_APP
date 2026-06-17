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

package org.geometerplus.fbreader.network.rss

import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import java.util.LinkedList

class RSSBookItem : NetworkBookItem {

    constructor(
        link: RSSNetworkLink, id: String, index: Int,
        title: CharSequence, summary: CharSequence?,
        authors: List<AuthorData>, tags: MutableList<String>,
        seriesTitle: String?, indexInSeries: Float,
        urls: UrlInfoCollection<*>?
    ) : super(link, id, index, title, summary, authors, tags, seriesTitle, indexInSeries, urls)

    internal constructor(networkLink: RSSNetworkLink, entry: RSSItem, baseUrl: String, index: Int) : this(
        networkLink, entry.id?.uri ?: "", index,
        entry.title ?: "", getAnnotation(entry),
        getAuthors(entry), LinkedList(getTags(entry)), null, 0f, null
    )

    companion object {
        private fun getAnnotation(entry: RSSItem): CharSequence? {
            if (entry.content != null) {
                return entry.content
            }
            if (entry.summary != null) {
                return entry.summary
            }
            return null
        }

        private fun getAuthors(entry: RSSItem): List<AuthorData> {
            val authors = LinkedList<AuthorData>()
            for (author in entry.authors) {
                val name = author.name ?: ""
                authors.add(AuthorData(name, name.lowercase()))
            }
            return authors
        }

        private fun getTags(entry: RSSItem): List<String> {
            val tags = LinkedList<String>()
            for (category in entry.categories) {
                var label = category.label
                if (label == null) {
                    label = category.term
                }
                if (label != null) {
                    tags.add(label)
                }
            }
            return tags
        }
    }
}
