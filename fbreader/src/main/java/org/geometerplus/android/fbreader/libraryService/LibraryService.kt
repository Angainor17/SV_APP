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

package org.geometerplus.android.fbreader.libraryService

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.FileObserver
import android.os.IBinder
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.httpd.DataService
import org.geometerplus.android.fbreader.httpd.DataUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.BookCollection
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.BooksDatabase
import org.geometerplus.fbreader.book.DbBook
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.IBookCollection.Status
import org.geometerplus.fbreader.book.SerializerUtil
import org.geometerplus.fbreader.book.UID
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import java.util.LinkedList

class LibraryService : Service() {

    private val dataConnection = DataService.Connection()
    @Volatile private var myLibrary: LibraryImplementation? = null

    override fun onStart(intent: Intent?, startId: Int) {
        onStartCommand(intent, 0, startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return myLibrary
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(ourDatabaseLock) {
            if (ourDatabase == null) {
                ourDatabase = SQLiteBooksDatabase(this@LibraryService)
            }
        }
        myLibrary = LibraryImplementation(ourDatabase!!)

        bindService(
            Intent(this, DataService::class.java),
            dataConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        unbindService(dataConnection)

        myLibrary?.let {
            myLibrary = null
            it.deactivate()
        }
        super.onDestroy()
    }

    private class Observer internal constructor(
        path: String,
        private val myPrefix: String,
        private val myCollection: BookCollection
    ) : FileObserver(path, MASK) {

        override fun onEvent(event: Int, path: String?) {
            val maskedEvent = event and ALL_EVENTS
            when (maskedEvent) {
                MOVE_SELF -> {
                    // TODO: File(path) removed; stop watching (?)
                }
                MOVED_TO -> myCollection.rescan(myPrefix + path)
                MOVED_FROM, DELETE -> myCollection.rescan(myPrefix + path)
                DELETE_SELF -> {
                    // TODO: File(path) removed; watching is stopped automatically (?)
                }
                CLOSE_WRITE, ATTRIB -> myCollection.rescan(myPrefix + path)
                else -> System.err.println("Unexpected event $maskedEvent on $myPrefix$path")
            }
        }

        companion object {
            private const val MASK = MOVE_SELF or MOVED_TO or MOVED_FROM or DELETE_SELF or DELETE or CLOSE_WRITE or ATTRIB
        }
    }

    inner class LibraryImplementation internal constructor(
        private val myDatabase: BooksDatabase
    ) : LibraryInterface.Stub() {

        private val myFileObservers = LinkedList<FileObserver>()
        private var myCollection: BookCollection = BookCollection(
            Paths.systemInfo(this@LibraryService), myDatabase, Paths.bookPath()
        )

        init {
            reset(true)
        }

        override fun reset(force: Boolean) {
            Config.Instance()?.runOnConnect { resetInternal(force) }
        }

        private fun resetInternal(force: Boolean) {
            val bookDirectories = Paths.bookPath()
            if (!force &&
                myCollection.status() != IBookCollection.Status.NotStarted &&
                bookDirectories == myCollection.bookDirectories
            ) {
                return
            }

            deactivate()
            myFileObservers.clear()

            myCollection = BookCollection(
                Paths.systemInfo(this@LibraryService), myDatabase, bookDirectories
            )
            for (dir in bookDirectories) {
                val observer = Observer(dir, "$dir/", myCollection)
                observer.startWatching()
                myFileObservers.add(observer)
            }

            myCollection.addListener(object : IBookCollection.Listener<DbBook> {
                override fun onBookEvent(event: BookEvent, book: DbBook?) {
                    val intent = Intent(FBReaderIntents.Event.LIBRARY_BOOK)
                    intent.putExtra("type", event.toString())
                    intent.putExtra("book", SerializerUtil.serialize(book))
                    sendBroadcast(intent)
                }

                override fun onBuildEvent(status: Status) {
                    val intent = Intent(FBReaderIntents.Event.LIBRARY_BUILD)
                    intent.putExtra("type", status.toString())
                    sendBroadcast(intent)
                }
            })
            myCollection.startBuild()
        }

        fun deactivate() {
            for (observer in myFileObservers) {
                observer.stopWatching()
            }
        }

        override fun status(): String = myCollection.status().toString()

        override fun size(): Int = myCollection.size()

        override fun books(query: String): List<String> {
            val bookQuery = SerializerUtil.deserializeBookQuery(query) ?: return emptyList()
            return SerializerUtil.serializeBookList(myCollection.books(bookQuery))
        }

        override fun hasBooks(query: String): Boolean {
            val bookQuery = SerializerUtil.deserializeBookQuery(query) ?: return false
            return myCollection.hasBooks(bookQuery.filter)
        }

        override fun recentBooks(): List<String> = recentlyOpenedBooks(12)

        override fun recentlyOpenedBooks(count: Int): List<String> {
            return SerializerUtil.serializeBookList(myCollection.recentlyOpenedBooks(count))
        }

        override fun recentlyAddedBooks(count: Int): List<String> {
            return SerializerUtil.serializeBookList(myCollection.recentlyAddedBooks(count))
        }

        override fun getRecentBook(index: Int): String {
            return SerializerUtil.serialize(myCollection.getRecentBook(index)) ?: ""
        }

        override fun getBookByFile(path: String): String {
            return SerializerUtil.serialize(myCollection.getBookByFile(path)) ?: ""
        }

        override fun getBookById(id: Long): String {
            return SerializerUtil.serialize(myCollection.getBookById(id)) ?: ""
        }

        override fun getBookByUid(type: String, id: String): String {
            return SerializerUtil.serialize(myCollection.getBookByUid(UID(type, id))) ?: ""
        }

        override fun getBookByHash(hash: String): String {
            return SerializerUtil.serialize(myCollection.getBookByHash(hash)) ?: ""
        }

        override fun authors(): List<String> {
            val authors = myCollection.authors()
            val strings = ArrayList<String>(authors.size)
            for (a in authors) {
                strings.add(Util.authorToString(a))
            }
            return strings
        }

        override fun hasSeries(): Boolean = myCollection.hasSeries()

        override fun series(): List<String> = myCollection.series()

        override fun tags(): List<String> {
            val tags = myCollection.tags()
            val strings = ArrayList<String>(tags.size)
            for (t in tags) {
                strings.add(Util.tagToString(t))
            }
            return strings
        }

        override fun titles(query: String): List<String> {
            val bookQuery = SerializerUtil.deserializeBookQuery(query) ?: return emptyList()
            return myCollection.titles(bookQuery)
        }

        override fun firstTitleLetters(): List<String> = myCollection.firstTitleLetters()

        override fun saveBook(book: String): Boolean {
            return myCollection.saveBook(SerializerUtil.deserializeBook(book, myCollection) ?: return false)
        }

        override fun canRemoveBook(book: String, deleteFromDisk: Boolean): Boolean {
            return myCollection.canRemoveBook(SerializerUtil.deserializeBook(book, myCollection) ?: return false, deleteFromDisk)
        }

        override fun removeBook(book: String, deleteFromDisk: Boolean) {
            myCollection.removeBook(SerializerUtil.deserializeBook(book, myCollection) ?: return, deleteFromDisk)
        }

        override fun addToRecentlyOpened(book: String) {
            myCollection.addToRecentlyOpened(SerializerUtil.deserializeBook(book, myCollection) ?: return)
        }

        override fun removeFromRecentlyOpened(book: String) {
            myCollection.removeFromRecentlyOpened(SerializerUtil.deserializeBook(book, myCollection) ?: return)
        }

        override fun labels(): List<String> = myCollection.labels()

        override fun getStoredPosition(bookId: Long): PositionWithTimestamp? {
            val position = myCollection.getStoredPosition(bookId)
            return position?.let { PositionWithTimestamp(it) }
        }

        override fun storePosition(bookId: Long, pos: PositionWithTimestamp?) {
            if (pos == null) {
                return
            }
            myCollection.storePosition(bookId, ZLTextFixedPosition.WithTimestamp(
                pos.paragraphIndex, pos.elementIndex, pos.charIndex, pos.timestamp
            ))
        }

        override fun isHyperlinkVisited(book: String, linkId: String): Boolean {
            return myCollection.isHyperlinkVisited(SerializerUtil.deserializeBook(book, myCollection) ?: return false, linkId)
        }

        override fun markHyperlinkAsVisited(book: String, linkId: String) {
            myCollection.markHyperlinkAsVisited(SerializerUtil.deserializeBook(book, myCollection) ?: return, linkId)
        }

        override fun getCoverUrl(path: String): String {
            return DataUtil.buildUrl(dataConnection, "cover", path) ?: ""
        }

        override fun getDescription(book: String): String? {
            return BookUtil.getAnnotation(SerializerUtil.deserializeBook(book, myCollection) ?: return null, myCollection.pluginCollection)
        }

        override fun getCover(bookString: String?, maxWidth: Int, maxHeight: Int, delayed: BooleanArray?): Bitmap? {
            // this method kept for compatibility
            delayed?.set(0, false)
            return null
        }

        override fun bookmarks(query: String): List<String> {
            val bookmarkQuery = SerializerUtil.deserializeBookmarkQuery(query, myCollection) ?: return emptyList()
            return SerializerUtil.serializeBookmarkList(myCollection.bookmarks(bookmarkQuery))
        }

        override fun saveBookmark(serialized: String): String {
            val bookmark = SerializerUtil.deserializeBookmark(serialized) ?: return ""
            myCollection.saveBookmark(bookmark)
            return SerializerUtil.serialize(bookmark) ?: ""
        }

        override fun deleteBookmark(serialized: String) {
            myCollection.deleteBookmark(SerializerUtil.deserializeBookmark(serialized) ?: return)
        }

        override fun deletedBookmarkUids(): List<String> = myCollection.deletedBookmarkUids()

        override fun purgeBookmarks(uids: List<String>) {
            myCollection.purgeBookmarks(uids)
        }

        override fun getHighlightingStyle(styleId: Int): String {
            return SerializerUtil.serialize(myCollection.getHighlightingStyle(styleId)) ?: ""
        }

        override fun highlightingStyles(): List<String> {
            return SerializerUtil.serializeStyleList(myCollection.highlightingStyles())
        }

        override fun saveHighlightingStyle(style: String) {
            myCollection.saveHighlightingStyle(SerializerUtil.deserializeStyle(style) ?: return)
        }

        override fun getDefaultHighlightingStyleId(): Int = myCollection.getDefaultHighlightingStyleId()

        override fun setDefaultHighlightingStyleId(styleId: Int) {
            myCollection.setDefaultHighlightingStyleId(styleId)
        }

        override fun rescan(path: String) {
            myCollection.rescan(path)
        }

        override fun getHash(book: String, force: Boolean): String? {
            return myCollection.getHash(SerializerUtil.deserializeBook(book, myCollection) ?: return null, force)
        }

        override fun setHash(book: String, hash: String?) {
            myCollection.setHash(SerializerUtil.deserializeBook(book, myCollection) ?: return, hash ?: return)
        }

        override fun formats(): List<String> {
            val descriptors = myCollection.formats()
            val serialized = ArrayList<String>(descriptors.size)
            for (d in descriptors) {
                serialized.add(Util.formatDescriptorToString(d))
            }
            return serialized
        }

        override fun setActiveFormats(formatIds: List<String>): Boolean {
            return if (myCollection.setActiveFormats(formatIds)) {
                reset(true)
                true
            } else {
                false
            }
        }
    }

    companion object {
        private val ourDatabaseLock = Any()
        private var ourDatabase: SQLiteBooksDatabase? = null
    }
}
