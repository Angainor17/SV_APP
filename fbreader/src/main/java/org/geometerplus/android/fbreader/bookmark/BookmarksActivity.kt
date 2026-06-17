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

package org.geometerplus.android.fbreader.bookmark

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.android.fbreader.FBReader
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.util.DeviceType
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.SearchDialogUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.MiscUtil
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler
import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView
import java.util.Collections

class BookmarksActivity : Activity(), IBookCollection.Listener<Book> {

    private val myStyles = Collections.synchronizedMap(mutableMapOf<Int, HighlightingStyle>())
    private val myCollection = BookCollectionShadow()
    private val myComparator = Bookmark.ByTimeComparator()
    private val myResource = ZLResource.resource("bookmarksView")
    private val myBookmarkSearchPatternOption = ZLStringOption("BookmarkSearch", "Pattern", "")
    private val myBookmarksLock = Any()

    private lateinit var myTabHost: TabHost
    @Volatile
    private var myBook: Book? = null
    @Volatile
    private var myBookmark: Bookmark? = null
    @Volatile
    private var myThisBookAdapter: BookmarksAdapter? = null
    @Volatile
    private var myAllBooksAdapter: BookmarksAdapter? = null
    @Volatile
    private var mySearchResultsAdapter: BookmarksAdapter? = null

