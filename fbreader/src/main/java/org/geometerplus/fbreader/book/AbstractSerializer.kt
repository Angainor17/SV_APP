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

package org.geometerplus.fbreader.book

abstract class AbstractSerializer {
    abstract fun serialize(query: BookQuery): String

    abstract fun deserializeBookQuery(data: String): BookQuery?

    abstract fun serialize(query: BookmarkQuery): String

    abstract fun deserializeBookmarkQuery(data: String, creator: BookCreator<out AbstractBook>): BookmarkQuery?

    abstract fun serialize(book: AbstractBook): String

    abstract fun <B : AbstractBook> deserializeBook(data: String, creator: BookCreator<B>): B?

    abstract fun serialize(bookmark: Bookmark): String

    abstract fun deserializeBookmark(data: String): Bookmark?

    abstract fun serialize(style: HighlightingStyle): String

    abstract fun deserializeStyle(data: String): HighlightingStyle?

    interface BookCreator<B : AbstractBook> {
        fun createBook(id: Long, url: String?, title: String?, encoding: String?, language: String?): B
    }
}
