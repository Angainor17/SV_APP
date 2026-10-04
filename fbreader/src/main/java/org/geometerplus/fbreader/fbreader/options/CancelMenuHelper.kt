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

package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.util.ArrayList
import java.util.Collections

class CancelMenuHelper {
    @JvmField
    val ShowLibraryItemOption = ZLBooleanOption(GROUP_NAME, "library", true)

    @JvmField
    val ShowPreviousBookItemOption = ZLBooleanOption(GROUP_NAME, "previousBook", false)

    @JvmField
    val ShowPositionItemsOption = ZLBooleanOption(GROUP_NAME, "positions", true)

    init {
        Config.Instance().requestAllValuesForGroup(GROUP_NAME)
    }

    fun getActionsList(collection: IBookCollection<Book>): List<ActionDescription> {
        val list = ArrayList<ActionDescription>()

        if (ShowLibraryItemOption.getValue()) {
            list.add(ActionDescription(ActionType.library, null))
        }
        if (ShowPreviousBookItemOption.getValue()) {
            val previousBook = collection.getRecentBook(1)
            if (previousBook != null) {
                list.add(ActionDescription(ActionType.previousBook, previousBook.getTitle()))
            }
        }
        if (ShowPositionItemsOption.getValue()) {
            val currentBook = collection.getRecentBook(0)
            if (currentBook != null) {
                val bookmarks = collection.bookmarks(BookmarkQuery(currentBook, false, 3))
                Collections.sort(bookmarks, Bookmark.ByTimeComparator())
                for (b in bookmarks) {
                    list.add(BookmarkDescription(b))
                }
            }
        }
        list.add(ActionDescription(ActionType.close, null))

        return list
    }

    @Suppress("EnumEntryName")
    enum class ActionType {
        library, previousBook, returnTo, close
    }

    open class ActionDescription(type: ActionType, summary: String?) {
        @JvmField
        val Type: ActionType

        @JvmField
        val Title: String

        @JvmField
        val Summary: String?

        init {
            val resource = ZLResource.resource("cancelMenu")
            Type = type
            Title = resource.getResource(type.toString()).getValue()
            Summary = summary
        }
    }

    class BookmarkDescription(b: Bookmark) : ActionDescription(ActionType.returnTo, b.getText()) {
        @JvmField
        val Bookmark: Bookmark = b
    }

    companion object {
        private const val GROUP_NAME = "CancelMenu"
    }
}
