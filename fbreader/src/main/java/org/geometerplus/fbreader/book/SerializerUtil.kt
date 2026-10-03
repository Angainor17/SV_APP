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

object SerializerUtil {
    private val defaultSerializer: AbstractSerializer = XMLSerializer()

    @JvmStatic
    fun serialize(query: BookQuery?): String? =
        query?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookQuery(xml: String?): BookQuery? =
        xml?.let { defaultSerializer.deserializeBookQuery(it) }

    @JvmStatic
    fun serialize(query: BookmarkQuery?): String? =
        query?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookmarkQuery(xml: String?, creator: AbstractSerializer.BookCreator<out AbstractBook>): BookmarkQuery? =
        xml?.let { defaultSerializer.deserializeBookmarkQuery(it, creator) }

    @JvmStatic
    fun serialize(book: AbstractBook?): String? =
        book?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun <B : AbstractBook> deserializeBook(xml: String?, creator: AbstractSerializer.BookCreator<B>): B? =
        xml?.let { defaultSerializer.deserializeBook(it, creator) }

    @JvmStatic
    fun serializeBookList(books: List<AbstractBook>): List<String> =
        books.map { defaultSerializer.serialize(it) }

    @JvmStatic
    fun <B : AbstractBook> deserializeBookList(xmlList: List<String>, creator: AbstractSerializer.BookCreator<B>): List<B> =
        xmlList.mapNotNull { defaultSerializer.deserializeBook(it, creator) }

    @JvmStatic
    fun serialize(bookmark: Bookmark?): String? =
        bookmark?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookmark(xml: String?): Bookmark? =
        xml?.let { defaultSerializer.deserializeBookmark(it) }

    @JvmStatic
    fun serializeBookmarkList(bookmarks: List<Bookmark>): List<String> =
        bookmarks.map { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookmarkList(xmlList: List<String>): List<Bookmark> =
        xmlList.mapNotNull { defaultSerializer.deserializeBookmark(it) }

    @JvmStatic
    fun serialize(style: HighlightingStyle?): String? =
        style?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeStyle(xml: String?): HighlightingStyle? =
        xml?.let { defaultSerializer.deserializeStyle(it) }

    @JvmStatic
    fun serializeStyleList(styles: List<HighlightingStyle>): List<String> =
        styles.map { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeStyleList(xmlList: List<String>): List<HighlightingStyle> =
        xmlList.mapNotNull { defaultSerializer.deserializeStyle(it) }
}