    private fun createTab(tag: String, id: Int) {
        val label = myResource.getResource(tag).value
        myTabHost.addTab(myTabHost.newTabSpec(tag).setIndicator(label).setContent(id))
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))
        setContentView(R.layout.bookmarks)

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL)

        val manager = getSystemService(SEARCH_SERVICE) as SearchManager
        manager.setOnCancelListener(null)

        myTabHost = findViewById(R.id.bookmarks_tabhost)
        myTabHost.setup()

        createTab("thisBook", R.id.bookmarks_this_book)
        createTab("allBooks", R.id.bookmarks_all_books)
        createTab("search", R.id.bookmarks_search)

        myTabHost.setOnTabChangedListener { tabId ->
            if (tabId == "search") {
                findViewById<View>(R.id.bookmarks_search_results).visibility = View.GONE
                onSearchRequested()
            }
        }

        myBook = FBReaderIntents.getBookExtra(intent, myCollection)
        if (myBook == null) {
            finish()
        }
        myBookmark = FBReaderIntents.getBookmarkExtra(intent)
    }

    override fun onStart() {
        super.onStart()

        myCollection.bindToService(this) {
            if (myAllBooksAdapter != null) {
                return@bindToService
            }

            myThisBookAdapter = BookmarksAdapter(findViewById(R.id.bookmarks_this_book), myBookmark != null)
            myAllBooksAdapter = BookmarksAdapter(findViewById(R.id.bookmarks_all_books), false)
            myCollection.addListener(this@BookmarksActivity)

            updateStyles()
            loadBookmarks()
        }

        OrientationUtil.setOrientation(this, intent)
    }

    private fun updateStyles() {
        synchronized(myStyles) {
            myStyles.clear()
            for (style in myCollection.highlightingStyles()) {
                myStyles[style.id] = style
            }
        }
    }

    private fun loadBookmarks() {
        Thread {
            synchronized(myBookmarksLock) {
                var query = BookmarkQuery(myBook!!, 50)
                while (true) {
                    val thisBookBookmarks = myCollection.bookmarks(query)
                    if (thisBookBookmarks.isEmpty()) break
                    myThisBookAdapter?.addAll(thisBookBookmarks)
                    myAllBooksAdapter?.addAll(thisBookBookmarks)
                    query = query.next()
                }
                var allQuery = BookmarkQuery(50)
                while (true) {
                    val allBookmarks = myCollection.bookmarks(allQuery)
                    if (allBookmarks.isEmpty()) break
                    myAllBooksAdapter?.addAll(allBookmarks)
                    allQuery = allQuery.next()
                }
            }
        }.start()
    }

    private fun updateBookmarks(book: Book) {
        Thread {
            synchronized(myBookmarksLock) {
                val flagThisBookTab = book.id == myBook!!.id
                val flagSearchTab = mySearchResultsAdapter != null

                val oldBookmarks = mutableMapOf<String, Bookmark>()
                if (flagThisBookTab) {
                    for (b in myThisBookAdapter!!.bookmarks()) {
                        oldBookmarks[b.uid] = b
                    }
                } else {
                    for (b in myAllBooksAdapter!!.bookmarks()) {
                        if (b.bookId == book.id) {
                            oldBookmarks[b.uid] = b
                        }
                    }
                }
                val pattern = myBookmarkSearchPatternOption.value.lowercase()

                var query = BookmarkQuery(book, 50)
                while (true) {
                    val loaded = myCollection.bookmarks(query)
                    if (loaded.isEmpty()) break
                    for (b in loaded) {
                        val old = oldBookmarks.remove(b.uid)
                        myAllBooksAdapter?.replace(old, b)
                        if (flagThisBookTab) {
                            myThisBookAdapter?.replace(old, b)
                        }
                        if (flagSearchTab && MiscUtil.matchesIgnoreCase(b.text, pattern)) {
                            mySearchResultsAdapter?.replace(old, b)
                        }
                    }
                    query = query.next()
                }
                myAllBooksAdapter?.removeAll(oldBookmarks.values)
                if (flagThisBookTab) {
                    myThisBookAdapter?.removeAll(oldBookmarks.values)
                }
                if (flagSearchTab) {
                    mySearchResultsAdapter?.removeAll(oldBookmarks.values)
                }
            }
        }.start()
    }

    override fun onNewIntent(intent: Intent) {
        OrientationUtil.setOrientation(this, intent)

        if (Intent.ACTION_SEARCH != intent.action) {
            return
        }
        var pattern = intent.getStringExtra(SearchManager.QUERY)
        myBookmarkSearchPatternOption.value = pattern ?: ""

        val bookmarks = mutableListOf<Bookmark>()
        pattern = pattern!!.lowercase()
        for (b in myAllBooksAdapter!!.bookmarks()) {
            if (MiscUtil.matchesIgnoreCase(b.text, pattern)) {
                bookmarks.add(b)
            }
        }
        if (bookmarks.isNotEmpty()) {
            val resultsView: ListView = findViewById(R.id.bookmarks_search_results)
            resultsView.visibility = View.VISIBLE
            if (mySearchResultsAdapter == null) {
                mySearchResultsAdapter = BookmarksAdapter(resultsView, false)
            } else {
                mySearchResultsAdapter!!.clear()
            }
            mySearchResultsAdapter!!.addAll(bookmarks)
        } else {
            UIMessageUtil.showErrorMessage(this, "bookmarkNotFound")
        }
    }

    override fun onDestroy() {
        myCollection.unbind()
        super.onDestroy()
    }

    override fun onSearchRequested(): Boolean {
        if (DeviceType.Instance().hasStandardSearchDialog()) {
            startSearch(myBookmarkSearchPatternOption.value, true, null, false)
        } else {
            SearchDialogUtil.showDialog(this, BookmarksActivity::class.java, myBookmarkSearchPatternOption.value, null)
        }
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = (item.menuInfo as AdapterView.AdapterContextMenuInfo).position
        val tag = myTabHost.currentTabTag
        val adapter: BookmarksAdapter? = when (tag) {
            "thisBook" -> myThisBookAdapter
            "allBooks" -> myAllBooksAdapter
            "search" -> mySearchResultsAdapter
            else -> throw RuntimeException("Unknown tab tag: $tag")
        }

        val bookmark = adapter!!.getItem(position)
        when (item.itemId) {
            OPEN_ITEM_ID -> {
                gotoBookmark(bookmark!!)
                return true
            }
            EDIT_ITEM_ID -> {
                val editIntent = Intent(this, EditBookmarkActivity::class.java)
                FBReaderIntents.putBookmarkExtra(editIntent, bookmark!!)
                OrientationUtil.startActivity(this, editIntent)
                return true
            }
            DELETE_ITEM_ID -> {
                myCollection.deleteBookmark(bookmark!!)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun gotoBookmark(bookmark: Bookmark) {
        bookmark.markAsAccessed()
        myCollection.saveBookmark(bookmark)
        val book = myCollection.getBookById(bookmark.bookId)
        if (book != null) {
            FBReader.openBookActivity(this, book, bookmark)
        } else {
            UIMessageUtil.showErrorMessage(this, "cannotOpenBook")
        }
    }

    override fun onBookEvent(event: BookEvent, book: Book?) {
        when (event) {
            BookEvent.BookmarkStyleChanged -> runOnUiThread {
                updateStyles()
                myAllBooksAdapter?.notifyDataSetChanged()
                myThisBookAdapter?.notifyDataSetChanged()
                mySearchResultsAdapter?.notifyDataSetChanged()
            }
            BookEvent.BookmarksUpdated -> updateBookmarks(book!!)
            else -> {}
        }
    }

    override fun onBuildEvent(status: IBookCollection.Status) {}

    private inner class BookmarksAdapter(
        listView: ListView,
        private var myShowAddBookmarkItem: Boolean
    ) : BaseAdapter(), AdapterView.OnItemClickListener, View.OnCreateContextMenuListener {

        private val myBookmarksList = Collections.synchronizedList(mutableListOf<Bookmark>())

        init {
            listView.adapter = this
            listView.onItemClickListener = this
            listView.setOnCreateContextMenuListener(this)
        }

        fun bookmarks(): List<Bookmark> = myBookmarksList.toList()

        fun addAll(bookmarks: List<Bookmark>) {
            runOnUiThread {
                synchronized(myBookmarksList) {
                    for (b in bookmarks) {
                        val position = Collections.binarySearch(myBookmarksList, b, myComparator)
                        if (position < 0) {
                            myBookmarksList.add(-position - 1, b)
                        }
                    }
                }
                notifyDataSetChanged()
            }
        }

        private fun areEqualsForView(b0: Bookmark, b1: Bookmark): Boolean =
            b0.styleId == b1.styleId &&
                b0.text == b1.text &&
                b0.getTimestamp(Bookmark.DateType.Latest) == b1.getTimestamp(Bookmark.DateType.Latest)

        fun replace(old: Bookmark?, b: Bookmark) {
            if (old != null && areEqualsForView(old, b)) {
                return
            }
            runOnUiThread {
                synchronized(myBookmarksList) {
                    if (old != null) {
                        myBookmarksList.remove(old)
                    }
                    val position = Collections.binarySearch(myBookmarksList, b, myComparator)
                    if (position < 0) {
                        myBookmarksList.add(-position - 1, b)
                    }
                }
                notifyDataSetChanged()
            }
        }

        fun removeAll(bookmarks: Collection<Bookmark>) {
            if (bookmarks.isEmpty()) {
                return
            }
            runOnUiThread {
                myBookmarksList.removeAll(bookmarks)
                notifyDataSetChanged()
            }
        }

        fun clear() {
            runOnUiThread {
                myBookmarksList.clear()
                notifyDataSetChanged()
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
            val position = (menuInfo as AdapterView.AdapterContextMenuInfo).position
            if (getItem(position) != null) {
                menu.add(0, OPEN_ITEM_ID, 0, myResource.getResource("openBook").value)
                menu.add(0, EDIT_ITEM_ID, 0, myResource.getResource("editBookmark").value)
                menu.add(0, DELETE_ITEM_ID, 0, myResource.getResource("deleteBookmark").value)
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.bookmark_item, parent, false)
            val imageView: ImageView = ViewUtil.findImageView(view, R.id.bookmark_item_icon)
            val colorContainer: View = ViewUtil.findView(view, R.id.bookmark_item_color_container)
            val colorView: AmbilWarnaPrefWidgetView = ViewUtil.findView(view, R.id.bookmark_item_color) as AmbilWarnaPrefWidgetView
            val textView: TextView = ViewUtil.findTextView(view, R.id.bookmark_item_text)
            val bookTitleView: TextView = ViewUtil.findTextView(view, R.id.bookmark_item_booktitle)

            val bookmark = getItem(position)
            if (bookmark == null) {
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.ic_list_plus)
                colorContainer.visibility = View.GONE
                textView.text = myResource.getResource("new").value
                bookTitleView.visibility = View.GONE
            } else {
                imageView.visibility = View.GONE
                colorContainer.visibility = View.VISIBLE
                BookmarksUtil.setupColorView(colorView, myStyles[bookmark.styleId])
                textView.text = bookmark.text
                if (myShowAddBookmarkItem) {
                    bookTitleView.visibility = View.GONE
                } else {
                    bookTitleView.visibility = View.VISIBLE
                    bookTitleView.text = bookmark.bookTitle
                }
            }
            return view
        }

        override fun areAllItemsEnabled(): Boolean = true

        override fun isEnabled(position: Int): Boolean = true

        override fun getItemId(position: Int): Long {
            val item = getItem(position)
            return item?.id ?: -1
        }

        override fun getItem(position: Int): Bookmark? {
            var pos = position
            if (myShowAddBookmarkItem) {
                pos--
            }
            return if (pos >= 0) myBookmarksList[pos] else null
        }

        override fun getCount(): Int =
            if (myShowAddBookmarkItem) myBookmarksList.size + 1 else myBookmarksList.size

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val bookmark = getItem(position)
            if (bookmark != null) {
                gotoBookmark(bookmark)
            } else if (myShowAddBookmarkItem) {
                myShowAddBookmarkItem = false
                myCollection.saveBookmark(myBookmark!!)
            }
        }
    }

    companion object {
        private const val OPEN_ITEM_ID = 0
        private const val EDIT_ITEM_ID = 1
        private const val DELETE_ITEM_ID = 2
    }
}
