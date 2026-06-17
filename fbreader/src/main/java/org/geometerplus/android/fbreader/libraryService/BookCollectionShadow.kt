package org.geometerplus.android.fbreader.libraryService

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.fbreader.book.AbstractBookCollection
import org.geometerplus.fbreader.book.Author
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookQuery
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.SerializerUtil
import org.geometerplus.fbreader.book.Tag
import org.geometerplus.fbreader.book.UID
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import java.util.Collections
import java.util.LinkedList

class BookCollectionShadow : AbstractBookCollection<Book>(), ServiceConnection {
    private val myOnBindActions = LinkedList<Runnable>()
    private val myReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!hasListeners()) {
                return
            }

            try {
                val type = intent.getStringExtra("type")
                if (FBReaderIntents.Event.LIBRARY_BOOK == intent.action) {
                    val book = SerializerUtil.deserializeBook(intent.getStringExtra("book"), this@BookCollectionShadow)
                    fireBookEvent(BookEvent.valueOf(type!!), book)
                } else {
                    fireBuildEvent(IBookCollection.Status.valueOf(type!!))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    @Volatile private var myContext: Context? = null
    @Volatile private var myInterface: LibraryInterface? = null

    @Synchronized
    fun bindToService(context: Context, onBindAction: Runnable?): Boolean {
        if (myInterface != null && myContext == context) {
            onBindAction?.let { Config.Instance()?.runOnConnect(it) }
            return true
        } else {
            onBindAction?.let {
                synchronized(myOnBindActions) {
                    myOnBindActions.add(it)
                }
            }
            val result = context.bindService(
                FBReaderIntents.internalIntent(FBReaderIntents.Action.LIBRARY_SERVICE),
                this,
                Service.BIND_AUTO_CREATE
            )
            if (result) {
                myContext = context
            }
            return result
        }
    }

    @Synchronized
    fun unbind() {
        if (myContext != null && myInterface != null) {
            try {
                myContext!!.unregisterReceiver(myReceiver)
            } catch (e: IllegalArgumentException) {
                // called before registration, that's ok
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                myContext!!.unbindService(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            myInterface = null
            myContext = null
        }
    }

    @Synchronized
    fun reset(force: Boolean) {
        myInterface?.let {
            try {
                it.reset(force)
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun size(): Int {
        myInterface?.let {
            try {
                return it.size()
            } catch (e: RemoteException) {
            }
        }
        return 0
    }

    @Synchronized
    override fun status(): IBookCollection.Status {
        myInterface?.let {
            try {
                return IBookCollection.Status.valueOf(it.status())
            } catch (t: Throwable) {
            }
        }
        return IBookCollection.Status.NotStarted
    }

    override fun books(query: BookQuery): List<Book> {
        return listCall(object : ListCallable<Book> {
            override fun call(): List<Book> {
                return SerializerUtil.deserializeBookList(
                    myInterface!!.books(SerializerUtil.serialize(query)), this@BookCollectionShadow
                )
            }
        })
    }

    @Synchronized
    override fun hasBooks(filter: Filter): Boolean {
        myInterface?.let {
            try {
                return it.hasBooks(SerializerUtil.serialize(BookQuery(filter, 1)))
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    override fun recentlyAddedBooks(count: Int): List<Book> {
        return listCall(object : ListCallable<Book> {
            override fun call(): List<Book> {
                return SerializerUtil.deserializeBookList(
                    myInterface!!.recentlyAddedBooks(count), this@BookCollectionShadow
                )
            }
        })
    }

    override fun recentlyOpenedBooks(count: Int): List<Book> {
        return listCall(object : ListCallable<Book> {
            override fun call(): List<Book> {
                return SerializerUtil.deserializeBookList(
                    myInterface!!.recentlyOpenedBooks(count), this@BookCollectionShadow
                )
            }
        })
    }

    @Synchronized
    override fun getRecentBook(index: Int): Book? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeBook(it.getRecentBook(index), this)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        return null
    }

    @Synchronized
    override fun getBookByFile(path: String): Book? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeBook(it.getBookByFile(path), this)
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    @Synchronized
    override fun getBookById(id: Long): Book? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeBook(it.getBookById(id), this)
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    @Synchronized
    override fun getBookByUid(uid: UID): Book? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeBook(it.getBookByUid(uid.type, uid.id), this)
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    @Synchronized
    override fun getBookByHash(hash: String): Book? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeBook(it.getBookByHash(hash), this)
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    override fun authors(): List<Author> {
        return listCall(object : ListCallable<Author> {
            override fun call(): List<Author> {
                val strings = myInterface!!.authors()
                val authors = ArrayList<Author>(strings.size)
                for (s in strings) {
                    authors.add(Util.stringToAuthor(s))
                }
                return authors
            }
        })
    }

    override fun tags(): List<Tag> {
        return listCall(object : ListCallable<Tag> {
            override fun call(): List<Tag> {
                val strings = myInterface!!.tags()
                val tags = ArrayList<Tag>(strings.size)
                for (s in strings) {
                    tags.add(Util.stringToTag(s))
                }
                return tags
            }
        })
    }

    @Synchronized
    override fun hasSeries(): Boolean {
        myInterface?.let {
            try {
                return it.hasSeries()
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    override fun series(): List<String> {
        return listCall(object : ListCallable<String> {
            override fun call(): List<String> {
                return myInterface!!.series()
            }
        })
    }

    override fun titles(query: BookQuery): List<String> {
        return listCall(object : ListCallable<String> {
            override fun call(): List<String> {
                return myInterface!!.titles(SerializerUtil.serialize(query))
            }
        })
    }

    override fun firstTitleLetters(): List<String> {
        return listCall(object : ListCallable<String> {
            override fun call(): List<String> {
                return myInterface!!.firstTitleLetters()
            }
        })
    }

    @Synchronized
    override fun saveBook(book: Book): Boolean {
        myInterface?.let {
            try {
                return it.saveBook(SerializerUtil.serialize(book))
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    @Synchronized
    override fun canRemoveBook(book: Book, deleteFromDisk: Boolean): Boolean {
        myInterface?.let {
            try {
                return it.canRemoveBook(SerializerUtil.serialize(book), deleteFromDisk)
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    @Synchronized
    override fun removeBook(book: Book, deleteFromDisk: Boolean) {
        myInterface?.let {
            try {
                it.removeBook(SerializerUtil.serialize(book), deleteFromDisk)
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun addToRecentlyOpened(book: Book) {
        myInterface?.let {
            try {
                it.addToRecentlyOpened(SerializerUtil.serialize(book))
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun removeFromRecentlyOpened(book: Book) {
        myInterface?.let {
            try {
                it.removeFromRecentlyOpened(SerializerUtil.serialize(book))
            } catch (e: RemoteException) {
            }
        }
    }

    override fun labels(): List<String> {
        return listCall(object : ListCallable<String> {
            override fun call(): List<String> {
                return myInterface!!.labels()
            }
        })
    }

    override fun getHash(book: Book, force: Boolean): String? {
        myInterface?.let {
            try {
                return it.getHash(SerializerUtil.serialize(book), force)
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    override fun setHash(book: Book, hash: String) {
        myInterface?.let {
            try {
                it.setHash(SerializerUtil.serialize(book), hash)
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun getStoredPosition(bookId: Long): ZLTextFixedPosition.WithTimestamp? {
        myInterface?.let {
            try {
                val pos = it.getStoredPosition(bookId) ?: return null
                return ZLTextFixedPosition.WithTimestamp(
                    pos.paragraphIndex, pos.elementIndex, pos.charIndex, pos.timestamp
                )
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    @Synchronized
    override fun storePosition(bookId: Long, position: ZLTextPosition) {
        if (myInterface != null) {
            try {
                myInterface!!.storePosition(bookId, PositionWithTimestamp(position))
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun isHyperlinkVisited(book: Book, linkId: String): Boolean {
        myInterface?.let {
            try {
                return it.isHyperlinkVisited(SerializerUtil.serialize(book), linkId)
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    @Synchronized
    override fun markHyperlinkAsVisited(book: Book, linkId: String) {
        myInterface?.let {
            try {
                it.markHyperlinkAsVisited(SerializerUtil.serialize(book), linkId)
            } catch (e: RemoteException) {
            }
        }
    }

    override fun getCoverUrl(book: Book): String? {
        myInterface?.let {
            try {
                return it.getCoverUrl(book.getPath())
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    override fun getDescription(book: Book): String? {
        myInterface?.let {
            try {
                return it.getDescription(SerializerUtil.serialize(book))
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    override fun bookmarks(query: BookmarkQuery): List<Bookmark> {
        return listCall(object : ListCallable<Bookmark> {
            override fun call(): List<Bookmark> {
                return SerializerUtil.deserializeBookmarkList(
                    myInterface!!.bookmarks(SerializerUtil.serialize(query))
                )
            }
        })
    }

    @Synchronized
    override fun saveBookmark(bookmark: Bookmark) {
        myInterface?.let {
            try {
                bookmark.update(SerializerUtil.deserializeBookmark(
                    it.saveBookmark(SerializerUtil.serialize(bookmark))
                ))
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun deleteBookmark(bookmark: Bookmark) {
        myInterface?.let {
            try {
                it.deleteBookmark(SerializerUtil.serialize(bookmark))
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun deletedBookmarkUids(): List<String> {
        return listCall(object : ListCallable<String> {
            override fun call(): List<String> {
                return myInterface!!.deletedBookmarkUids()
            }
        })
    }

    override fun purgeBookmarks(uids: List<String>) {
        myInterface?.let {
            try {
                it.purgeBookmarks(uids)
            } catch (e: RemoteException) {
            }
        }
    }

    @Synchronized
    override fun getHighlightingStyle(styleId: Int): HighlightingStyle? {
        myInterface?.let {
            try {
                return SerializerUtil.deserializeStyle(it.getHighlightingStyle(styleId))
            } catch (e: RemoteException) {
            }
        }
        return null
    }

    override fun highlightingStyles(): List<HighlightingStyle> {
        return listCall(object : ListCallable<HighlightingStyle> {
            override fun call(): List<HighlightingStyle> {
                return SerializerUtil.deserializeStyleList(myInterface!!.highlightingStyles())
            }
        })
    }

    @Synchronized
    override fun saveHighlightingStyle(style: HighlightingStyle) {
        myInterface?.let {
            try {
                it.saveHighlightingStyle(SerializerUtil.serialize(style))
            } catch (e: RemoteException) {
                // ignore
            }
        }
    }

    override fun getDefaultHighlightingStyleId(): Int {
        myInterface?.let {
            try {
                return it.defaultHighlightingStyleId
            } catch (e: RemoteException) {
            }
        }
        return 1
    }

    override fun setDefaultHighlightingStyleId(styleId: Int) {
        myInterface?.let {
            try {
                it.setDefaultHighlightingStyleId(styleId)
            } catch (e: RemoteException) {
                // ignore
            }
        }
    }

    @Synchronized
    override fun rescan(path: String) {
        myInterface?.let {
            try {
                it.rescan(path)
            } catch (e: RemoteException) {
                // ignore
            }
        }
    }

    override fun formats(): List<IBookCollection.FormatDescriptor> {
        return listCall(object : ListCallable<IBookCollection.FormatDescriptor> {
            override fun call(): List<IBookCollection.FormatDescriptor> {
                val serialized = myInterface!!.formats()
                val formats = ArrayList<IBookCollection.FormatDescriptor>(serialized.size)
                for (s in serialized) {
                    formats.add(Util.stringToFormatDescriptor(s))
                }
                return formats
            }
        })
    }

    @Synchronized
    override fun setActiveFormats(formats: List<String>): Boolean {
        myInterface?.let {
            try {
                return it.setActiveFormats(formats)
            } catch (e: RemoteException) {
            }
        }
        return false
    }

    @Synchronized
    private fun <T> listCall(callable: ListCallable<T>): List<T> {
        myInterface?.let {
            try {
                return callable.call()
            } catch (e: Exception) {
                return Collections.emptyList()
            } catch (e: Throwable) {
                // TODO: report problem
                return Collections.emptyList()
            }
        }
        return Collections.emptyList()
    }

    // method from ServiceConnection interface
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        synchronized(this) {
            myInterface = LibraryInterface.Stub.asInterface(service)
        }

        val actions: List<Runnable>
        synchronized(myOnBindActions) {
            actions = ArrayList(myOnBindActions)
            myOnBindActions.clear()
        }
        for (a in actions) {
            Config.Instance()?.runOnConnect(a)
        }

        myContext?.let {
            it.registerReceiver(myReceiver, IntentFilter(FBReaderIntents.Event.LIBRARY_BOOK))
            it.registerReceiver(myReceiver, IntentFilter(FBReaderIntents.Event.LIBRARY_BUILD))
        }
    }

    // method from ServiceConnection interface
    @Synchronized
    override fun onServiceDisconnected(name: ComponentName) {
    }

    override fun createBook(id: Long, url: String?, title: String?, encoding: String?, language: String?): Book? {
        return Book(id, url?.substring("file://".length) ?: "", title, encoding, language)
    }

    private interface ListCallable<T> {
        @Throws(RemoteException::class)
        fun call(): List<T>
    }
}
