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

import java.util.Locale

abstract class Filter {
    abstract fun matches(book: AbstractBook): Boolean

    class Empty : Filter() {
        override fun matches(book: AbstractBook): Boolean = true
    }

    class ByAuthor(@JvmField val Author: Author) : Filter() {
        override fun matches(book: AbstractBook): Boolean {
            val bookAuthors = book.authors()
            return if (org.geometerplus.fbreader.book.Author.NULL == Author) {
                bookAuthors.isEmpty()
            } else {
                bookAuthors.contains(Author)
            }
        }
    }

    class ByTag(@JvmField val Tag: Tag) : Filter() {
        override fun matches(book: AbstractBook): Boolean {
            val bookTags = book.tags()
            return if (org.geometerplus.fbreader.book.Tag.NULL == Tag) {
                bookTags.isEmpty()
            } else {
                bookTags.contains(Tag)
            }
        }
    }

    class ByLabel(@JvmField val Label: String) : Filter() {
        override fun matches(book: AbstractBook): Boolean = book.hasLabel(Label)
    }

    class ByPattern(pattern: String?) : Filter() {
        @JvmField
        val Pattern: String = pattern?.lowercase(Locale.ROOT) ?: ""

        override fun matches(book: AbstractBook): Boolean =
            Pattern != "" && book.matches(Pattern)
    }

    class ByTitlePrefix(prefix: String?) : Filter() {
        @JvmField
        val Prefix: String = prefix ?: ""

        override fun matches(book: AbstractBook): Boolean =
            Prefix == book.firstTitleLetter()
    }

    class BySeries(@JvmField val Series: Series) : Filter() {
        override fun matches(book: AbstractBook): Boolean {
            val info = book.getSeriesInfo()
            return info != null && Series == info.Series
        }
    }

    class HasBookmark : Filter() {
        override fun matches(book: AbstractBook): Boolean = book.HasBookmark
    }

    class HasPhysicalFile : Filter() {
        override fun matches(book: AbstractBook): Boolean =
            book.getPath().startsWith("/")
    }

    class And(@JvmField val First: Filter, @JvmField val Second: Filter) : Filter() {
        override fun matches(book: AbstractBook): Boolean = First.matches(book) && Second.matches(book)
    }

    class Or(@JvmField val First: Filter, @JvmField val Second: Filter) : Filter() {
        override fun matches(book: AbstractBook): Boolean = First.matches(book) || Second.matches(book)
    }

    class Not(@JvmField val Base: Filter) : Filter() {
        override fun matches(book: AbstractBook): Boolean = !Base.matches(book)
    }
}
