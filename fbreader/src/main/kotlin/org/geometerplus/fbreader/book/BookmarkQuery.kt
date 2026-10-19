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

class BookmarkQuery(
    @JvmField val Book: AbstractBook?,
    @JvmField val Visible: Boolean,
    @JvmField val Limit: Int,
    @JvmField val Page: Int
) {
    constructor(limit: Int) : this(null, limit)

    constructor(book: AbstractBook?, limit: Int) : this(book, true, limit)

    constructor(book: AbstractBook?, visible: Boolean, limit: Int) : this(book, visible, limit, 0)

    fun next(): BookmarkQuery = BookmarkQuery(Book, Visible, Limit, Page + 1)
}
