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

package org.geometerplus.android.fbreader.api

import android.content.Intent
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.AbstractSerializer
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.SerializerUtil

object FBReaderIntents {

    const val DEFAULT_PACKAGE = "org.geometerplus.zlibrary.ui.android"

    @JvmStatic
    fun defaultInternalIntent(action: String): Intent {
        return internalIntent(action).addCategory(Intent.CATEGORY_DEFAULT)
    }

    @JvmStatic
    fun internalIntent(action: String): Intent {
        return Intent(action).setPackage(DEFAULT_PACKAGE)
    }

    @JvmStatic
    fun putBookExtra(intent: Intent, key: String, book: Book) {
        intent.putExtra(key, SerializerUtil.serialize(book))
    }

    @JvmStatic
    fun putBookExtra(intent: Intent, book: Book) {
        putBookExtra(intent, Key.BOOK, book)
    }

    @JvmStatic
    fun <B : AbstractBook> getBookExtra(intent: Intent, key: String, creator: AbstractSerializer.BookCreator<B>): B? {
        return SerializerUtil.deserializeBook(intent.getStringExtra(key), creator)
    }

    @JvmStatic
    fun <B : AbstractBook> getBookExtra(intent: Intent, creator: AbstractSerializer.BookCreator<B>): B? {
        return getBookExtra(intent, Key.BOOK, creator)
    }

    @JvmStatic
    fun putBookmarkExtra(intent: Intent, key: String, bookmark: Bookmark) {
        intent.putExtra(key, SerializerUtil.serialize(bookmark))
    }

    @JvmStatic
    fun putBookmarkExtra(intent: Intent, bookmark: Bookmark) {
        putBookmarkExtra(intent, Key.BOOKMARK, bookmark)
    }

    @JvmStatic
    fun getBookmarkExtra(intent: Intent, key: String): Bookmark? {
        return SerializerUtil.deserializeBookmark(intent.getStringExtra(key))
    }

    @JvmStatic
    fun getBookmarkExtra(intent: Intent): Bookmark? {
        return getBookmarkExtra(intent, Key.BOOKMARK)
    }

    object Action {
        const val API = "android.fbreader.action.API"
        const val API_CALLBACK = "android.fbreader.action.API_CALLBACK"
        const val VIEW = "android.fbreader.action.VIEW"
        const val CANCEL_MENU = "android.fbreader.action.CANCEL_MENU"
        const val CONFIG_SERVICE = "android.fbreader.action.CONFIG_SERVICE"
        const val LIBRARY_SERVICE = "android.fbreader.action.LIBRARY_SERVICE"
        const val BOOK_INFO = "android.fbreader.action.BOOK_INFO"
        const val LIBRARY = "android.fbreader.action.LIBRARY"
        const val EXTERNAL_LIBRARY = "android.fbreader.action.EXTERNAL_LIBRARY"
        const val BOOKMARKS = "android.fbreader.action.BOOKMARKS"
        const val EXTERNAL_BOOKMARKS = "android.fbreader.action.EXTERNAL_BOOKMARKS"
        const val PREFERENCES = "android.fbreader.action.PREFERENCES"
        const val NETWORK_LIBRARY = "android.fbreader.action.NETWORK_LIBRARY"
        const val OPEN_NETWORK_CATALOG = "android.fbreader.action.OPEN_NETWORK_CATALOG"
        const val ERROR = "android.fbreader.action.ERROR"
        const val CRASH = "android.fbreader.action.CRASH"
        const val PLUGIN = "android.fbreader.action.PLUGIN"
        const val CLOSE = "android.fbreader.action.CLOSE"
        const val PLUGIN_CRASH = "android.fbreader.action.PLUGIN_CRASH"
        const val EDIT_STYLES = "android.fbreader.action.EDIT_STYLES"
        const val EDIT_BOOKMARK = "android.fbreader.action.EDIT_BOOKMARK"
        const val SWITCH_YOTA_SCREEN = "android.fbreader.action.SWITCH_YOTA_SCREEN"

        const val SYNC_START = "android.fbreader.action.sync.START"
        const val SYNC_STOP = "android.fbreader.action.sync.STOP"
        const val SYNC_SYNC = "android.fbreader.action.sync.SYNC"
        const val SYNC_QUICK_SYNC = "android.fbreader.action.sync.QUICK_SYNC"

        const val PLUGIN_VIEW = "android.fbreader.action.plugin.VIEW"
        const val PLUGIN_KILL = "android.fbreader.action.plugin.KILL"
        const val PLUGIN_CONNECT_COVER_SERVICE = "android.fbreader.action.plugin.CONNECT_COVER_SERVICE"
    }

    object Event {
        const val CONFIG_OPTION_CHANGE = "fbreader.config_service.option_change_event"

        const val LIBRARY_BOOK = "fbreader.library_service.book_event"
        const val LIBRARY_BUILD = "fbreader.library_service.build_event"
        const val LIBRARY_COVER_READY = "fbreader.library_service.cover_ready"

        const val SYNC_UPDATED = "android.fbreader.event.sync.UPDATED"
    }

    object Key {
        const val BOOK = "fbreader.book"
        const val BOOKMARK = "fbreader.bookmark"
        const val PLUGIN = "fbreader.plugin"
        const val TYPE = "fbreader.type"
    }
}
